package com.gw.api.service.vault;

import com.gw.api.convert.vault.VaultConvert;
import com.gw.api.dto.vault.CredentialResponse;
import com.gw.api.dto.vault.SaveCredentialRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.vault.VaultCategoryMapper;
import com.gw.infra.db.mapper.vault.VaultCredentialCategoryMapper;
import com.gw.infra.db.mapper.vault.VaultMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.vault.CatVo;
import com.gw.share.vo.vault.CrdCatVo;
import com.gw.share.vo.vault.CrdVo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class VaultService {

    private static final Pattern MULTI_WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final VaultMapper vaultMapper;
    private final VaultCategoryMapper vaultCategoryMapper;
    private final VaultCredentialCategoryMapper vaultCredentialCategoryMapper;
    private final AccountLookupService accountLookupService;

    public VaultService(
            VaultMapper vaultMapper,
            VaultCategoryMapper vaultCategoryMapper,
            VaultCredentialCategoryMapper vaultCredentialCategoryMapper,
            AccountLookupService accountLookupService
    ) {
        this.vaultMapper = vaultMapper;
        this.vaultCategoryMapper = vaultCategoryMapper;
        this.vaultCredentialCategoryMapper = vaultCredentialCategoryMapper;
        this.accountLookupService = accountLookupService;
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 자격증명 목록을 조회한다.
    public List<CredentialResponse> getCredentialList(String keyword, List<String> categoryUuids, String loginId) {
        log.info("getCredentialList 시작 - loginId: {}, keyword: {}, categoryUuids: {}", loginId, keyword, categoryUuids);
        AcctVo account = getAccountByLoginId(loginId);
        Map<Long, CatVo> categoryByIdx = getCategoryByIdxMap();
        List<CrdVo> credentialList = vaultMapper.selectCredentialList(
                normalizeKeywordTokens(keyword),
                normalizeCategoryUuids(categoryUuids),
                account.getIdx()
        );
        Map<Long, List<CatVo>> categoriesByCredentialIdx = getCategoriesByCredentialIdx(credentialList, categoryByIdx);
        List<CredentialResponse> response = credentialList.stream()
                .map(credential -> VaultConvert.toResponse(credential, categoriesByCredentialIdx))
                .toList();
        log.info("getCredentialList 완료 - loginId: {}, count: {}", loginId, response.size());
        return response;
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 자격증명 상세를 조회한다.
    public CredentialResponse getCredential(String uuid, String loginId) {
        log.info("getCredential 시작 - loginId: {}, uuid: {}", loginId, uuid);
        AcctVo account = getAccountByLoginId(loginId);
        CrdVo credential = getCredentialVo(uuid, account.getIdx());
        Map<Long, CatVo> categoryByIdx = getCategoryByIdxMap();
        Map<Long, List<CatVo>> categoriesByCredentialIdx = getCategoriesByCredentialIdx(List.of(credential), categoryByIdx);
        CredentialResponse response = VaultConvert.toResponse(credential, categoriesByCredentialIdx);
        log.info("getCredential 완료 - loginId: {}, uuid: {}", loginId, uuid);
        return response;
    }

    // 로그인 사용자의 자격증명을 생성한다.
    public CredentialResponse saveCredential(SaveCredentialRequest request, String loginId) {
        log.info("saveCredential 시작 - loginId: {}, title: {}", loginId, request.title());
        AcctVo account = getAccountByLoginId(loginId);
        List<Long> categoryIdxList = getCategoryIndexes(request.categoryUuids());

        CrdVo credential = CrdVo.builder()
                .mbrAcctIdx(account.getIdx())
                .ttl(request.title())
                .lgnId(request.loginId())
                .pwd(request.password())
                .memo(request.memo())
                .createdBy(loginId)
                .build();
        vaultMapper.insertCredential(credential);
        replaceCredentialCategoryMappings(credential.getIdx(), categoryIdxList);

        CredentialResponse response = getCredentialByIndex(credential.getIdx());
        log.info("saveCredential 완료 - loginId: {}, uuid: {}", loginId, response.credentialUuid());
        return response;
    }

    // 로그인 사용자의 자격증명을 수정한다.
    public CredentialResponse updateCredential(String uuid, SaveCredentialRequest request, String loginId) {
        log.info("updateCredential 시작 - loginId: {}, uuid: {}", loginId, uuid);
        AcctVo account = getAccountByLoginId(loginId);
        CrdVo credential = getCredentialVo(uuid, account.getIdx());
        List<Long> categoryIdxList = getCategoryIndexes(request.categoryUuids());

        credential.setTtl(request.title());
        credential.setLgnId(request.loginId());
        credential.setPwd(request.password());
        credential.setMemo(request.memo());
        credential.setUpdatedBy(loginId);

        vaultMapper.updateCredential(credential);
        replaceCredentialCategoryMappings(credential.getIdx(), categoryIdxList);

        CredentialResponse response = getCredential(uuid, loginId);
        log.info("updateCredential 완료 - loginId: {}, uuid: {}", loginId, uuid);
        return response;
    }

    // 로그인 사용자의 자격증명을 삭제한다.
    public void deleteCredential(String uuid, String loginId) {
        log.info("deleteCredential 시작 - loginId: {}, uuid: {}", loginId, uuid);
        AcctVo account = getAccountByLoginId(loginId);
        CrdVo credential = getCredentialVo(uuid, account.getIdx());
        vaultCredentialCategoryMapper.deleteCredentialCategoryMappings(credential.getIdx());
        vaultMapper.deleteCredential(uuid, account.getIdx(), loginId);
        log.info("deleteCredential 완료 - loginId: {}, uuid: {}", loginId, uuid);
    }

    private CrdVo getCredentialVo(String uuid, Long mbrAcctIdx) {
        CrdVo credential = vaultMapper.selectCredential(uuid, mbrAcctIdx);

        if (credential == null) {
            log.error("getCredentialVo 실패 - 원인: 자격증명 정보를 찾을 수 없습니다. uuid={}, memberAccountIdx={}", uuid, mbrAcctIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "자격증명 정보를 찾을 수 없습니다.");
        }

        return credential;
    }

    @Transactional(readOnly = true)
    private CredentialResponse getCredentialByIndex(Long idx) {
        CrdVo credential = vaultMapper.selectCredentialByIdx(idx);

        if (credential == null) {
            log.error("getCredentialByIndex 실패 - 원인: 자격증명 정보를 찾을 수 없습니다. idx={}", idx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "자격증명 정보를 찾을 수 없습니다.");
        }

        Map<Long, CatVo> categoryByIdx = getCategoryByIdxMap();
        Map<Long, List<CatVo>> categoriesByCredentialIdx = getCategoriesByCredentialIdx(List.of(credential), categoryByIdx);
        return VaultConvert.toResponse(credential, categoriesByCredentialIdx);
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private List<String> normalizeKeywordTokens(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return MULTI_WHITESPACE_PATTERN.splitAsStream(keyword.trim())
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .distinct()
                .toList();
    }

    private List<String> normalizeCategoryUuids(List<String> categoryUuids) {
        if (categoryUuids == null || categoryUuids.isEmpty()) {
            return List.of();
        }

        return categoryUuids.stream()
                .filter(categoryUuid -> categoryUuid != null && !categoryUuid.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<Long> getCategoryIndexes(List<String> categoryUuids) {
        if (categoryUuids == null || categoryUuids.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueCategoryUuids = new LinkedHashSet<>();
        for (String categoryUuid : categoryUuids) {
            if (categoryUuid != null && !categoryUuid.isBlank()) {
                uniqueCategoryUuids.add(categoryUuid);
            }
        }

        if (uniqueCategoryUuids.isEmpty()) {
            return List.of();
        }

        List<Long> categoryIdxList = new ArrayList<>();
        for (String categoryUuid : uniqueCategoryUuids) {
            CatVo category = vaultCategoryMapper.selectCategory(categoryUuid);

            if (category == null) {
                log.error("getCategoryIndexes 실패 - 원인: 카테고리를 찾을 수 없습니다. categoryUuid={}", categoryUuid);
                throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
            }

            categoryIdxList.add(category.getIdx());
        }

        return categoryIdxList;
    }

    private Map<Long, CatVo> getCategoryByIdxMap() {
        Map<Long, CatVo> categoryByIdx = new HashMap<>();

        for (CatVo category : vaultCategoryMapper.selectCategoryList()) {
            categoryByIdx.put(category.getIdx(), category);
        }

        return categoryByIdx;
    }

    private Map<Long, List<CatVo>> getCategoriesByCredentialIdx(List<CrdVo> credentialList, Map<Long, CatVo> categoryByIdx) {
        Map<Long, List<CatVo>> categoriesByCredentialIdx = new HashMap<>();

        if (credentialList.isEmpty()) {
            return categoriesByCredentialIdx;
        }

        List<Long> credentialIdxList = credentialList.stream()
                .map(CrdVo::getIdx)
                .toList();

        for (CrdCatVo mapping : vaultCredentialCategoryMapper.selectCredentialCategoryMappings(credentialIdxList)) {
            CatVo category = categoryByIdx.get(mapping.getTbVltCatIdx());

            if (category == null) {
                continue;
            }

            categoriesByCredentialIdx
                    .computeIfAbsent(mapping.getTbVltCrdIdx(), ignored -> new ArrayList<>())
                    .add(category);
        }

        return categoriesByCredentialIdx;
    }

    private void replaceCredentialCategoryMappings(Long credentialIdx, List<Long> categoryIdxList) {
        vaultCredentialCategoryMapper.deleteCredentialCategoryMappings(credentialIdx);

        if (!categoryIdxList.isEmpty()) {
            vaultCredentialCategoryMapper.insertCredentialCategoryMappings(credentialIdx, categoryIdxList);
        }
    }
}
