package com.gw.api.service.vault;

import com.gw.api.dto.vault.SaveVaultCategoryRequest;
import com.gw.api.dto.vault.VaultCategoryResponse;
import com.gw.infra.db.mapper.vault.VaultMapper;
import com.gw.infra.db.mapper.vault.VaultCategoryMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.vault.CatVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VaultCategoryService {

    private final VaultCategoryMapper vaultCategoryMapper;
    private final VaultMapper vaultMapper;

    public VaultCategoryService(VaultCategoryMapper vaultCategoryMapper, VaultMapper vaultMapper) {
        this.vaultCategoryMapper = vaultCategoryMapper;
        this.vaultMapper = vaultMapper;
    }

    @Transactional(readOnly = true)
    public List<VaultCategoryResponse> getCategoryList() {
        return vaultCategoryMapper.selectCategoryList().stream()
                .map(this::toResponse)
                .toList();
    }

    public VaultCategoryResponse saveCategory(SaveVaultCategoryRequest request, String loginId) {
        validateDuplicateName(request.name(), null);

        CatVo category = CatVo.builder()
                .nm(request.name())
                .dsc(request.description())
                .sortOrd(request.sortOrder() == null ? 0 : request.sortOrder())
                .createdBy(loginId)
                .build();

        vaultCategoryMapper.insertCategory(category);
        return toResponse(getCategoryByIdx(category.getIdx()));
    }

    public VaultCategoryResponse updateCategory(String uuid, SaveVaultCategoryRequest request, String loginId) {
        CatVo category = getCategory(uuid);
        validateDuplicateName(request.name(), uuid);
        category.setNm(request.name());
        category.setDsc(request.description());
        category.setSortOrd(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setUpdatedBy(loginId);
        vaultCategoryMapper.updateCategory(category);
        return toResponse(getCategory(uuid));
    }

    public void deleteCategory(String uuid, String loginId) {
        CatVo category = getCategory(uuid);
        vaultMapper.clearCredentialCategory(category.getIdx());
        vaultCategoryMapper.deleteCategory(uuid, loginId);
    }

    @Transactional(readOnly = true)
    public CatVo getCategory(String uuid) {
        CatVo category = vaultCategoryMapper.selectCategory(uuid);

        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    @Transactional(readOnly = true)
    public CatVo getCategoryByIdx(Long idx) {
        CatVo category = vaultCategoryMapper.selectCategoryByIdx(idx);

        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    private void validateDuplicateName(String name, String excludeUuid) {
        if (vaultCategoryMapper.existsByName(name, excludeUuid)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 카테고리 이름입니다.");
        }
    }

    private VaultCategoryResponse toResponse(CatVo category) {
        return new VaultCategoryResponse(
                category.getUuid(),
                category.getNm(),
                category.getDsc(),
                category.getSortOrd()
        );
    }
}
