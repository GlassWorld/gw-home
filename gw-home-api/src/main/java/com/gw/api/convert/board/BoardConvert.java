package com.gw.api.convert.board;

import com.gw.api.dto.board.BoardAttachmentResponse;
import com.gw.api.dto.board.BoardCategoryResponse;
import com.gw.api.dto.board.BoardPostResponse;
import com.gw.api.dto.board.BoardPostSummaryResponse;
import com.gw.api.dto.tag.TagResponse;
import com.gw.share.jvo.board.BrdPstFileJvo;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.board.BrdPstSmryJvo;
import com.gw.share.util.ConvertUtil;
import java.util.List;
import com.gw.share.vo.board.BrdCtgrVo;

public final class BoardConvert {

    private BoardConvert() {
    }

    // 게시글 JVO를 상세 응답으로 변환한다.
    public static BoardPostResponse toResponse(
            BrdPstJvo boardPost,
            List<BoardAttachmentResponse> attachments,
            List<TagResponse> tags
    ) {
        return new BoardPostResponse(
                boardPost.getUuid(),
                boardPost.getCtgrNm(),
                boardPost.getTtl(),
                boardPost.getCntnt(),
                ConvertUtil.toInteger(boardPost.getViewCnt(), 0),
                boardPost.getAthrNickNm(),
                boardPost.getFavCnt() == null ? 0L : boardPost.getFavCnt(),
                boardPost.getCmtCnt() == null ? 0L : boardPost.getCmtCnt(),
                attachments,
                tags,
                boardPost.getCreatedAt(),
                boardPost.getUpdatedAt()
        );
    }

    // 게시글 요약 JVO를 목록 응답으로 변환한다.
    public static BoardPostSummaryResponse toSummaryResponse(BrdPstSmryJvo boardPost) {
        return new BoardPostSummaryResponse(
                boardPost.getUuid(),
                boardPost.getCtgrNm(),
                boardPost.getTtl(),
                ConvertUtil.toInteger(boardPost.getViewCnt(), 0),
                boardPost.getAthrNickNm(),
                boardPost.getFavCnt() == null ? 0L : boardPost.getFavCnt(),
                boardPost.getCmtCnt() == null ? 0L : boardPost.getCmtCnt(),
                boardPost.getCreatedAt()
        );
    }

    // 게시글 첨부 JVO를 첨부 응답으로 변환한다.
    public static BoardAttachmentResponse toAttachmentResponse(BrdPstFileJvo boardAttachment) {
        return new BoardAttachmentResponse(
                boardAttachment.getFileUuid(),
                boardAttachment.getOrgnlNm(),
                boardAttachment.getFileUrl(),
                boardAttachment.getMimeType(),
                boardAttachment.getFileSize() == null ? 0L : boardAttachment.getFileSize(),
                boardAttachment.getUpldrType(),
                boardAttachment.getCreatedAt()
        );
    }

    // 게시판 카테고리 VO를 카테고리 응답으로 변환한다.
    public static BoardCategoryResponse toCategoryResponse(BrdCtgrVo category) {
        return new BoardCategoryResponse(
                category.getUuid(),
                category.getNm(),
                category.getSortOrd() == null ? 0 : category.getSortOrd()
        );
    }
}
