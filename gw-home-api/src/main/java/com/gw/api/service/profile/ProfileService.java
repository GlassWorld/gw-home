package com.gw.api.service.profile;

import com.gw.api.dto.profile.MemoResponse;
import com.gw.api.dto.profile.ProfileResponse;
import com.gw.api.dto.profile.SaveMemoRequest;
import com.gw.api.dto.profile.UpdateProfileRequest;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.profile.ProfileMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.profile.PrflVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final AccountMapper accountMapper;

    public ProfileService(ProfileMapper profileMapper, AccountMapper accountMapper) {
        this.profileMapper = profileMapper;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String profileUuid) {
        log.info("getProfile 시작 - profileUuid: {}", profileUuid);
        ProfileResponse response = toResponse(getProfileByUuid(profileUuid));
        log.info("getProfile 완료");
        return response;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String loginId) {
        log.info("getMyProfile 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        PrflVo profile = profileMapper.selectProfileByAccountIdx(account.getIdx());

        if (profile == null) {
            log.error("getMyProfile 실패 - 원인: 프로필을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        log.info("getMyProfile 완료");
        return toResponse(profile);
    }

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
        return toResponse(profileMapper.selectProfileByAccountIdx(account.getIdx()));
    }

    @Transactional(readOnly = true)
    public MemoResponse getMemo(String loginId) {
        log.info("getMemo 시작 - loginId: {}", loginId);
        AcctVo account = getAccountByLoginId(loginId);
        String memo = profileMapper.selectMemoByAccountIdx(account.getIdx());
        log.info("getMemo 완료");
        return new MemoResponse(memo == null ? "" : memo);
    }

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
        return new MemoResponse(memo);
    }

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
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            log.error("getAccountByLoginId 실패 - 원인: 계정을 찾을 수 없습니다. loginId={}", loginId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    private ProfileResponse toResponse(PrflVo profile) {
        return new ProfileResponse(
                profile.getUuid(),
                profile.getNickNm(),
                profile.getIntro(),
                profile.getPrflImgUrl(),
                profile.getCreatedAt()
        );
    }
}
