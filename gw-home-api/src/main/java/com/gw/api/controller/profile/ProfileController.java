package com.gw.api.controller.profile;

import com.gw.api.dto.profile.MemoResponse;
import com.gw.api.dto.profile.NavigationFavoriteResponse;
import com.gw.api.dto.profile.ProfileResponse;
import com.gw.api.dto.profile.SaveMemoRequest;
import com.gw.api.dto.profile.SaveNavigationFavoriteRequest;
import com.gw.api.dto.profile.UpdateProfileRequest;
import com.gw.api.service.profile.ProfileService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // 공개 프로필 정보를 조회한다.
    @GetMapping("/{profileUuid}")
    public ApiResponse<ProfileResponse> getProfile(@PathVariable String profileUuid) {
        return ApiResponse.ok(profileService.getProfile(profileUuid));
    }

    // 로그인 사용자의 프로필 정보를 조회한다.
    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getMyProfile(Principal principal) {
        return ApiResponse.ok(profileService.getMyProfile(getLoginId(principal)));
    }

    // 로그인 사용자의 메모를 조회한다.
    @GetMapping("/me/memo")
    public ApiResponse<MemoResponse> getMyMemo(Principal principal) {
        return ApiResponse.ok(profileService.getMemo(getLoginId(principal)));
    }

    // 로그인 사용자의 네비게이션 즐겨찾기를 조회한다.
    @GetMapping("/me/navigation-favorites")
    public ApiResponse<NavigationFavoriteResponse> getMyNavigationFavorites(Principal principal) {
        return ApiResponse.ok(profileService.getNavigationFavorites(getLoginId(principal)));
    }

    // 로그인 사용자의 프로필 정보를 수정한다.
    @PutMapping("/me")
    public ApiResponse<ProfileResponse> updateMyProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.ok(profileService.updateMyProfile(getLoginId(principal), request));
    }

    // 로그인 사용자의 메모를 저장한다.
    @PutMapping("/me/memo")
    public ApiResponse<MemoResponse> saveMyMemo(
            Principal principal,
            @Valid @RequestBody SaveMemoRequest request
    ) {
        return ApiResponse.ok(profileService.saveMemo(getLoginId(principal), request));
    }

    // 로그인 사용자의 네비게이션 즐겨찾기를 저장한다.
    @PutMapping("/me/navigation-favorites")
    public ApiResponse<NavigationFavoriteResponse> saveMyNavigationFavorites(
            Principal principal,
            @Valid @RequestBody SaveNavigationFavoriteRequest request
    ) {
        return ApiResponse.ok(profileService.saveNavigationFavorites(getLoginId(principal), request));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
