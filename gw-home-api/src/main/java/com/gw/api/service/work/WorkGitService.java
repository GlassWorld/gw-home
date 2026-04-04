package com.gw.api.service.work;

import com.gw.api.convert.work.WorkGitConvert;
import com.gw.api.dto.work.WorkGitAccountRequest;
import com.gw.api.dto.work.WorkGitAccountResponse;
import com.gw.api.dto.work.WorkGitConnectionTestResponse;
import com.gw.api.dto.work.WorkGitProjectRequest;
import com.gw.api.dto.work.WorkGitProjectResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.work.WorkGitMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.GitProviderPolicy;
import com.gw.share.common.policy.UseYnPolicy;
import com.gw.share.common.util.WorkGitAccessTokenEncryptor;
import com.gw.share.util.StringUtil;
import com.gw.share.util.ValidationUtil;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkGitAcctVo;
import com.gw.share.vo.work.WorkGitPrjVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WorkGitService {

    private final WorkGitMapper workGitMapper;
    private final AccountLookupService accountLookupService;
    private final WorkGitAccessTokenEncryptor workGitAccessTokenEncryptor;
    private final WorkGitCommitClient workGitCommitClient;

    public WorkGitService(
            WorkGitMapper workGitMapper,
            AccountLookupService accountLookupService,
            WorkGitAccessTokenEncryptor workGitAccessTokenEncryptor,
            WorkGitCommitClient workGitCommitClient
    ) {
        this.workGitMapper = workGitMapper;
        this.accountLookupService = accountLookupService;
        this.workGitAccessTokenEncryptor = workGitAccessTokenEncryptor;
        this.workGitCommitClient = workGitCommitClient;
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 Git 계정 목록을 조회한다.
    public List<WorkGitAccountResponse> getGitAccounts(String loginId) {
        log.info("getGitAccounts 시작 - loginId: {}", loginId);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            List<WorkGitAccountResponse> response = workGitMapper.selectGitAccounts(account.getIdx()).stream()
                    .map(WorkGitConvert::toAccountResponse)
                    .toList();
            log.info("getGitAccounts 완료 - loginId: {}, count: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getGitAccounts 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 Git 계정을 생성한다.
    public WorkGitAccountResponse createGitAccount(String loginId, WorkGitAccountRequest request) {
        log.info("createGitAccount 시작 - loginId: {}, request: {}", loginId, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkGitAcctVo gitAccount = WorkGitAcctVo.builder()
                    .mbrAcctIdx(account.getIdx())
                    .prvdCd(normalizeProvider(request.provider()))
                    .acctLbl(requireText(request.accountLabel(), "accountLabel은 필수입니다."))
                    .authNm(requireText(request.authorName(), "authorName은 필수입니다."))
                    .acsToknEnc(encryptToken(request.accessToken()))
                    .useYn(normalizeUseYn(request.useYn()))
                    .createdBy(loginId)
                    .build();
            workGitMapper.insertGitAccount(gitAccount);
            WorkGitAccountResponse response = WorkGitConvert.toAccountResponse(getGitAccountByIdx(gitAccount.getIdx(), account.getIdx()));
            log.info("createGitAccount 완료 - loginId: {}, uuid: {}", loginId, response.gitAccountUuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "createGitAccount 실패 - loginId: {}, request: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    request,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 Git 계정을 수정한다.
    public WorkGitAccountResponse updateGitAccount(String loginId, String uuid, WorkGitAccountRequest request) {
        log.info("updateGitAccount 시작 - loginId: {}, uuid: {}, request: {}", loginId, uuid, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkGitAcctVo gitAccount = getGitAccount(uuid, account.getIdx());
            gitAccount.setPrvdCd(normalizeProvider(request.provider()));
            gitAccount.setAcctLbl(requireText(request.accountLabel(), "accountLabel은 필수입니다."));
            gitAccount.setAuthNm(requireText(request.authorName(), "authorName은 필수입니다."));
            gitAccount.setAcsToknEnc(resolveUpdatedToken(gitAccount.getAcsToknEnc(), request.accessToken()));
            gitAccount.setUseYn(normalizeUseYn(request.useYn()));
            gitAccount.setUpdatedBy(loginId);
            workGitMapper.updateGitAccount(gitAccount);
            WorkGitAccountResponse response = WorkGitConvert.toAccountResponse(getGitAccount(uuid, account.getIdx()));
            log.info("updateGitAccount 완료 - loginId: {}, uuid: {}", loginId, response.gitAccountUuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateGitAccount 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 Git 계정을 삭제한다.
    public void deleteGitAccount(String loginId, String uuid) {
        log.info("deleteGitAccount 시작 - loginId: {}, uuid: {}", loginId, uuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkGitAcctVo gitAccount = getGitAccount(uuid, account.getIdx());
            workGitMapper.deleteWorkUnitGitProjectsByGitAccount(gitAccount.getIdx(), account.getIdx(), loginId);
            workGitMapper.deleteGitProjectsByAccount(gitAccount.getIdx(), account.getIdx(), loginId);
            workGitMapper.deleteGitAccount(uuid, account.getIdx(), loginId);
            log.info("deleteGitAccount 완료 - loginId: {}, uuid: {}", loginId, uuid);
        } catch (BusinessException exception) {
            log.error(
                    "deleteGitAccount 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 Git 프로젝트 목록을 조회한다.
    public List<WorkGitProjectResponse> getGitProjects(String loginId, String gitAccountUuid) {
        log.info("getGitProjects 시작 - loginId: {}, gitAccountUuid: {}", loginId, gitAccountUuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            List<WorkGitProjectResponse> response = workGitMapper.selectGitProjects(account.getIdx(), normalizeOptionalText(gitAccountUuid)).stream()
                    .map(WorkGitConvert::toProjectResponse)
                    .toList();
            log.info("getGitProjects 완료 - loginId: {}, count: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getGitProjects 실패 - loginId: {}, gitAccountUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    gitAccountUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 Git 프로젝트 선택 옵션을 조회한다.
    public List<WorkGitProjectResponse> getGitProjectOptions(String loginId) {
        log.info("getGitProjectOptions 시작 - loginId: {}", loginId);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            List<WorkGitProjectResponse> response = workGitMapper.selectGitProjectOptions(account.getIdx()).stream()
                    .map(WorkGitConvert::toProjectResponse)
                    .toList();
            log.info("getGitProjectOptions 완료 - loginId: {}, count: {}", loginId, response.size());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getGitProjectOptions 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 Git 프로젝트를 생성한다.
    public WorkGitProjectResponse createGitProject(String loginId, WorkGitProjectRequest request) {
        log.info("createGitProject 시작 - loginId: {}, request: {}", loginId, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkGitAcctVo gitAccount = getGitAccount(request.gitAccountUuid(), account.getIdx());
            WorkGitPrjVo gitProject = WorkGitPrjVo.builder()
                    .wrkGitAcctIdx(gitAccount.getIdx())
                    .mbrAcctIdx(account.getIdx())
                    .prjNm(requireText(request.projectName(), "projectName은 필수입니다."))
                    .repoUrl(requireText(request.repositoryUrl(), "repositoryUrl은 필수입니다."))
                    .useYn(normalizeUseYn(request.useYn()))
                    .createdBy(loginId)
                    .build();
            workGitMapper.insertGitProject(gitProject);
            WorkGitProjectResponse response = WorkGitConvert.toProjectResponse(getGitProjectByIdx(gitProject.getIdx(), account.getIdx()));
            log.info("createGitProject 완료 - loginId: {}, uuid: {}", loginId, response.gitProjectUuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "createGitProject 실패 - loginId: {}, request: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    request,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 Git 프로젝트를 수정한다.
    public WorkGitProjectResponse updateGitProject(String loginId, String uuid, WorkGitProjectRequest request) {
        log.info("updateGitProject 시작 - loginId: {}, uuid: {}, request: {}", loginId, uuid, request);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkGitPrjVo gitProject = getGitProject(uuid, account.getIdx());
            WorkGitAcctVo gitAccount = getGitAccount(request.gitAccountUuid(), account.getIdx());
            gitProject.setWrkGitAcctIdx(gitAccount.getIdx());
            gitProject.setPrjNm(requireText(request.projectName(), "projectName은 필수입니다."));
            gitProject.setRepoUrl(requireText(request.repositoryUrl(), "repositoryUrl은 필수입니다."));
            gitProject.setUseYn(normalizeUseYn(request.useYn()));
            gitProject.setUpdatedBy(loginId);
            workGitMapper.updateGitProject(gitProject);
            WorkGitProjectResponse response = WorkGitConvert.toProjectResponse(getGitProject(uuid, account.getIdx()));
            log.info("updateGitProject 완료 - loginId: {}, uuid: {}", loginId, response.gitProjectUuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateGitProject 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 로그인 사용자의 Git 프로젝트를 삭제한다.
    public void deleteGitProject(String loginId, String uuid) {
        log.info("deleteGitProject 시작 - loginId: {}, uuid: {}", loginId, uuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkGitPrjVo gitProject = getGitProject(uuid, account.getIdx());
            workGitMapper.deleteWorkUnitGitProjectsByGitProject(gitProject.getIdx(), account.getIdx(), loginId);
            workGitMapper.deleteGitProject(uuid, account.getIdx(), loginId);
            log.info("deleteGitProject 완료 - loginId: {}, uuid: {}", loginId, uuid);
        } catch (BusinessException exception) {
            log.error(
                    "deleteGitProject 실패 - loginId: {}, uuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    uuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 로그인 사용자의 Git 프로젝트 연결 상태를 점검한다.
    public WorkGitConnectionTestResponse testGitProjectConnection(String loginId, String gitProjectUuid) {
        log.info("testGitProjectConnection 시작 - loginId: {}, gitProjectUuid: {}", loginId, gitProjectUuid);
        try {
            AcctVo account = getAccountByLoginId(loginId);
            WorkGitPrjVo gitProject = getGitProject(gitProjectUuid, account.getIdx());
            WorkGitConnectionTestResponse response = workGitCommitClient.testProjectConnection(gitProject);
            log.info("testGitProjectConnection 완료 - loginId: {}, gitProjectUuid: {}, connected: {}", loginId, gitProjectUuid, response.connected());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "testGitProjectConnection 실패 - loginId: {}, gitProjectUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    gitProjectUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private WorkGitAcctVo getGitAccount(String uuid, Long mbrAcctIdx) {
        WorkGitAcctVo gitAccount = workGitMapper.selectGitAccount(uuid, mbrAcctIdx);
        if (gitAccount == null) {
            log.error("getGitAccount 실패 - 원인: Git 계정을 찾을 수 없습니다. uuid={}, memberAccountIdx={}", uuid, mbrAcctIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "Git 계정을 찾을 수 없습니다.");
        }
        return gitAccount;
    }

    private WorkGitAcctVo getGitAccountByIdx(Long idx, Long mbrAcctIdx) {
        WorkGitAcctVo gitAccount = workGitMapper.selectGitAccountByIdx(idx, mbrAcctIdx);
        if (gitAccount == null) {
            log.error("getGitAccountByIdx 실패 - 원인: Git 계정을 찾을 수 없습니다. idx={}, memberAccountIdx={}", idx, mbrAcctIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "Git 계정을 찾을 수 없습니다.");
        }
        return gitAccount;
    }

    private WorkGitPrjVo getGitProject(String uuid, Long mbrAcctIdx) {
        WorkGitPrjVo gitProject = workGitMapper.selectGitProject(uuid, mbrAcctIdx);
        if (gitProject == null) {
            log.error("getGitProject 실패 - 원인: Git 프로젝트를 찾을 수 없습니다. uuid={}, memberAccountIdx={}", uuid, mbrAcctIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "Git 프로젝트를 찾을 수 없습니다.");
        }
        return gitProject;
    }

    private WorkGitPrjVo getGitProjectByIdx(Long idx, Long mbrAcctIdx) {
        WorkGitPrjVo gitProject = workGitMapper.selectGitProjectByIdx(idx, mbrAcctIdx);
        if (gitProject == null) {
            log.error("getGitProjectByIdx 실패 - 원인: Git 프로젝트를 찾을 수 없습니다. idx={}, memberAccountIdx={}", idx, mbrAcctIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "Git 프로젝트를 찾을 수 없습니다.");
        }
        return gitProject;
    }

    private String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        ValidationUtil.requireNonNull(normalized, ErrorCode.BAD_REQUEST, message);
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        return StringUtil.normalizeBlank(value);
    }

    private String normalizeProvider(String provider) {
        String normalized = requireText(provider, "provider는 필수입니다.");
        return switch (normalized) {
            case GitProviderPolicy.GITLAB -> normalized;
            default -> {
                log.error("normalizeProvider 실패 - 원인: 지원하지 않는 Git provider입니다. provider={}", provider);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Git provider는 GITLAB만 지원합니다.");
            }
        };
    }

    private String normalizeUseYn(String useYn) {
        String normalized = normalizeOptionalText(useYn);
        if (normalized == null) {
            return UseYnPolicy.YES;
        }
        return switch (normalized) {
            case UseYnPolicy.YES, UseYnPolicy.NO -> normalized;
            default -> {
                log.error("normalizeUseYn 실패 - 원인: useYn 값이 올바르지 않습니다. useYn={}", useYn);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "useYn은 Y 또는 N 이어야 합니다.");
            }
        };
    }

    private String encryptToken(String accessToken) {
        String normalized = normalizeOptionalText(accessToken);
        return normalized == null ? null : workGitAccessTokenEncryptor.encrypt(normalized);
    }

    private String resolveUpdatedToken(String currentEncryptedToken, String accessToken) {
        String normalized = normalizeOptionalText(accessToken);
        return normalized == null ? currentEncryptedToken : workGitAccessTokenEncryptor.encrypt(normalized);
    }
}
