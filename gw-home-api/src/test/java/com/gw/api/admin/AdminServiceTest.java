package com.gw.api.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gw.api.dto.admin.AdminBoardPostListRequest;
import com.gw.api.dto.admin.AdminMemberListRequest;
import com.gw.api.dto.admin.AdminSummaryResponse;
import com.gw.api.service.admin.AdminService;
import com.gw.infra.db.mapper.admin.AdminMapper;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.admin.AdminBrdPstJvo;
import com.gw.share.jvo.admin.AdminMbrJvo;
import com.gw.share.jvo.admin.AdminSmryJvo;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminMapper adminMapper;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(adminMapper);
    }

    @Test
    void getMemberListReturnsPagedResponse() {
        when(adminMapper.selectMemberList(any())).thenReturn(List.of(
                AdminMbrJvo.builder()
                        .uuid("member-uuid")
                        .lgnId("tester")
                        .email("tester@example.com")
                        .role("USER")
                        .nickNm("테스터")
                        .createdAt(OffsetDateTime.parse("2026-03-17T21:00:00+09:00"))
                        .build()
        ));
        when(adminMapper.countMemberList(any())).thenReturn(1L);

        PageResponse<?> response = adminService.getMemberList(
                new AdminMemberListRequest("tester", null, false, 1, 20, null, null)
        );

        assertEquals(1, response.content().size());
        assertEquals(1L, response.totalCount());
    }

    @Test
    void getBoardPostListReturnsPagedResponse() {
        when(adminMapper.selectBoardPostList(any())).thenReturn(List.of(
                AdminBrdPstJvo.builder()
                        .uuid("board-uuid")
                        .ctgrNm("공지")
                        .ttl("관리자 조회 게시글")
                        .athrNickNm("작성자")
                        .lgnId("writer")
                        .favCnt(2L)
                        .cmtCnt(3L)
                        .createdAt(OffsetDateTime.parse("2026-03-17T21:00:00+09:00"))
                        .build()
        ));
        when(adminMapper.countBoardPostList(any())).thenReturn(1L);

        PageResponse<?> response = adminService.getBoardPostList(
                new AdminBoardPostListRequest("관리자", false, 1, 20, null, null)
        );

        assertEquals(1, response.content().size());
        assertEquals(1L, response.totalCount());
    }

    @Test
    void getSummaryReturnsMappedCounts() {
        when(adminMapper.selectSummary()).thenReturn(
                AdminSmryJvo.builder()
                        .totalMbrCnt(10L)
                        .actvMbrCnt(8L)
                        .totalBrdPstCnt(15L)
                        .totalBrdCmtCnt(20L)
                        .totalFileCnt(5L)
                        .build()
        );

        AdminSummaryResponse response = adminService.getSummary();

        assertEquals(10L, response.totalMemberCount());
        assertEquals(8L, response.activeMemberCount());
        assertEquals(15L, response.totalBoardPostCount());
        assertEquals(20L, response.totalCommentCount());
        assertEquals(5L, response.totalFileCount());
    }
}
