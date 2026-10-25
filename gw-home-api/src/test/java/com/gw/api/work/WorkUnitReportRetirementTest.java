package com.gw.api.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gw.api.dto.work.WorkUnitListRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.work.WorkGitCommitClient;
import com.gw.api.service.work.WorkUnitService;
import com.gw.infra.db.mapper.work.WorkGitMapper;
import com.gw.infra.db.mapper.work.WorkUnitMapper;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.work.WorkUnitListSearchVo;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class WorkUnitReportRetirementTest {
    @ParameterizedTest
    @ValueSource(strings = {"updated", "recent", "frequent"})
    void currentAndLegacySortsUseModificationTimeWithinOwner(String sort) {
        WorkUnitMapper mapper = mock(WorkUnitMapper.class);
        AccountLookupService accounts = mock(AccountLookupService.class);
        WorkUnitService service = new WorkUnitService(mapper, mock(WorkGitMapper.class), accounts, mock(WorkGitCommitClient.class), mock(com.gw.api.service.work.WorkNotificationReservations.class));
        when(accounts.getAccountByLoginId("tester")).thenReturn(AcctVo.builder().idx(7L).build());
        when(mapper.selectWorkUnitList(any())).thenReturn(List.of());

        service.getWorkUnitList("tester", new WorkUnitListRequest(null, null, null, "Y", sort));

        ArgumentCaptor<WorkUnitListSearchVo> query = ArgumentCaptor.forClass(WorkUnitListSearchVo.class);
        verify(mapper).selectWorkUnitList(query.capture());
        assertEquals(7L, query.getValue().getMbrAcctIdx());
        assertEquals("wu.use_yn DESC, wu.updated_at DESC, wu.created_at DESC", query.getValue().getOrderByClause());
    }
}
