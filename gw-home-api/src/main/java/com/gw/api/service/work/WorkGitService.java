package com.gw.api.service.work;

import com.gw.api.convert.work.WorkGitConvert;
import com.gw.api.dto.work.WorkGitAccountRequest;
import com.gw.api.dto.work.WorkGitAccountResponse;
import com.gw.api.dto.work.WorkGitConnectionTestResponse;
import com.gw.api.dto.work.WorkGitProjectRequest;
import com.gw.api.dto.work.WorkGitProjectResponse;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.work.WorkGitMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
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
    private final AccountMapper accountMapper;
    private final WorkGitAccessTokenEncryptor workGitAccessTokenEncryptor;
    private final WorkGitCommitClient workGitCommitClient;

    public WorkGitService(
            WorkGitMapper workGitMapper,
            AccountMapper accountMapper,
            WorkGitAccessTokenEncryptor workGitAccessTokenEncryptor,
            WorkGitCommitClient workGitCommitClient
    ) {
        this.workGitMapper = workGitMapper;
        this.accountMapper = accountMapper;
        this.workGitAccessTokenEncryptor = workGitAccessTokenEncryptor;
        this.workGitCommitClient = workGitCommitClient;
    }

    @Transactional(readOnly = true)
    public List<WorkGitAccountResponse> getGitAccounts(String loginId) {
        AcctVo account = getAccountByLoginId(loginId);
        return workGitMapper.selectGitAccounts(account.getIdx()).stream()
                .map(WorkGitConvert::toAccountResponse)
                .toList();
    }

    public WorkGitAccountResponse createGitAccount(String loginId, WorkGitAccountRequest request) {
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
        return WorkGitConvert.toAccountResponse(getGitAccountByIdx(gitAccount.getIdx(), account.getIdx()));
    }

    public WorkGitAccountResponse updateGitAccount(String loginId, String uuid, WorkGitAccountRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        WorkGitAcctVo gitAccount = getGitAccount(uuid, account.getIdx());
        gitAccount.setPrvdCd(normalizeProvider(request.provider()));
        gitAccount.setAcctLbl(requireText(request.accountLabel(), "accountLabel은 필수입니다."));
        gitAccount.setAuthNm(requireText(request.authorName(), "authorName은 필수입니다."));
        gitAccount.setAcsToknEnc(resolveUpdatedToken(gitAccount.getAcsToknEnc(), request.accessToken()));
        gitAccount.setUseYn(normalizeUseYn(request.useYn()));
        gitAccount.setUpdatedBy(loginId);
        workGitMapper.updateGitAccount(gitAccount);
        return WorkGitConvert.toAccountResponse(getGitAccount(uuid, account.getIdx()));
    }

    public void deleteGitAccount(String loginId, String uuid) {
        AcctVo account = getAccountByLoginId(loginId);
        WorkGitAcctVo gitAccount = getGitAccount(uuid, account.getIdx());
        workGitMapper.deleteWorkUnitGitProjectsByGitAccount(gitAccount.getIdx(), account.getIdx(), loginId);
        workGitMapper.deleteGitProjectsByAccount(gitAccount.getIdx(), account.getIdx(), loginId);
        workGitMapper.deleteGitAccount(uuid, account.getIdx(), loginId);
    }

    @Transactional(readOnly = true)
    public List<WorkGitProjectResponse> getGitProjects(String loginId, String gitAccountUuid) {
        AcctVo account = getAccountByLoginId(loginId);
        return workGitMapper.selectGitProjects(account.getIdx(), normalizeOptionalText(gitAccountUuid)).stream()
                .map(WorkGitConvert::toProjectResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkGitProjectResponse> getGitProjectOptions(String loginId) {
        AcctVo account = getAccountByLoginId(loginId);
        return workGitMapper.selectGitProjectOptions(account.getIdx()).stream()
                .map(WorkGitConvert::toProjectResponse)
                .toList();
    }

    public WorkGitProjectResponse createGitProject(String loginId, WorkGitProjectRequest request) {
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
        return WorkGitConvert.toProjectResponse(getGitProjectByIdx(gitProject.getIdx(), account.getIdx()));
    }

    public WorkGitProjectResponse updateGitProject(String loginId, String uuid, WorkGitProjectRequest request) {
        AcctVo account = getAccountByLoginId(loginId);
        WorkGitPrjVo gitProject = getGitProject(uuid, account.getIdx());
        WorkGitAcctVo gitAccount = getGitAccount(request.gitAccountUuid(), account.getIdx());
        gitProject.setWrkGitAcctIdx(gitAccount.getIdx());
        gitProject.setPrjNm(requireText(request.projectName(), "projectName은 필수입니다."));
        gitProject.setRepoUrl(requireText(request.repositoryUrl(), "repositoryUrl은 필수입니다."));
        gitProject.setUseYn(normalizeUseYn(request.useYn()));
        gitProject.setUpdatedBy(loginId);
        workGitMapper.updateGitProject(gitProject);
        return WorkGitConvert.toProjectResponse(getGitProject(uuid, account.getIdx()));
    }

    public void deleteGitProject(String loginId, String uuid) {
        AcctVo account = getAccountByLoginId(loginId);
        WorkGitPrjVo gitProject = getGitProject(uuid, account.getIdx());
        workGitMapper.deleteWorkUnitGitProjectsByGitProject(gitProject.getIdx(), account.getIdx(), loginId);
        workGitMapper.deleteGitProject(uuid, account.getIdx(), loginId);
    }

    @Transactional(readOnly = true)
    public WorkGitConnectionTestResponse testGitProjectConnection(String loginId, String gitProjectUuid) {
        AcctVo account = getAccountByLoginId(loginId);
        WorkGitPrjVo gitProject = getGitProject(gitProjectUuid, account.getIdx());
        return workGitCommitClient.testProjectConnection(gitProject);
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }
        return account;
    }

    private WorkGitAcctVo getGitAccount(String uuid, Long mbrAcctIdx) {
        WorkGitAcctVo gitAccount = workGitMapper.selectGitAccount(uuid, mbrAcctIdx);
        if (gitAccount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Git 계정을 찾을 수 없습니다.");
        }
        return gitAccount;
    }

    private WorkGitAcctVo getGitAccountByIdx(Long idx, Long mbrAcctIdx) {
        WorkGitAcctVo gitAccount = workGitMapper.selectGitAccountByIdx(idx, mbrAcctIdx);
        if (gitAccount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Git 계정을 찾을 수 없습니다.");
        }
        return gitAccount;
    }

    private WorkGitPrjVo getGitProject(String uuid, Long mbrAcctIdx) {
        WorkGitPrjVo gitProject = workGitMapper.selectGitProject(uuid, mbrAcctIdx);
        if (gitProject == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Git 프로젝트를 찾을 수 없습니다.");
        }
        return gitProject;
    }

    private WorkGitPrjVo getGitProjectByIdx(Long idx, Long mbrAcctIdx) {
        WorkGitPrjVo gitProject = workGitMapper.selectGitProjectByIdx(idx, mbrAcctIdx);
        if (gitProject == null) {
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
            case "GITLAB" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Git provider는 GITLAB만 지원합니다.");
        };
    }

    private String normalizeUseYn(String useYn) {
        String normalized = normalizeOptionalText(useYn);
        if (normalized == null) {
            return "Y";
        }
        return switch (normalized) {
            case "Y", "N" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "useYn은 Y 또는 N 이어야 합니다.");
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
