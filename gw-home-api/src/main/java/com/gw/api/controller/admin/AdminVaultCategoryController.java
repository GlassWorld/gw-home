package com.gw.api.controller.admin;

import com.gw.api.dto.vault.SaveVaultCategoryRequest;
import com.gw.api.dto.vault.VaultCategoryResponse;
import com.gw.api.service.vault.VaultCategoryService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/vault-categories")
public class AdminVaultCategoryController {

    private final VaultCategoryService vaultCategoryService;

    public AdminVaultCategoryController(VaultCategoryService vaultCategoryService) {
        this.vaultCategoryService = vaultCategoryService;
    }

    @GetMapping
    public ApiResponse<List<VaultCategoryResponse>> getCategories() {
        return ApiResponse.ok(vaultCategoryService.getCategoryList());
    }

    @PostMapping
    public ApiResponse<VaultCategoryResponse> saveCategory(
            Principal principal,
            @Valid @RequestBody SaveVaultCategoryRequest request
    ) {
        return ApiResponse.ok(vaultCategoryService.saveCategory(request, getLoginId(principal)));
    }

    @PutMapping("/{uuid}")
    public ApiResponse<VaultCategoryResponse> updateCategory(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody SaveVaultCategoryRequest request
    ) {
        return ApiResponse.ok(vaultCategoryService.updateCategory(uuid, request, getLoginId(principal)));
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteCategory(Principal principal, @PathVariable String uuid) {
        vaultCategoryService.deleteCategory(uuid, getLoginId(principal));
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
