package com.gw.api.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.api.dto.profile.SaveNavigationFavoriteRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.profile.ProfileService;
import com.gw.infra.db.mapper.profile.ProfileMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.profile.PrflVo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileNavigationFavoriteTest {
    private final ProfileMapper mapper = mock(ProfileMapper.class);
    private final AccountLookupService accounts = mock(AccountLookupService.class);
    private final ProfileService service = new ProfileService(mapper, accounts, new ObjectMapper());

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void oldReportFavoritesAreHiddenWithoutMutatingStoredData(String role) {
        when(accounts.getAccountByLoginId("tester")).thenReturn(AcctVo.builder().idx(7L).role(role).build());
        when(mapper.selectNavigationFavoritesByAccountIdx(7L)).thenReturn("""
                ["/work/git-accounts", "/work/daily-reports", "/work/todos", "/weekly-reports",
                 "/work/weekly-reports", "/admin/daily-reports", "/dashboard", "/admin/accounts"]
                """);
        List<String> expected = role.equals("ADMIN")
                ? List.of("/work/git-accounts", "/work/todos", "/dashboard", "/admin/accounts")
                : List.of("/work/git-accounts", "/work/todos", "/dashboard");
        assertEquals(expected, service.getNavigationFavorites("tester").favoriteMenus());
        verify(mapper, never()).updateNavigationFavorites(anyLong(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/weekly-reports", "/work/daily-reports", "/work/weekly-reports", "/admin/daily-reports"})
    void retiredFavoritesCannotBeSavedAgain(String path) {
        when(accounts.getAccountByLoginId("tester")).thenReturn(AcctVo.builder().idx(7L).role("ADMIN").build());
        when(mapper.selectProfileByAccountIdx(7L)).thenReturn(PrflVo.builder().idx(9L).build());
        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.saveNavigationFavorites("tester", new SaveNavigationFavoriteRequest(List.of(path))));
        assertEquals(ErrorCode.BAD_REQUEST, failure.getErrorCode());
        verify(mapper, never()).updateNavigationFavorites(anyLong(), anyString(), anyString());
    }

    @Test
    void remainingFavoritesCanBeSavedInSelectedOrder() {
        when(accounts.getAccountByLoginId("tester")).thenReturn(AcctVo.builder().idx(7L).role("USER").build());
        when(mapper.selectProfileByAccountIdx(7L)).thenReturn(PrflVo.builder().idx(9L).build());
        List<String> paths = List.of("/work/git-accounts", "/work/todos", "/notices");
        assertEquals(paths, service.saveNavigationFavorites("tester", new SaveNavigationFavoriteRequest(paths)).favoriteMenus());
        verify(mapper).updateNavigationFavorites(7L, "[\"/work/git-accounts\",\"/work/todos\",\"/notices\"]", "tester");
    }
}
