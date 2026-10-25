package com.gw.api.query;

import com.gw.api.convert.board.BoardConvert;
import com.gw.api.convert.notice.NoticeConvert;
import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.dto.notice.CreateNoticeRequest;
import com.gw.api.dto.notice.NoticeListRequest;
import com.gw.api.dto.notice.UpdateNoticeRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.account.AdminAccountService;
import com.gw.api.service.notice.NoticeService;
import com.gw.api.service.profile.ProfileService;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.notice.NoticeMapper;
import com.gw.share.jvo.notice.NtcJvo;
import com.gw.share.vo.notice.NtcListSrchVo;
import com.gw.share.vo.notice.NtcVo;
import com.gw.share.vo.account.AcctSmryVo;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListQueryRefactorTest {
    @Test
    void noticeDetailUsesOneUpdateAndOneSelectAndMissingRowsRemainNotFound() {
        NoticeMapper mapper = mock(NoticeMapper.class);
        var service = new NoticeService(mapper);
        when(mapper.incrementViewCount("notice")).thenReturn(1);
        when(mapper.selectNoticeByUuid("notice")).thenReturn(NtcJvo.builder().uuid("notice").viewCnt(9).build());
        assertEquals(9, service.getNotice("notice").viewCount());
        var order = inOrder(mapper);
        order.verify(mapper).incrementViewCount("notice");
        order.verify(mapper).selectNoticeByUuid("notice");
        verify(mapper).selectNoticeByUuid("notice");
        assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () -> service.getNotice("missing")).getErrorCode());
        verify(mapper, never()).selectNoticeByUuid("missing");
    }

    @Test
    void listAndDashboardLimitsUseTheCommonCap() {
        var board = BoardConvert.toSearchVo(new BoardPostListRequest(null, null, null, 2, Integer.MAX_VALUE, null, null));
        var notice = NoticeConvert.toSearchVo(new NoticeListRequest(null, 2, 101, null, null));
        assertEquals(100, board.getSize()); assertEquals(100L, board.getOffset());
        assertEquals(100, notice.getSize()); assertEquals(100L, notice.getOffset());
        NoticeMapper mapper = mock(NoticeMapper.class);
        var service = new NoticeService(mapper);
        service.getDashboardNotices(Integer.MAX_VALUE);
        service.getDashboardNotices(0);
        verify(mapper).selectDashboardNoticeList(100);
        verify(mapper).selectDashboardNoticeList(5);
    }

    @Test
    void accountSummaryKeepsResponseFieldsAndCappedMetadata() {
        AccountMapper mapper = mock(AccountMapper.class);
        var service = new AdminAccountService(mapper, mock(AccountLookupService.class), mock(PasswordEncoder.class), mock(ProfileService.class));
        var createdAt = OffsetDateTime.parse("2026-10-25T00:00:00Z");
        when(mapper.selectAllAccounts(null, null, null, 100L, 100)).thenReturn(List.of(
                AcctSmryVo.builder().uuid("account").lgnId("owner").email("owner@example.test").role("USER")
                        .acctStat("ACTIVE").lckYn(true).otpRequired(true).otpEnabled(true).createdAt(createdAt).build()));
        when(mapper.countAllAccounts(null, null, null)).thenReturn(201L);
        var response = service.getAccounts(null, null, null, 2, Integer.MAX_VALUE);
        assertEquals(100, response.size()); assertEquals(3, response.totalPages());
        assertEquals("account", response.content().getFirst().uuid());
        assertEquals("owner", response.content().getFirst().loginId());
        assertTrue(response.content().getFirst().lckYn());
        assertTrue(response.content().getFirst().otpEnabled());
        assertTrue(response.content().getFirst().otpRequired());
        assertEquals(createdAt, response.content().getFirst().createdAt());
    }

    @Test
    void convertersPreserveDomainSearchDefaultsAndSortAllowLists() {
        var board = BoardConvert.toSearchVo(new BoardPostListRequest(null, "all", " 검색 ", null, -1, null, null));
        assertEquals(" 검색 ", board.getKwd());
        assertEquals("all", board.getSrchType());
        assertEquals(1, board.getPage());
        assertEquals(20, board.getSize());
        assertEquals("bp.created_at DESC", board.getOrderByClause());
        var notice = NoticeConvert.toSearchVo(new NoticeListRequest(" 검색 ", 2, 10, "viewCount", "asc"));
        assertEquals("검색", notice.getKwd());
        assertEquals(10L, notice.getOffset());
        assertEquals("view_cnt ASC", notice.getOrderByClause());
        assertNull(NoticeConvert.toSearchVo(new NoticeListRequest("  ", null, null, null, null)).getKwd());
        assertEquals("created_at DESC", NoticeConvert.toSearchVo(new NoticeListRequest(
                null, 1, 20, "title; DROP TABLE tb_ntc", "desc; --")).getOrderByClause());
    }

    @Test
    void noticeListUsesTheSameNormalizedSearchForRowsAndCount() {
        NoticeMapper mapper = mock(NoticeMapper.class);
        when(mapper.selectNoticeList(any())).thenReturn(List.of());
        when(mapper.countNoticeList(any())).thenReturn(21L);
        var response = new NoticeService(mapper).getNoticeList(new NoticeListRequest("  제목 ", 9, 20, null, null));
        var query = ArgumentCaptor.forClass(NtcListSrchVo.class);
        verify(mapper).selectNoticeList(query.capture());
        verify(mapper).countNoticeList(same(query.getValue()));
        assertEquals("제목", query.getValue().getKwd());
        assertEquals(2, response.totalPages());
        assertEquals(9, response.page());
    }

    @Test
    void noticeWritesPreserveIdentityAuditAndViewCount() {
        NoticeMapper mapper = mock(NoticeMapper.class);
        NoticeService service = new NoticeService(mapper);
        var createdAt = OffsetDateTime.parse("2026-10-20T00:00:00Z");
        var updatedAt = createdAt.plusDays(1);
        var saved = NtcJvo.builder().idx(7L).uuid("notice-uuid").ttl("원본").cntnt("본문")
                .viewCnt(12).createdBy("writer").createdAt(createdAt).updatedAt(updatedAt).build();
        doAnswer(invocation -> { invocation.<NtcVo>getArgument(0).setIdx(7L); return null; })
                .when(mapper).insertNotice(any());
        when(mapper.selectNoticeByIdx(7L)).thenReturn(saved);
        assertEquals("notice-uuid", service.createNotice("writer", new CreateNoticeRequest("등록", "본문")).noticeUuid());
        var inserted = ArgumentCaptor.forClass(NtcVo.class);
        verify(mapper).insertNotice(inserted.capture());
        assertEquals("writer", inserted.getValue().getCreatedBy());
        assertEquals("등록", inserted.getValue().getTtl());
        when(mapper.selectNoticeByUuid("notice-uuid")).thenReturn(saved);
        when(mapper.updateNotice(any())).thenReturn(1);
        service.updateNotice("editor", "notice-uuid", new UpdateNoticeRequest("수정", "새 본문"));
        var updated = ArgumentCaptor.forClass(NtcVo.class);
        verify(mapper).updateNotice(updated.capture());
        assertEquals(7L, updated.getValue().getIdx());
        assertEquals("notice-uuid", updated.getValue().getUuid());
        assertEquals("writer", updated.getValue().getCreatedBy());
        assertEquals(createdAt, updated.getValue().getCreatedAt());
        assertEquals(updatedAt, updated.getValue().getUpdatedAt());
        assertEquals("editor", updated.getValue().getUpdatedBy());
        assertEquals(12, updated.getValue().getViewCnt());
        assertEquals("수정", updated.getValue().getTtl());
        assertEquals("새 본문", updated.getValue().getCntnt());
        verify(mapper, never()).incrementViewCount(anyString());
    }

    @Test
    void adminAccountsPassLargeOffsetWithoutOverflow() {
        AccountMapper mapper = mock(AccountMapper.class);
        var service = new AdminAccountService(mapper, mock(AccountLookupService.class),
                mock(PasswordEncoder.class), mock(ProfileService.class));
        when(mapper.selectAllAccounts("user", null, "ACTIVE", 42_949_672_920L, 20)).thenReturn(List.of());
        when(mapper.countAllAccounts("user", null, "ACTIVE")).thenReturn(21L);
        var response = service.getAccounts(" user ", " ", " ACTIVE ", Integer.MAX_VALUE, 20);
        verify(mapper).selectAllAccounts("user", null, "ACTIVE", 42_949_672_920L, 20);
        assertEquals(2, response.totalPages());
        assertEquals(Integer.MAX_VALUE, response.page());
    }
}
