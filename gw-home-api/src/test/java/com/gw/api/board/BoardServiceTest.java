package com.gw.api.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.board.BoardService;
import com.gw.api.service.tag.TagService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.board.BrdPstSmryJvo;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private AccountLookupService accountLookupService;

    @Mock
    private TagService tagService;

    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(boardMapper, accountLookupService, tagService);
    }

    @Test
    void getBoardPostListReturnsPagedResponse() {
        BrdPstSmryJvo summaryVo = BrdPstSmryJvo.builder()
                .uuid("board-uuid")
                .ctgrNm("공지")
                .ttl("테스트 게시글")
                .viewCnt(3)
                .athrNickNm("tester")
                .favCnt(0L)
                .cmtCnt(0L)
                .createdAt(OffsetDateTime.parse("2026-03-17T21:00:00+09:00"))
                .build();

        when(boardMapper.selectBoardPostList(any())).thenReturn(List.of(summaryVo));
        when(boardMapper.countBoardPostList(any())).thenReturn(1L);

        PageResponse<?> response = boardService.getBoardPostList(
                new BoardPostListRequest(null, "테스트", 1, 20, null, null)
        );

        assertEquals(1, response.content().size());
        assertEquals(1L, response.totalCount());
        assertEquals(1, response.totalPages());
    }
}
