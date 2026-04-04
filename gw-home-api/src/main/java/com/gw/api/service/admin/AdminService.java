package com.gw.api.service.admin;

import com.gw.api.convert.admin.AdminConvert;
import com.gw.api.dto.admin.AdminBoardPostListRequest;
import com.gw.api.dto.admin.AdminBoardPostResponse;
import com.gw.api.dto.admin.AdminMemberListRequest;
import com.gw.api.dto.admin.AdminMemberResponse;
import com.gw.api.dto.admin.AdminSummaryResponse;
import com.gw.infra.db.mapper.admin.AdminMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.query.SortDirection;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.admin.AdminMbrJvo;
import com.gw.share.jvo.admin.AdminSmryJvo;
import com.gw.share.vo.admin.AdminBrdPstListSrchVo;
import com.gw.share.vo.admin.AdminMbrListSrchVo;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class AdminService {

    private static final Map<String, String> MBR_SORT_FIELD_MAP = Map.of(
            "createdAt", "ma.created_at",
            "loginId", "ma.lgn_id",
            "email", "ma.email",
            "role", "ma.role"
    );

    private static final Map<String, String> BRD_PST_SORT_FIELD_MAP = Map.of(
            "createdAt", "bp.created_at",
            "title", "bp.ttl",
            "viewCnt", "bp.view_cnt"
    );

    private final AdminMapper adminMapper;

    public AdminService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Transactional(readOnly = true)
    // 관리자 회원 목록을 조회한다.
    public PageResponse<AdminMemberResponse> getMemberList(AdminMemberListRequest request) {
        log.info("getMemberList 시작 - request: {}", request);
        try {
            AdminMbrListSrchVo query = AdminMbrListSrchVo.builder()
                    .kwd(request.keyword())
                    .role(request.role())
                    .deleted(Boolean.TRUE.equals(request.deleted()))
                    .page(PageSortSupport.normalizePage(request.page()))
                    .size(PageSortSupport.normalizeSize(request.size()))
                    .sortBy(request.sortBy())
                    .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                    .orderByClause(PageSortSupport.resolveOrderByClause(
                            request.sortBy(),
                            request.sortDirection(),
                            MBR_SORT_FIELD_MAP,
                            "createdAt",
                            SortDirection.DESC
                    ))
                    .build();

            List<AdminMemberResponse> content = adminMapper.selectMemberList(query).stream()
                    .map(AdminConvert::toMemberResponse)
                    .toList();
            long totalCount = adminMapper.countMemberList(query);
            int totalPages = (int) Math.ceil((double) totalCount / query.getSize());
            PageResponse<AdminMemberResponse> response =
                    new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
            log.info("getMemberList 완료 - totalCount: {}", totalCount);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getMemberList 실패 - request: {}, 원인: {}, detailMessage: {}",
                    request,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 관리자 회원 상세를 조회한다.
    public AdminMemberResponse getMember(String memberAccountUuid) {
        log.info("getMember 시작 - memberAccountUuid: {}", memberAccountUuid);
        try {
            AdminMbrJvo member = adminMapper.selectMemberByUuid(memberAccountUuid);

            if (member == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다.");
            }

            AdminMemberResponse response = AdminConvert.toMemberResponse(member);
            log.info("getMember 완료 - memberAccountUuid: {}", memberAccountUuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getMember 실패 - memberAccountUuid: {}, 원인: {}, detailMessage: {}",
                    memberAccountUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 관리자 권한으로 회원을 강제 삭제한다.
    public void forceDeleteMember(String loginId, String memberAccountUuid) {
        log.info("forceDeleteMember 시작 - loginId: {}, memberAccountUuid: {}", loginId, memberAccountUuid);
        try {
            int updatedCount = adminMapper.forceDeleteMember(memberAccountUuid, loginId);

            if (updatedCount == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "회원이 없거나 이미 삭제되었습니다.");
            }

            log.info("forceDeleteMember 완료 - loginId: {}, memberAccountUuid: {}", loginId, memberAccountUuid);
        } catch (BusinessException exception) {
            log.error(
                    "forceDeleteMember 실패 - loginId: {}, memberAccountUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    memberAccountUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 관리자 게시글 목록을 조회한다.
    public PageResponse<AdminBoardPostResponse> getBoardPostList(AdminBoardPostListRequest request) {
        log.info("getBoardPostList 시작 - request: {}", request);
        try {
            AdminBrdPstListSrchVo query = AdminBrdPstListSrchVo.builder()
                    .kwd(request.keyword())
                    .deleted(Boolean.TRUE.equals(request.deleted()))
                    .page(PageSortSupport.normalizePage(request.page()))
                    .size(PageSortSupport.normalizeSize(request.size()))
                    .sortBy(request.sortBy())
                    .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                    .orderByClause(PageSortSupport.resolveOrderByClause(
                            request.sortBy(),
                            request.sortDirection(),
                            BRD_PST_SORT_FIELD_MAP,
                            "createdAt",
                            SortDirection.DESC
                    ))
                    .build();

            List<AdminBoardPostResponse> content = adminMapper.selectBoardPostList(query).stream()
                    .map(AdminConvert::toBoardPostResponse)
                    .toList();
            long totalCount = adminMapper.countBoardPostList(query);
            int totalPages = (int) Math.ceil((double) totalCount / query.getSize());
            PageResponse<AdminBoardPostResponse> response =
                    new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
            log.info("getBoardPostList 완료 - totalCount: {}", totalCount);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getBoardPostList 실패 - request: {}, 원인: {}, detailMessage: {}",
                    request,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 관리자 권한으로 게시글을 강제 삭제한다.
    public void forceDeleteBoardPost(String loginId, String boardPostUuid) {
        log.info("forceDeleteBoardPost 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        try {
            int updatedCount = adminMapper.forceDeleteBoardPost(boardPostUuid, loginId);

            if (updatedCount == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "게시글이 없거나 이미 삭제되었습니다.");
            }

            log.info("forceDeleteBoardPost 완료 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        } catch (BusinessException exception) {
            log.error(
                    "forceDeleteBoardPost 실패 - loginId: {}, boardPostUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    boardPostUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 관리자 대시보드 요약 정보를 조회한다.
    public AdminSummaryResponse getSummary() {
        log.info("getSummary 시작");
        AdminSmryJvo summary = adminMapper.selectSummary();
        AdminSummaryResponse response = AdminConvert.toSummaryResponse(summary);
        log.info("getSummary 완료 - totalMemberCount: {}", response.totalMemberCount());
        return response;
    }
}
