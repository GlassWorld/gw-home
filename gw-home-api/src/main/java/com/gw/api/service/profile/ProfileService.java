package com.gw.api.service.profile;

import com.gw.api.dto.profile.ProfileResponse;
import com.gw.api.dto.profile.UpdateProfileRequest;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.profile.ProfileMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.profile.PrflVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final AccountMapper accountMapper;

    public ProfileService(ProfileMapper profileMapper, AccountMapper accountMapper) {
        this.profileMapper = profileMapper;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String profileUuid) {
        return toResponse(getProfileByUuid(profileUuid));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String loginId) {
        AcctVo account = getAccountByLoginId(loginId);
        PrflVo profile = profileMapper.selectProfileByAccountIdx(account.getIdx());

        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        return toResponse(profile);
    }

    public ProfileResponse updateMyProfile(String loginId, UpdateProfileRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        PrflVo profile = profileMapper.selectProfileByAccountIdx(account.getIdx());

        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        profile.setNickNm(request.nickname());
        profile.setIntro(request.introduction());
        profile.setPrflImgUrl(request.profileImageUrl());
        profile.setUpdatedBy(loginId);
        profileMapper.updateProfile(profile);

        return toResponse(profileMapper.selectProfileByAccountIdx(account.getIdx()));
    }

    public void createDefaultProfile(AcctVo account) {
        PrflVo profile = PrflVo.builder()
                .mbrAcctIdx(account.getIdx())
                .nickNm(account.getLgnId())
                .createdBy(account.getLgnId())
                .build();
        profileMapper.insertProfile(profile);
    }

    private PrflVo getProfileByUuid(String profileUuid) {
        PrflVo profile = profileMapper.selectProfileByUuid(profileUuid);

        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }

        return profile;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
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
