package com.gw.api.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.comment.CommentService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.comment.CommentMapper;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.comment.BrdCmtJvo;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private AccountLookupService accountLookupService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentMapper, boardMapper, accountLookupService);
    }

    @Test
    void getCommentsReturnsTreeStructure() {
        when(boardMapper.selectBoardPostByUuid("board-uuid")).thenReturn(
                BrdPstJvo.builder()
                        .idx(1L)
                        .uuid("board-uuid")
                        .build()
        );
        when(commentMapper.selectCommentsByBrdPstIdx(1L)).thenReturn(List.of(
                BrdCmtJvo.builder()
                        .idx(10L)
                        .uuid("parent-uuid")
                        .brdPstIdx(1L)
                        .cntnt("parent")
                        .athrNickNm("writer")
                        .createdAt(OffsetDateTime.parse("2026-03-17T21:00:00+09:00"))
                        .updatedAt(OffsetDateTime.parse("2026-03-17T21:00:00+09:00"))
                        .build(),
                BrdCmtJvo.builder()
                        .idx(11L)
                        .uuid("child-uuid")
                        .brdPstIdx(1L)
                        .prntBrdCmtIdx(10L)
                        .prntBrdCmtUuid("parent-uuid")
                        .cntnt("child")
                        .athrNickNm("writer")
                        .createdAt(OffsetDateTime.parse("2026-03-17T21:01:00+09:00"))
                        .updatedAt(OffsetDateTime.parse("2026-03-17T21:01:00+09:00"))
                        .build()
        ));

        List<?> responses = commentService.getComments("board-uuid");

        assertEquals(1, responses.size());
        assertEquals(1, ((com.gw.api.dto.comment.CommentResponse) responses.get(0)).replies().size());
    }
}
