package com.gw.api.service.vault;

import com.gw.api.convert.vault.VaultCategoryConvert;
import com.gw.api.dto.vault.SaveVaultCategoryRequest;
import com.gw.api.dto.vault.VaultCategoryResponse;
import com.gw.infra.db.mapper.vault.VaultCategoryMapper;
import com.gw.infra.db.mapper.vault.VaultCredentialCategoryMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.vault.CatVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class VaultCategoryService {

    private final VaultCategoryMapper vaultCategoryMapper;
    private final VaultCredentialCategoryMapper vaultCredentialCategoryMapper;

    public VaultCategoryService(
            VaultCategoryMapper vaultCategoryMapper,
            VaultCredentialCategoryMapper vaultCredentialCategoryMapper
    ) {
        this.vaultCategoryMapper = vaultCategoryMapper;
        this.vaultCredentialCategoryMapper = vaultCredentialCategoryMapper;
    }

    /** 금고 카테고리 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<VaultCategoryResponse> getCategoryList() {
        log.info("금고 카테고리 목록 조회를 시작합니다.");
        List<VaultCategoryResponse> categoryList = vaultCategoryMapper.selectCategoryList().stream()
                .map(VaultCategoryConvert::toResponse)
                .toList();
        log.info("금고 카테고리 목록 조회를 완료했습니다. count={}", categoryList.size());
        return categoryList;
    }

    /** 금고 카테고리를 등록한다. */
    public VaultCategoryResponse saveCategory(SaveVaultCategoryRequest request, String loginId) {
        log.info("금고 카테고리 등록을 시작합니다. loginId={}, categoryName={}", loginId, request.name());

        try {
            validateDuplicateName(request.name(), null);

            CatVo category = CatVo.builder()
                    .nm(request.name())
                    .dsc(request.description())
                    .color(request.color())
                    .sortOrd(request.sortOrder() == null ? 0 : request.sortOrder())
                    .createdBy(loginId)
                    .build();

            vaultCategoryMapper.insertCategory(category);
            VaultCategoryResponse response = VaultCategoryConvert.toResponse(getCategoryByIdx(category.getIdx()));
            log.info("금고 카테고리 등록을 완료했습니다. loginId={}, categoryUuid={}", loginId, response.categoryUuid());
            return response;
        } catch (BusinessException exception) {
            log.warn("금고 카테고리 등록에 실패했습니다. loginId={}, categoryName={}, error={}",
                    loginId, request.name(), exception.getMessage());
            throw exception;
        }
    }

    /** 금고 카테고리를 수정한다. */
    public VaultCategoryResponse updateCategory(String uuid, SaveVaultCategoryRequest request, String loginId) {
        log.info("금고 카테고리 수정을 시작합니다. loginId={}, categoryUuid={}", loginId, uuid);

        try {
            CatVo category = getCategory(uuid);
            validateDuplicateName(request.name(), uuid);
            category.setNm(request.name());
            category.setDsc(request.description());
            category.setColor(request.color());
            category.setSortOrd(request.sortOrder() == null ? 0 : request.sortOrder());
            category.setUpdatedBy(loginId);
            vaultCategoryMapper.updateCategory(category);
            VaultCategoryResponse response = VaultCategoryConvert.toResponse(getCategory(uuid));
            log.info("금고 카테고리 수정을 완료했습니다. loginId={}, categoryUuid={}", loginId, uuid);
            return response;
        } catch (BusinessException exception) {
            log.warn("금고 카테고리 수정에 실패했습니다. loginId={}, categoryUuid={}, error={}",
                    loginId, uuid, exception.getMessage());
            throw exception;
        }
    }

    /** 금고 카테고리를 삭제한다. */
    public void deleteCategory(String uuid, String loginId) {
        log.info("금고 카테고리 삭제를 시작합니다. loginId={}, categoryUuid={}", loginId, uuid);

        try {
            CatVo category = getCategory(uuid);
            vaultCredentialCategoryMapper.deleteCategoryMappings(category.getIdx());
            vaultCategoryMapper.deleteCategory(uuid, loginId);
            log.info("금고 카테고리 삭제를 완료했습니다. loginId={}, categoryUuid={}", loginId, uuid);
        } catch (BusinessException exception) {
            log.warn("금고 카테고리 삭제에 실패했습니다. loginId={}, categoryUuid={}, error={}",
                    loginId, uuid, exception.getMessage());
            throw exception;
        }
    }

    /** 금고 카테고리 정보를 조회한다. */
    @Transactional(readOnly = true)
    public CatVo getCategory(String uuid) {
        CatVo category = vaultCategoryMapper.selectCategory(uuid);

        if (category == null) {
            log.warn("금고 카테고리 조회에 실패했습니다. categoryUuid={}", uuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    /** 금고 카테고리 식별자로 정보를 조회한다. */
    @Transactional(readOnly = true)
    public CatVo getCategoryByIdx(Long idx) {
        CatVo category = vaultCategoryMapper.selectCategoryByIdx(idx);

        if (category == null) {
            log.warn("금고 카테고리 식별자 조회에 실패했습니다. categoryIdx={}", idx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    private void validateDuplicateName(String name, String excludeUuid) {
        if (vaultCategoryMapper.existsByName(name, excludeUuid)) {
            log.warn("중복된 금고 카테고리 이름이 감지되었습니다. categoryName={}, excludeUuid={}", name, excludeUuid);
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 카테고리 이름입니다.");
        }
    }
}
