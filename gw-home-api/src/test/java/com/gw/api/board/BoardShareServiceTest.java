package com.gw.api.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gw.api.dto.board.BoardShareAccessRequest;
import com.gw.api.dto.board.BoardShareAccessStatusResponse;
import com.gw.api.dto.board.BoardShareSettingsResponse;
import com.gw.api.dto.board.PublicBoardShareResponse;
import com.gw.api.dto.board.SaveBoardShareRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.board.BoardShareService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.board.BoardShareMapper;
import com.gw.infra.db.mapper.comment.CommentMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.policy.RolePolicy;
import com.gw.share.common.util.PasswordUtil;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.board.BrdPstShrJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.board.BrdPstShrVo;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardShareServiceTest {

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private BoardShareMapper boardShareMapper;

    @Mock
    private AccountLookupService accountLookupService;

    @Mock
    private CommentMapper commentMapper;

    private BoardShareService boardShareService;

    @BeforeEach
    void setUp() {
        boardShareService = new BoardShareService(boardMapper, boardShareMapper, commentMapper, accountLookupService);
    }

    @Test
    void saveBoardShareCreatesActiveShare() {
        AcctVo account = AcctVo.builder()
                .idx(1L)
                .role(RolePolicy.ADMIN)
                .lgnId("admin")
                .build();
        BrdPstJvo boardPost = BrdPstJvo.builder()
                .idx(10L)
                .uuid("board-uuid")
                .mbrAcctIdx(1L)
                .athrNickNm("admin")
                .ttl("공유 게시글")
                .build();

        when(accountLookupService.getAccountByLoginId("admin")).thenReturn(account);
        when(boardMapper.selectBoardPostByUuid("board-uuid")).thenReturn(boardPost);

        BoardShareSettingsResponse response = boardShareService.saveBoardShare(
                "admin",
                "board-uuid",
                new SaveBoardShareRequest(true, 7, null, false, null)
        );

        assertEquals("SHARING", response.status());
        assertEquals(true, response.shareEnabled());
        assertNotNull(response.shareToken());
        verify(boardShareMapper).insertBoardShare(any(BrdPstShrVo.class));
    }

    @Test
    void accessBoardShareThrowsWhenPasswordDoesNotMatch() {
        BrdPstShrJvo boardShare = BrdPstShrJvo.builder()
                .brdPstIdx(10L)
                .shrTkn("share-token")
                .pwdHash(PasswordUtil.encodeWithBcrypt("1234"))
                .pwdUseYn("Y")
                .actvYn("Y")
                .exprAt(OffsetDateTime.now().plusDays(3))
                .ttl("공유 게시글")
                .cntnt("본문")
                .athrNickNm("작성자")
                .brdPstCreatedAt(OffsetDateTime.now().minusDays(1))
                .brdPstUpdatedAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(boardShareMapper.selectBoardShareByToken("share-token")).thenReturn(boardShare);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardShareService.accessBoardShare("share-token", new BoardShareAccessRequest("9999"))
        );

        assertEquals("비밀번호가 일치하지 않습니다.", exception.getDetailMessage());
    }

    @Test
    void accessBoardShareReturnsReadOnlyBoardWhenPasswordMatches() {
        BrdPstShrJvo boardShare = BrdPstShrJvo.builder()
                .brdPstIdx(10L)
                .shrTkn("share-token")
                .pwdHash(PasswordUtil.encodeWithBcrypt("1234"))
                .pwdUseYn("Y")
                .actvYn("Y")
                .exprAt(OffsetDateTime.now().plusDays(3))
                .ttl("공유 게시글")
                .cntnt("본문")
                .athrNickNm("작성자")
                .brdPstCreatedAt(OffsetDateTime.parse("2026-04-08T10:00:00+09:00"))
                .brdPstUpdatedAt(OffsetDateTime.parse("2026-04-08T10:00:00+09:00"))
                .build();

        when(boardShareMapper.selectBoardShareByToken("share-token")).thenReturn(boardShare);
        when(boardMapper.selectBoardAttachments(10L)).thenReturn(List.of());
        when(commentMapper.selectCommentsByBrdPstIdx(10L)).thenReturn(List.of());

        PublicBoardShareResponse response = boardShareService.accessBoardShare("share-token", new BoardShareAccessRequest("1234"));

        assertEquals("공유 게시글", response.title());
        assertEquals("본문", response.content());
        assertEquals(0, response.attachments().size());
    }

    @Test
    void getBoardShareAccessStatusReturnsExpired() {
        BrdPstShrJvo boardShare = BrdPstShrJvo.builder()
                .shrTkn("share-token")
                .pwdUseYn("N")
                .actvYn("Y")
                .exprAt(OffsetDateTime.now().minusMinutes(1))
                .ttl("만료 게시글")
                .build();

        when(boardShareMapper.selectBoardShareByToken(eq("share-token"))).thenReturn(boardShare);

        BoardShareAccessStatusResponse response = boardShareService.getBoardShareAccessStatus("share-token");

        assertEquals("EXPIRED", response.status());
        assertEquals(false, response.passwordRequired());
    }
}
