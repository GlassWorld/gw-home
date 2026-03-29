package com.gw.api.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gw.api.dto.vault.CredentialResponse;
import com.gw.api.dto.vault.SaveCredentialRequest;
import com.gw.api.service.vault.VaultService;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.vault.VaultCategoryMapper;
import com.gw.infra.db.mapper.vault.VaultCredentialCategoryMapper;
import com.gw.infra.db.mapper.vault.VaultMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.vault.CrdVo;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock
    private VaultMapper vaultMapper;

    @Mock
    private VaultCategoryMapper vaultCategoryMapper;

    @Mock
    private VaultCredentialCategoryMapper vaultCredentialCategoryMapper;

    @Mock
    private AccountMapper accountMapper;

    private VaultService vaultService;

    @BeforeEach
    void setUp() {
        vaultService = new VaultService(
                vaultMapper,
                vaultCategoryMapper,
                vaultCredentialCategoryMapper,
                accountMapper
        );
    }

    @Test
    void getCredentialListUsesMemberAccountIndex() {
        when(accountMapper.selectAccountByLoginId("tester_a")).thenReturn(createAccount(7L, "tester_a"));
        when(vaultCategoryMapper.selectCategoryList()).thenReturn(List.of());
        when(vaultMapper.selectCredentialList("mail", "category-uuid", 7L)).thenReturn(List.of());

        vaultService.getCredentialList("mail", "category-uuid", "tester_a");

        verify(vaultMapper).selectCredentialList("mail", "category-uuid", 7L);
    }

    @Test
    void getCredentialThrowsWhenOtherAccountTriesToAccess() {
        when(accountMapper.selectAccountByLoginId("tester_b")).thenReturn(createAccount(9L, "tester_b"));
        when(vaultMapper.selectCredential("credential-uuid", 9L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> vaultService.getCredential("credential-uuid", "tester_b")
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals(ErrorCode.NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void saveCredentialStoresMemberAccountIndex() {
        SaveCredentialRequest request = new SaveCredentialRequest(
                "메일 계정",
                List.of(),
                "tester@example.com",
                "password1234",
                "개인 메모"
        );

        when(accountMapper.selectAccountByLoginId("tester_a")).thenReturn(createAccount(7L, "tester_a"));
        doAnswer(invocation -> {
            CrdVo credential = invocation.getArgument(0);
            credential.setIdx(15L);
            credential.setUuid("credential-uuid");
            return null;
        }).when(vaultMapper).insertCredential(any(CrdVo.class));
        when(vaultMapper.selectCredentialByIdx(15L)).thenReturn(
                CrdVo.builder()
                        .idx(15L)
                        .uuid("credential-uuid")
                        .mbrAcctIdx(7L)
                        .ttl("메일 계정")
                        .lgnId("tester@example.com")
                        .pwd("password1234")
                        .memo("개인 메모")
                        .createdBy("tester_a")
                        .createdAt(OffsetDateTime.parse("2026-03-29T10:00:00+09:00"))
                        .build()
        );
        when(vaultCategoryMapper.selectCategoryList()).thenReturn(List.of());
        when(vaultCredentialCategoryMapper.selectCredentialCategoryMappings(eq(List.of(15L)))).thenReturn(List.of());

        CredentialResponse response = vaultService.saveCredential(request, "tester_a");

        ArgumentCaptor<CrdVo> credentialCaptor = ArgumentCaptor.forClass(CrdVo.class);
        verify(vaultMapper).insertCredential(credentialCaptor.capture());
        CrdVo savedCredential = credentialCaptor.getValue();

        assertEquals(7L, savedCredential.getMbrAcctIdx());
        assertEquals("tester_a", savedCredential.getCreatedBy());
        assertEquals("credential-uuid", response.credentialUuid());
        assertEquals("메일 계정", response.title());
    }

    @Test
    void updateCredentialThrowsWhenOtherAccountTriesToModify() {
        SaveCredentialRequest request = new SaveCredentialRequest(
                "수정 시도",
                List.of(),
                "tester@example.com",
                "password1234",
                "개인 메모"
        );

        when(accountMapper.selectAccountByLoginId("tester_b")).thenReturn(createAccount(9L, "tester_b"));
        when(vaultMapper.selectCredential("credential-uuid", 9L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> vaultService.updateCredential("credential-uuid", request, "tester_b")
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals(ErrorCode.NOT_FOUND.getMessage(), exception.getMessage());
        verify(vaultMapper, never()).updateCredential(any(CrdVo.class));
    }

    @Test
    void deleteCredentialThrowsWhenOtherAccountTriesToDelete() {
        when(accountMapper.selectAccountByLoginId("tester_b")).thenReturn(createAccount(9L, "tester_b"));
        when(vaultMapper.selectCredential("credential-uuid", 9L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> vaultService.deleteCredential("credential-uuid", "tester_b")
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals(ErrorCode.NOT_FOUND.getMessage(), exception.getMessage());
        verify(vaultMapper, never()).deleteCredential(any(), any(), any());
    }

    private AcctVo createAccount(Long idx, String loginId) {
        return AcctVo.builder()
                .idx(idx)
                .uuid(loginId + "-uuid")
                .lgnId(loginId)
                .email(loginId + "@example.com")
                .role("USER")
                .build();
    }
}
