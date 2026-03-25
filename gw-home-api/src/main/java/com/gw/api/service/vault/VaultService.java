package com.gw.api.service.vault;

import com.gw.api.dto.vault.CredentialResponse;
import com.gw.api.dto.vault.SaveCredentialRequest;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.vault.VaultCategoryMapper;
import com.gw.infra.db.mapper.vault.VaultMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.vault.CatVo;
import com.gw.share.vo.vault.CrdVo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VaultService {

    private final VaultMapper vaultMapper;
    private final VaultCategoryMapper vaultCategoryMapper;
    private final AccountMapper accountMapper;

    public VaultService(VaultMapper vaultMapper, VaultCategoryMapper vaultCategoryMapper, AccountMapper accountMapper) {
        this.vaultMapper = vaultMapper;
        this.vaultCategoryMapper = vaultCategoryMapper;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public List<CredentialResponse> getCredentialList(String keyword, String categoryUuid, String loginId) {
        getAccountByLoginId(loginId);
        Map<Long, CatVo> categoryByIdx = getCategoryByIdxMap();

        return vaultMapper.selectCredentialList(keyword, categoryUuid, loginId)
                .stream()
                .map(credential -> toResponse(credential, categoryByIdx))
                .toList();
    }

    @Transactional(readOnly = true)
    public CredentialResponse getCredential(String uuid, String loginId) {
        getAccountByLoginId(loginId);
        return toResponse(getCredentialVo(uuid, loginId), getCategoryByIdxMap());
    }

    public CredentialResponse saveCredential(SaveCredentialRequest request, String loginId) {
        getAccountByLoginId(loginId);

        CrdVo credential = CrdVo.builder()
                .ttl(request.title())
                .vltCatIdx(getCategoryIndex(request.categoryUuid()))
                .lgnId(request.loginId())
                .pwd(request.password())
                .memo(request.memo())
                .createdBy(loginId)
                .build();
        vaultMapper.insertCredential(credential);

        return toResponse(vaultMapper.selectCredentialByIdx(credential.getIdx()), getCategoryByIdxMap());
    }

    public CredentialResponse updateCredential(String uuid, SaveCredentialRequest request, String loginId) {
        getAccountByLoginId(loginId);
        CrdVo credential = getCredentialVo(uuid, loginId);

        credential.setTtl(request.title());
        credential.setVltCatIdx(getCategoryIndex(request.categoryUuid()));
        credential.setLgnId(request.loginId());
        credential.setPwd(request.password());
        credential.setMemo(request.memo());
        credential.setUpdatedBy(loginId);

        vaultMapper.updateCredential(credential);
        return toResponse(getCredentialVo(uuid, loginId), getCategoryByIdxMap());
    }

    public void deleteCredential(String uuid, String loginId) {
        getAccountByLoginId(loginId);
        getCredentialVo(uuid, loginId);
        vaultMapper.deleteCredential(uuid, loginId, loginId);
    }

    private CrdVo getCredentialVo(String uuid, String loginId) {
        CrdVo credential = vaultMapper.selectCredential(uuid, loginId);

        if (credential == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "자격증명 정보를 찾을 수 없습니다.");
        }

        return credential;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    private Long getCategoryIndex(String categoryUuid) {
        if (categoryUuid == null || categoryUuid.isBlank()) {
            return null;
        }

        CatVo category = vaultCategoryMapper.selectCategory(categoryUuid);

        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category.getIdx();
    }

    private Map<Long, CatVo> getCategoryByIdxMap() {
        Map<Long, CatVo> categoryByIdx = new HashMap<>();

        for (CatVo category : vaultCategoryMapper.selectCategoryList()) {
            categoryByIdx.put(category.getIdx(), category);
        }

        return categoryByIdx;
    }

    private CredentialResponse toResponse(CrdVo credential, Map<Long, CatVo> categoryByIdx) {
        CatVo category = credential.getVltCatIdx() == null ? null : categoryByIdx.get(credential.getVltCatIdx());

        return new CredentialResponse(
                credential.getUuid(),
                credential.getTtl(),
                category == null ? null : category.getUuid(),
                category == null ? null : category.getNm(),
                credential.getLgnId(),
                credential.getPwd(),
                credential.getMemo(),
                credential.getCreatedAt()
        );
    }
}
