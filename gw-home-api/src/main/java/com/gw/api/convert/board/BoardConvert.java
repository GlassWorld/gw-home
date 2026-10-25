package com.gw.api.convert.board;

import com.gw.api.dto.board.BoardAttachmentResponse;
import com.gw.api.dto.board.BoardCategoryResponse;
import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.dto.board.BoardPostResponse;
import com.gw.api.dto.board.BoardPostSummaryResponse;
import com.gw.api.dto.tag.TagResponse;
import com.gw.share.jvo.board.BrdPstFileJvo;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.board.BrdPstSmryJvo;
import com.gw.share.util.ConvertUtil;
import com.gw.share.common.query.PageSortSupport;
import com.gw.share.common.query.SortDirection;
import com.gw.share.vo.board.BrdPstListSrchVo;
import com.gw.share.vo.board.BrdPstFileVo;
import com.gw.share.vo.file.FileVo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.gw.share.vo.board.BrdCtgrVo;

public final class BoardConvert {

    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            "createdAt", "bp.created_at",
            "viewCnt", "bp.view_cnt",
            "ttl", "bp.ttl"
    );

    private BoardConvert() {
    }

    // 검증된 파일 목록을 요청 순서에 맞는 게시글 첨부 저장 모델로 변환한다.
    public static List<BrdPstFileVo> toAttachmentVos(Long boardPostIdx, List<FileVo> files, String loginId) {
        List<BrdPstFileVo> attachments = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            attachments.add(BrdPstFileVo.builder()
                    .brdPstIdx(boardPostIdx)
                    .fileIdx(files.get(index).getIdx())
                    .fileRole("ATTACHMENT")
                    .sortOrd(index)
                    .createdBy(loginId)
                    .build());
        }
        return attachments;
    }

    // 게시글 목록 요청을 기존 검색·정렬 규칙에 맞는 조회 모델로 변환한다.
    public static BrdPstListSrchVo toSearchVo(BoardPostListRequest request) {
        return BrdPstListSrchVo.builder()
                .ctgrUuid(request.categoryUuid())
                .srchType(request.searchType())
                .kwd(request.keyword())
                .page(PageSortSupport.normalizePage(request.page()))
                .size(PageSortSupport.normalizeSize(request.size()))
                .sortBy(request.sortBy())
                .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                .orderByClause(PageSortSupport.resolveOrderByClause(
                        request.sortBy(), request.sortDirection(), SORT_FIELD_MAP, "createdAt", SortDirection.DESC))
                .build();
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
