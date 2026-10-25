package com.gw.api.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.dto.board.UpdateBoardPostRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.board.BoardService;
import com.gw.api.service.tag.TagService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.file.FileMapper;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.board.BrdPstSmryJvo;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.board.BrdPstFileVo;
import com.gw.share.vo.file.FileVo;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
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
    private FileMapper fileMapper;

    @Mock
    private TagService tagService;

    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(boardMapper, fileMapper, accountLookupService, tagService);
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
                new BoardPostListRequest(null, null, "테스트", 1, 20, null, null)
        );

        assertEquals(1, response.content().size());
        assertEquals(1L, response.totalCount());
        assertEquals(1, response.totalPages());
    }

    @Test
    void detailIncrementsOnceAndSelectsOnce() {
        when(boardMapper.incrementViewCount("post")).thenReturn(1);
        when(boardMapper.selectBoardPostByUuid("post")).thenReturn(
                BrdPstJvo.builder().idx(1L).uuid("post").viewCnt(8).build());
        var response = boardService.getBoardPost("post");
        assertEquals(8, response.viewCount());
        var order = inOrder(boardMapper);
        order.verify(boardMapper).incrementViewCount("post");
        order.verify(boardMapper).selectBoardPostByUuid("post");
        verify(boardMapper).selectBoardPostByUuid("post");
    }

    @Test
    void missingOrDeletedDetailStopsAfterUpdate() {
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> boardService.getBoardPost("missing")).getErrorCode());
        verify(boardMapper, never()).selectBoardPostByUuid(anyString());
        verifyNoInteractions(fileMapper, tagService);
    }

    @Test
    void tenAttachmentsUseOneLookupAndOneInsertPreservingOrder() {
        stubEditablePost();
        List<String> uuids = IntStream.range(1, 11)
                .mapToObj(index -> "aaaaaaaa-0000-0000-0000-%012d".formatted(index)).toList();
        List<FileVo> files = new ArrayList<>(IntStream.range(1, 11)
                .mapToObj(index -> FileVo.builder().idx((long) index).uuid(uuids.get(index - 1)).createdBy("owner").build()).toList());
        Collections.reverse(files);
        when(fileMapper.selectFilesByUuids(uuids)).thenReturn(files);
        var requestUuids = new ArrayList<>(uuids);
        requestUuids.set(0, uuids.getFirst().toUpperCase(java.util.Locale.ROOT));
        requestUuids.add(uuids.getFirst()); requestUuids.add(null); requestUuids.add("  ");
        boardService.updateBoardPost("owner", "post", new UpdateBoardPostRequest(null, "title", "body", requestUuids));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BrdPstFileVo>> attachments = ArgumentCaptor.forClass(List.class);
        var order = inOrder(fileMapper, boardMapper);
        order.verify(fileMapper).selectFilesByUuids(uuids);
        order.verify(boardMapper).deleteBoardAttachments(1L);
        order.verify(boardMapper).insertBoardAttachments(attachments.capture());
        assertEquals(10, attachments.getValue().size());
        for (int index = 0; index < 10; index++) {
            assertEquals(index + 1L, attachments.getValue().get(index).getFileIdx());
            assertEquals(index, attachments.getValue().get(index).getSortOrd());
            assertEquals("owner", attachments.getValue().get(index).getCreatedBy());
        }
        verify(fileMapper).selectFilesByUuids(uuids);
        verify(fileMapper, never()).selectFileByUuid(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "deleted", "foreign"})
    void invalidAttachmentsDoNotReplaceExistingLinks(String reason) {
        stubEditablePost();
        String uuid = "aaaaaaaa-0000-0000-0000-000000000001";
        var file = FileVo.builder().idx(2L).uuid(uuid).createdBy(reason.equals("foreign") ? "other" : "owner")
                .delAt(reason.equals("deleted") ? OffsetDateTime.now() : null).build();
        when(fileMapper.selectFilesByUuids(List.of(uuid))).thenReturn(reason.equals("missing") ? List.of() : List.of(file));
        var failure = assertThrows(BusinessException.class, () -> boardService.updateBoardPost("owner", "post",
                new UpdateBoardPostRequest(null, "title", "body", List.of(uuid))));
        assertEquals(reason.equals("foreign") ? ErrorCode.FORBIDDEN : ErrorCode.NOT_FOUND, failure.getErrorCode());
        verify(boardMapper, never()).deleteBoardAttachments(anyLong());
        verify(boardMapper, never()).insertBoardAttachments(anyList());
    }

    @Test
    void emptyAttachmentsClearLinksWithoutFileLookup() {
        stubEditablePost();
        boardService.updateBoardPost("owner", "post", new UpdateBoardPostRequest(null, "title", "body", List.of()));
        verify(boardMapper).deleteBoardAttachments(1L);
        verify(boardMapper, never()).insertBoardAttachments(anyList());
        verifyNoInteractions(fileMapper);
    }

    @Test
    void malformedAttachmentUuidDoesNotReachFileQuery() {
        stubEditablePost();
        var failure = assertThrows(BusinessException.class, () -> boardService.updateBoardPost("owner", "post",
                new UpdateBoardPostRequest(null, "title", "body", List.of("invalid"))));
        assertEquals(ErrorCode.BAD_REQUEST, failure.getErrorCode());
        verifyNoInteractions(fileMapper);
        verify(boardMapper, never()).deleteBoardAttachments(anyLong());
    }

    private void stubEditablePost() {
        when(accountLookupService.getAccountByLoginId("owner")).thenReturn(AcctVo.builder().idx(7L).lgnId("owner").role("USER").build());
        when(boardMapper.selectBoardPostByUuid("post")).thenReturn(BrdPstJvo.builder().idx(1L).uuid("post").mbrAcctIdx(7L).build());
    }
}
