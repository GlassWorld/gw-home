package com.gw.api.service.profile;

import com.gw.api.convert.profile.ProfileConvert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.api.dto.profile.MemoResponse;
import com.gw.api.dto.profile.NavigationFavoriteResponse;
import com.gw.api.dto.profile.ProfileResponse;
import com.gw.api.dto.profile.SaveMemoRequest;
import com.gw.api.dto.profile.SaveNavigationFavoriteRequest;
import com.gw.api.dto.profile.UpdateProfileRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.profile.ProfileMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.ProfilePolicy;
import com.gw.share.common.policy.RolePolicy;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.profile.PrflVo;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ProfileService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final ProfileMapper profileMapper;
    private final AccountLookupService accountLookupService;
    private final ObjectMapper objectMapper;

    public ProfileService(ProfileMapper profileMapper, AccountLookupService accountLookupService, ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.accountLookupService = accountLookupService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    // 공개 프로필을 조회한다.
    public ProfileResponse getProfile(String profileUuid) {
        log.info("getProfile 시작 - profileUuid: {}", profileUuid);
        ProfileResponse response = ProfileConvert.toResponse(getProfileByUuid(profileUuid));
        log.info("getProfile 완료");
        return response;
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 프로필을 조회한다.
    public ProfileResponse getMyProfile(String loginId) {
        log.info("getMyProfile 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        PrflVo profile = profileMapper.selectProfileByAccountIdx(account.getIdx());

        if (profile == null) {
            log.error("getMyProfile 실패 - 원인: 프로필을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        log.info("getMyProfile 완료");
        return ProfileConvert.toResponse(profile);
    }

    // 로그인 사용자의 프로필을 수정한다.
    public ProfileResponse updateMyProfile(String loginId, UpdateProfileRequest request) {
        log.info("updateMyProfile 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        PrflVo profile = profileMapper.selectProfileByAccountIdx(account.getIdx());

        if (profile == null) {
            log.error("updateMyProfile 실패 - 원인: 프로필을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        profile.setNickNm(request.nickname());
        profile.setIntro(request.introduction());
        profile.setPrflImgUrl(request.profileImageUrl());
        profile.setUpdatedBy(loginId);
        profileMapper.updateProfile(profile);

        log.info("updateMyProfile 완료");
        return ProfileConvert.toResponse(profileMapper.selectProfileByAccountIdx(account.getIdx()));
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 메모를 조회한다.
    public MemoResponse getMemo(String loginId) {
        log.info("getMemo 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        String memo = profileMapper.selectMemoByAccountIdx(account.getIdx());
        log.info("getMemo 완료");
        return ProfileConvert.toMemoResponse(memo);
    }

    // 로그인 사용자의 메모를 저장한다.
    public MemoResponse saveMemo(String loginId, SaveMemoRequest request) {
        log.info("saveMemo 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        PrflVo profile = profileMapper.selectProfileByAccountIdx(account.getIdx());

        if (profile == null) {
            log.error("saveMemo 실패 - 원인: 프로필을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        String memo = request.memo() == null ? "" : request.memo();
        profileMapper.updateMemo(account.getIdx(), memo, loginId);
        log.info("saveMemo 완료");
        return ProfileConvert.toMemoResponse(memo);
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 내비게이션 즐겨찾기를 조회한다.
    public NavigationFavoriteResponse getNavigationFavorites(String loginId) {
        log.info("getNavigationFavorites 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        String favoriteMenusJson = profileMapper.selectNavigationFavoritesByAccountIdx(account.getIdx());
        List<String> favoriteMenus = filterNavigationFavoritesByRole(parseNavigationFavorites(favoriteMenusJson), account.getRole());
        log.info("getNavigationFavorites 완료");
        return ProfileConvert.toNavigationFavoriteResponse(favoriteMenus);
    }

    // 로그인 사용자의 내비게이션 즐겨찾기를 저장한다.
    public NavigationFavoriteResponse saveNavigationFavorites(String loginId, SaveNavigationFavoriteRequest request) {
        log.info("saveNavigationFavorites 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        PrflVo profile = profileMapper.selectProfileByAccountIdx(account.getIdx());

        if (profile == null) {
            log.error("saveNavigationFavorites 실패 - 원인: 프로필을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        List<String> normalizedFavoriteMenus = normalizeNavigationFavorites(request.favoriteMenus(), account.getRole());
        profileMapper.updateNavigationFavorites(account.getIdx(), writeNavigationFavorites(normalizedFavoriteMenus), loginId);
        log.info("saveNavigationFavorites 완료");
        return ProfileConvert.toNavigationFavoriteResponse(normalizedFavoriteMenus);
    }

    // 회원 기본 프로필을 생성한다.
    public void createDefaultProfile(AcctVo account) {
        log.info("createDefaultProfile 시작 - loginId: {}", account.getLgnId());
        PrflVo profile = PrflVo.builder()
                .mbrAcctIdx(account.getIdx())
                .nickNm(account.getLgnId())
                .createdBy(account.getLgnId())
                .build();
        profileMapper.insertProfile(profile);
        log.info("createDefaultProfile 완료");
    }

    private PrflVo getProfileByUuid(String profileUuid) {
        PrflVo profile = profileMapper.selectProfileByUuid(profileUuid);

        if (profile == null) {
            log.error("getProfileByUuid 실패 - 원인: 프로필을 찾을 수 없습니다. profileUuid={}", profileUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        return profile;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private List<String> normalizeNavigationFavorites(List<String> favoriteMenus, String role) {
        if (favoriteMenus == null || favoriteMenus.isEmpty()) {
            return List.of();
        }

        if (favoriteMenus.size() > ProfilePolicy.MAX_FAVORITE_MENU_COUNT) {
            log.error("normalizeNavigationFavorites 실패 - 원인: 즐겨찾기 메뉴 개수 초과. role={}, count={}", role, favoriteMenus.size());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "즐겨찾기 메뉴는 5개까지 저장할 수 있습니다.");
        }

        Set<String> deduplicatedFavoriteMenus = new LinkedHashSet<>();

        for (String favoriteMenu : favoriteMenus) {
            if (favoriteMenu == null || favoriteMenu.isBlank()) {
                log.error("normalizeNavigationFavorites 실패 - 원인: 즐겨찾기 메뉴 경로가 비어 있습니다. role={}", role);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "즐겨찾기 메뉴 경로가 올바르지 않습니다.");
            }

            if (!isAllowedNavigationMenu(favoriteMenu, role)) {
                log.error("normalizeNavigationFavorites 실패 - 원인: 저장할 수 없는 즐겨찾기 메뉴입니다. role={}, favoriteMenu={}", role, favoriteMenu);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "저장할 수 없는 즐겨찾기 메뉴입니다.");
            }

            if (!deduplicatedFavoriteMenus.add(favoriteMenu)) {
                log.error("normalizeNavigationFavorites 실패 - 원인: 중복된 즐겨찾기 메뉴입니다. role={}, favoriteMenu={}", role, favoriteMenu);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "중복된 즐겨찾기 메뉴는 저장할 수 없습니다.");
            }
        }

        return List.copyOf(deduplicatedFavoriteMenus);
    }

    private List<String> filterNavigationFavoritesByRole(List<String> favoriteMenus, String role) {
        return favoriteMenus.stream()
                .filter(favoriteMenu -> isAllowedNavigationMenu(favoriteMenu, role))
                .toList();
    }

    private boolean isAllowedNavigationMenu(String favoriteMenu, String role) {
        if (ProfilePolicy.USER_NAVIGATION_MENU_PATHS.contains(favoriteMenu)) {
            return true;
        }

        return RolePolicy.ADMIN.equals(role) && ProfilePolicy.ADMIN_NAVIGATION_MENU_PATHS.contains(favoriteMenu);
    }

    private List<String> parseNavigationFavorites(String favoriteMenusJson) {
        if (favoriteMenusJson == null || favoriteMenusJson.isBlank()) {
            return List.of();
        }

        try {
            List<String> favoriteMenus = objectMapper.readValue(favoriteMenusJson, STRING_LIST_TYPE_REFERENCE);
            return favoriteMenus == null ? List.of() : favoriteMenus;
        } catch (JsonProcessingException exception) {
            log.error("parseNavigationFavorites 실패 - 저장된 JSON 파싱 오류", exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "즐겨찾기 메뉴를 읽을 수 없습니다.");
        }
    }

    private String writeNavigationFavorites(List<String> favoriteMenus) {
        try {
            return objectMapper.writeValueAsString(favoriteMenus);
        } catch (JsonProcessingException exception) {
            log.error("writeNavigationFavorites 실패 - JSON 직렬화 오류", exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "즐겨찾기 메뉴를 저장할 수 없습니다.");
        }
    }
}
