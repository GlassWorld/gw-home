package com.gw.api.convert.board;

import com.gw.api.dto.board.BoardPostResponse;
import com.gw.api.dto.board.BoardPostSummaryResponse;
import com.gw.api.dto.tag.TagResponse;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.board.BrdPstSmryJvo;
import com.gw.share.util.ConvertUtil;
import java.util.List;

public final class BoardConvert {

    private BoardConvert() {
    }

    // 게시글 JVO를 상세 응답으로 변환한다.
    public static BoardPostResponse toResponse(BrdPstJvo boardPost, List<TagResponse> tags) {
        return new BoardPostResponse(
                boardPost.getUuid(),
                boardPost.getCtgrNm(),
                boardPost.getTtl(),
                boardPost.getCntnt(),
                ConvertUtil.toInteger(boardPost.getViewCnt(), 0),
                boardPost.getAthrNickNm(),
                boardPost.getFavCnt() == null ? 0L : boardPost.getFavCnt(),
                boardPost.getCmtCnt() == null ? 0L : boardPost.getCmtCnt(),
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
}
