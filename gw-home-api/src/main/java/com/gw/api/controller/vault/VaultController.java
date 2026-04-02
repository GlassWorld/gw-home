package com.gw.api.controller.vault;

import com.gw.api.dto.vault.CredentialResponse;
import com.gw.api.dto.vault.SaveCredentialRequest;
import com.gw.api.dto.vault.VaultCategoryResponse;
import com.gw.api.service.vault.VaultCategoryService;
import com.gw.api.service.vault.VaultService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vault")
public class VaultController {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    private final VaultService vaultService;
    private final VaultCategoryService vaultCategoryService;

    public VaultController(VaultService vaultService, VaultCategoryService vaultCategoryService) {
        this.vaultService = vaultService;
        this.vaultCategoryService = vaultCategoryService;
    }

    // 로그인 사용자의 자격증명 목록을 조회한다.
    @GetMapping("/credentials")
    public ApiResponse<List<CredentialResponse>> getCredentialList(
            Principal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryUuid,
            @RequestParam(required = false) List<String> categoryUuids
    ) {
        List<String> requestedCategoryUuids = new ArrayList<>();

        if (categoryUuids != null) {
            for (String categoryValue : categoryUuids) {
                if (categoryValue == null || categoryValue.isBlank()) {
                    continue;
                }

                for (String categoryToken : COMMA_PATTERN.split(categoryValue)) {
                    if (!categoryToken.isBlank()) {
                        requestedCategoryUuids.add(categoryToken);
                    }
                }
            }
        }

        if (categoryUuid != null && !categoryUuid.isBlank()) {
            requestedCategoryUuids.add(categoryUuid);
        }

        return ApiResponse.ok(vaultService.getCredentialList(keyword, requestedCategoryUuids, getLoginId(principal)));
    }

    // 자격증명 카테고리 목록을 조회한다.
    @GetMapping("/categories")
    public ApiResponse<List<VaultCategoryResponse>> getCategories() {
        return ApiResponse.ok(vaultCategoryService.getCategoryList());
    }

    // 로그인 사용자의 자격증명 상세 정보를 조회한다.
    @GetMapping("/credentials/{uuid}")
    public ApiResponse<CredentialResponse> getCredential(Principal principal, @PathVariable String uuid) {
        return ApiResponse.ok(vaultService.getCredential(uuid, getLoginId(principal)));
    }

    // 로그인 사용자가 자격증명을 생성한다.
    @PostMapping("/credentials")
    public ApiResponse<CredentialResponse> saveCredential(
            Principal principal,
            @Valid @RequestBody SaveCredentialRequest request
    ) {
        return ApiResponse.ok(vaultService.saveCredential(request, getLoginId(principal)));
    }

    // 로그인 사용자가 자격증명을 수정한다.
    @PutMapping("/credentials/{uuid}")
    public ApiResponse<CredentialResponse> updateCredential(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody SaveCredentialRequest request
    ) {
        return ApiResponse.ok(vaultService.updateCredential(uuid, request, getLoginId(principal)));
    }

    // 로그인 사용자가 자격증명을 삭제한다.
    @DeleteMapping("/credentials/{uuid}")
    public ApiResponse<Void> deleteCredential(Principal principal, @PathVariable String uuid) {
        vaultService.deleteCredential(uuid, getLoginId(principal));
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
