package com.gw.api.service.life;

import com.gw.api.convert.life.LifeTodoConvert;
import com.gw.api.dto.life.LifeTodoDtos.*;
import com.gw.api.dto.life.LifeTodoListRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.life.LifeTodoMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.LifeTodoPolicy;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.life.LifeTodoJvo;
import com.gw.share.util.StringUtil;
import com.gw.share.vo.life.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class LifeTodoService {
    private final LifeTodoMapper mapper;
    private final AccountLookupService accounts;

    // 인증 사용자의 일상 할 일을 같은 스냅샷의 건수와 함께 조회한다.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<Todo> getTodos(String loginId, LifeTodoListRequest request) {
        return logged("getLifeTodos", loginId, () -> {
            var search = prepareSearch(loginId, request);
            long total = mapper.selectCount(search);
            var content = mapper.selectTodos(search).stream().map(LifeTodoConvert::toResponse).toList();
            return PageResponse.of(content, request.getPage(), request.getSize(), total);
        });
    }

    // 업무 집계와 독립된 일상 할 일 요약을 반환한다.
    public Summary getSummary(String loginId, LifeTodoListRequest request) {
        return logged("getLifeTodoSummary", loginId, () -> {
            var search = prepareSearch(loginId, request);
            return LifeTodoConvert.toSummary(mapper.selectSummary(search), search.getToday());
        });
    }

    // 본인의 일상 할 일 상세를 조회한다.
    public Todo getTodo(String loginId, String uuid, String timeZone) {
        return logged("getLifeTodo", loginId, () -> LifeTodoConvert.toResponse(
                ownedTodo(owner(loginId), requiredUuid(uuid), LifeTodoPolicy.today(timeZone))));
    }

    // 업무 소속 없이 선택적인 생활 카테고리와 일정으로 일상 할 일을 등록한다.
    @Transactional
    public Todo createTodo(String loginId, SaveTodo request, String timeZone) {
        return logged("createLifeTodo", loginId, () -> {
            validateTodo(request);
            LocalDate today = LifeTodoPolicy.today(timeZone);
            Long owner = lockOwner(loginId);
            Long category = categoryId(owner, request.categoryUuid());
            String uuid = UUID.randomUUID().toString();
            mapper.insertTodo(LifeTodoConvert.toVo(request, owner, category, uuid, loginId));
            return LifeTodoConvert.toResponse(ownedTodo(owner, uuid, today));
        });
    }

    // 일상 할 일의 제목·메모·일정·카테고리를 수정하고 완료 상태는 보존한다.
    @Transactional
    public Todo updateTodo(String loginId, String uuid, SaveTodo request, String timeZone) {
        return logged("updateLifeTodo", loginId, () -> {
            validateTodo(request);
            String normalizedUuid = requiredUuid(uuid);
            LocalDate today = LifeTodoPolicy.today(timeZone);
            Long owner = lockOwner(loginId);
            ownedTodo(owner, normalizedUuid, today);
            Long category = categoryId(owner, request.categoryUuid());
            requireChanged(mapper.updateTodo(LifeTodoConvert.toVo(request, owner, category, normalizedUuid, loginId)));
            return LifeTodoConvert.toResponse(ownedTodo(owner, normalizedUuid, today));
        });
    }

    // 일상 할 일을 완료하거나 취소하며 반복 완료 시 기존 완료 시각을 유지한다.
    @Transactional
    public Todo completeTodo(String loginId, String uuid, Completion request, String timeZone) {
        return logged("completeLifeTodo", loginId, () -> {
            if (request == null || request.completed() == null) throw new BusinessException(ErrorCode.BAD_REQUEST);
            String normalizedUuid = requiredUuid(uuid);
            LocalDate today = LifeTodoPolicy.today(timeZone);
            Long owner = lockOwner(loginId);
            requireChanged(mapper.updateCompletion(owner, normalizedUuid, request.completed(), loginId));
            return LifeTodoConvert.toResponse(ownedTodo(owner, normalizedUuid, today));
        });
    }

    // 인증 사용자의 일상 항목을 소프트 삭제한다.
    @Transactional
    public void deleteTodo(String loginId, String uuid) {
        logged("deleteLifeTodo", loginId, () -> {
            String normalizedUuid = requiredUuid(uuid);
            requireChanged(mapper.deleteTodo(lockOwner(loginId), normalizedUuid, loginId));
            return null;
        });
    }

    // 인증 사용자만 사용하는 생활 카테고리를 조회한다.
    public List<Category> getCategories(String loginId) {
        return logged("getLifeCategories", loginId, () -> mapper.selectCategories(owner(loginId)).stream().map(LifeTodoConvert::toCategory).toList());
    }

    // 중복되지 않는 생활 카테고리를 생성한다.
    @Transactional
    public Category createCategory(String loginId, SaveCategory request) {
        return logged("createLifeCategory", loginId, () -> saveCategory(loginId, null, request));
    }

    // 본인의 생활 카테고리 이름을 변경한다.
    @Transactional
    public Category updateCategory(String loginId, String uuid, SaveCategory request) {
        return logged("updateLifeCategory", loginId, () -> saveCategory(loginId, requiredUuid(uuid), request));
    }

    // 카테고리는 삭제하고 기존 일상 할 일은 미분류로 보존한다.
    @Transactional
    public void deleteCategory(String loginId, String uuid) {
        logged("deleteLifeCategory", loginId, () -> {
            String normalizedUuid = requiredUuid(uuid);
            Long owner = lockOwner(loginId);
            var category = ownedCategory(owner, normalizedUuid);
            mapper.clearCategory(owner, category.getIdx(), loginId);
            requireChanged(mapper.deleteCategory(owner, normalizedUuid, loginId));
            return null;
        });
    }

    // 일상 카테고리의 생성·수정에 동일한 소유권과 이름 중복 정책을 적용한다.
    private Category saveCategory(String loginId, String uuid, SaveCategory request) {
        if (request == null || StringUtil.normalizeBlank(request.name()) == null || request.name().length() > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "카테고리 이름을 1~50자로 입력해주세요.");
        }
        Long owner = lockOwner(loginId);
        if (uuid != null) ownedCategory(owner, uuid);
        if (mapper.categoryNameExists(owner, request.name().trim(), uuid)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "같은 이름의 카테고리가 있습니다.");
        }
        var category = LifeTodoConvert.toCategoryVo(request.name(), owner, uuid, loginId);
        if (uuid == null) mapper.insertCategory(category);
        else requireChanged(mapper.updateCategory(category));
        return LifeTodoConvert.toCategory(category);
    }

    // 조회 조건을 검증한 후 인증 계정으로 검색 소유자를 고정한다.
    private LifeTodoSearchVo prepareSearch(String loginId, LifeTodoListRequest request) {
        if (request.getGroup() == null || !LifeTodoPolicy.GROUPS.contains(request.getGroup())
                || request.getPage() < 1 || request.getPage() > 1000000 || request.getSize() < 1 || request.getSize() > 100
                || (request.getKeyword() != null && request.getKeyword().length() > 200)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "일상 할 일 조회 조건을 확인해주세요.");
        }
        String category = optionalUuid(request.getCategoryUuid());
        if (category != null && request.isUncategorized()) throw new BusinessException(ErrorCode.BAD_REQUEST);
        LocalDate today = LifeTodoPolicy.today(request.getTimeZone());
        return LifeTodoSearchVo.builder().mbrAcctIdx(owner(loginId)).categoryUuid(category).uncategorized(request.isUncategorized())
                .keyword(StringUtil.normalizeBlank(request.getKeyword())).group(request.getGroup()).today(today)
                .size(request.getSize()).offset((long) (request.getPage() - 1) * request.getSize()).build();
    }

    // 직접 서비스 호출에서도 공백 제목과 과도한 입력을 거부한다.
    private void validateTodo(SaveTodo request) {
        if (request == null || StringUtil.normalizeBlank(request.title()) == null || request.title().length() > 200
                || (request.memo() != null && request.memo().length() > 4000)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "제목은 1~200자, 메모는 4000자 이내로 입력해주세요.");
        }
        optionalUuid(request.categoryUuid());
    }

    // 소유자는 요청 본문이 아닌 인증 계정에서 읽는다.
    private Long owner(String loginId) { return accounts.getAccountByLoginId(loginId).getIdx(); }

    // 카테고리 연결·삭제와 변경 요청의 순서를 계정 행으로 보장한다.
    private Long lockOwner(String loginId) {
        Long owner = owner(loginId);
        if (mapper.lockOwner(owner) == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return owner;
    }

    // 존재하지 않거나 다른 사용자에게 속한 항목은 같은 404로 처리한다.
    private LifeTodoJvo ownedTodo(Long owner, String uuid, LocalDate today) {
        var todo = mapper.selectTodo(owner, uuid, today);
        if (todo == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return todo;
    }

    // 다른 사용자의 카테고리를 연결하거나 수정할 수 없도록 검사한다.
    private LifeTodoCategoryVo ownedCategory(Long owner, String uuid) {
        var category = mapper.selectCategory(owner, uuid);
        if (category == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return category;
    }

    // 카테고리를 선택하지 않은 항목은 미분류로 저장한다.
    private Long categoryId(Long owner, String uuid) {
        String normalized = optionalUuid(uuid);
        return normalized == null ? null : ownedCategory(owner, normalized).getIdx();
    }

    // 잘못된 UUID 문자열이 DB 형 변환 오류로 이어지지 않게 한다.
    private String optionalUuid(String value) {
        String normalized = StringUtil.normalizeBlank(value);
        if (normalized == null) return null;
        try {
            String canonical = UUID.fromString(normalized).toString();
            if (!canonical.equalsIgnoreCase(normalized)) throw new IllegalArgumentException();
            return canonical;
        } catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST); }
    }

    // 경로 식별자는 비어 있을 수 없다.
    private String requiredUuid(String value) {
        String uuid = optionalUuid(value);
        if (uuid == null) throw new BusinessException(ErrorCode.BAD_REQUEST);
        return uuid;
    }

    // 삭제·수정 대상이 없으면 다른 소유자의 리소스와 동일하게 처리한다.
    private void requireChanged(int count) { if (count != 1) throw new BusinessException(ErrorCode.NOT_FOUND); }

    // 모든 공개 업무 흐름에서 제목·메모 없이 시작·완료·실패를 기록한다.
    private <T> T logged(String action, String loginId, Supplier<T> operation) {
        log.info("{} 시작 - loginId: {}", action, loginId);
        try {
            T result = operation.get();
            log.info("{} 완료 - loginId: {}", action, loginId);
            return result;
        } catch (RuntimeException exception) {
            log.error("{} 실패 - loginId: {}", action, loginId, exception);
            throw exception;
        }
    }
}
