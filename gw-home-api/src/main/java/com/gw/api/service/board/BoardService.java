package com.gw.api.service.board;

import com.gw.api.convert.board.BoardConvert;
import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.dto.board.BoardPostResponse;
import com.gw.api.dto.board.BoardPostSummaryResponse;
import com.gw.api.dto.board.CreateBoardPostRequest;
import com.gw.api.dto.board.UpdateBoardPostRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.tag.TagService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.query.SortDirection;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.board.BrdCtgrVo;
import com.gw.share.vo.board.BrdPstListSrchVo;
import com.gw.share.vo.board.BrdPstVo;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class BoardService {

    private static final Map<String, String> BRD_PST_SORT_FIELD_MAP = Map.of(
            "createdAt", "bp.created_at",
            "viewCnt", "bp.view_cnt",
            "ttl", "bp.ttl"
    );

    private final BoardMapper boardMapper;
    private final AccountLookupService accountLookupService;
    private final TagService tagService;

    public BoardService(BoardMapper boardMapper, AccountLookupService accountLookupService, TagService tagService) {
        this.boardMapper = boardMapper;
        this.accountLookupService = accountLookupService;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    // 게시글 목록을 조회한다.
    public PageResponse<BoardPostSummaryResponse> getBoardPostList(BoardPostListRequest request) {
        log.info("getBoardPostList 시작 - request: {}", request);
        BrdPstListSrchVo query = BrdPstListSrchVo.builder()
                .ctgrUuid(request.categoryUuid())
                .kwd(request.keyword())
                .page(PageSortSupport.normalizePage(request.page()))
                .size(PageSortSupport.normalizeSize(request.size()))
                .sortBy(request.sortBy())
                .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                .orderByClause(
                        PageSortSupport.resolveOrderByClause(
                                request.sortBy(),
                                request.sortDirection(),
                                BRD_PST_SORT_FIELD_MAP,
                                "createdAt",
                                SortDirection.DESC
                        )
                )
                .build();

        List<BoardPostSummaryResponse> content = boardMapper.selectBoardPostList(query)
                .stream()
                .map(BoardConvert::toSummaryResponse)
                .toList();
        long totalCount = boardMapper.countBoardPostList(query);
        int totalPages = (int) Math.ceil((double) totalCount / query.getSize());
        PageResponse<BoardPostSummaryResponse> response = new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
        log.info("getBoardPostList 완료 - totalCount: {}", totalCount);
        return response;
    }

    // 게시글 상세를 조회한다.
    public BoardPostResponse getBoardPost(String boardPostUuid) {
        log.info("getBoardPost 시작 - boardPostUuid: {}", boardPostUuid);
        BrdPstJvo boardPost = getBrdPstJvo(boardPostUuid);
        boardMapper.incrementViewCount(boardPostUuid);
        BoardPostResponse response = BoardConvert.toResponse(getBrdPstJvo(boardPostUuid), tagService.getTagsByBrdPstUuid(boardPostUuid));
        log.info("getBoardPost 완료 - boardPostUuid: {}", boardPostUuid);
        return response;
    }

    // 로그인 사용자의 게시글을 생성한다.
    public BoardPostResponse createBoardPost(String loginId, CreateBoardPostRequest request) {
        log.info("createBoardPost 시작 - loginId: {}, categoryUuid: {}", loginId, request.categoryUuid());
        AcctVo account = getAccountByLoginId(loginId);
        BrdCtgrVo category = getCtgrByUuid(request.categoryUuid());

        BrdPstVo boardPost = BrdPstVo.builder()
                .brdCtgrIdx(category.getIdx())
                .mbrAcctIdx(account.getIdx())
                .ttl(request.title())
                .cntnt(request.content())
                .createdBy(loginId)
                .build();

        boardMapper.insertBoardPost(boardPost);
        BrdPstJvo savedBoardPost = boardMapper.selectBoardPostByIdx(boardPost.getIdx());
        BoardPostResponse response = BoardConvert.toResponse(savedBoardPost, tagService.getTagsByBrdPstUuid(savedBoardPost.getUuid()));
        log.info("createBoardPost 완료 - loginId: {}, boardPostUuid: {}", loginId, response.boardPostUuid());
        return response;
    }

    // 로그인 사용자의 게시글을 수정한다.
    public BoardPostResponse updateBoardPost(String loginId, String boardPostUuid, UpdateBoardPostRequest request) {
        log.info("updateBoardPost 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        AcctVo account = getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBrdPstJvo(boardPostUuid);
        validateOwner(account, boardPost);

        BrdCtgrVo category = getCtgrByUuid(request.categoryUuid());
        BrdPstVo boardPostVo = BrdPstVo.builder()
                .uuid(boardPost.getUuid())
                .brdCtgrIdx(category.getIdx())
                .mbrAcctIdx(boardPost.getMbrAcctIdx())
                .ttl(request.title())
                .cntnt(request.content())
                .viewCnt(boardPost.getViewCnt())
                .createdBy(boardPost.getCreatedBy())
                .updatedBy(loginId)
                .createdAt(boardPost.getCreatedAt())
                .updatedAt(boardPost.getUpdatedAt())
                .build();
        boardMapper.updateBoardPost(boardPostVo);

        BoardPostResponse response = BoardConvert.toResponse(getBrdPstJvo(boardPostUuid), tagService.getTagsByBrdPstUuid(boardPostUuid));
        log.info("updateBoardPost 완료 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        return response;
    }

    // 로그인 사용자의 게시글을 삭제한다.
    public void deleteBoardPost(String loginId, String boardPostUuid) {
        log.info("deleteBoardPost 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        AcctVo account = getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBrdPstJvo(boardPostUuid);
        validateOwner(account, boardPost);
        boardMapper.deleteBoardPost(boardPostUuid);
        log.info("deleteBoardPost 완료 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
    }

    private BrdPstJvo getBrdPstJvo(String boardPostUuid) {
        BrdPstJvo boardPost = boardMapper.selectBoardPostByUuid(boardPostUuid);

        if (boardPost == null) {
            log.error("getBrdPstJvo 실패 - 원인: 게시글을 찾을 수 없습니다. boardPostUuid={}", boardPostUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return boardPost;
    }

    private BrdCtgrVo getCtgrByUuid(String categoryUuid) {
        BrdCtgrVo category = boardMapper.selectBoardCategoryByUuid(categoryUuid);

        if (category == null) {
            log.error("getCtgrByUuid 실패 - 원인: 카테고리를 찾을 수 없습니다. categoryUuid={}", categoryUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private void validateOwner(AcctVo account, BrdPstJvo boardPost) {
        if (!account.getIdx().equals(boardPost.getMbrAcctIdx())) {
            log.error(
                    "validateOwner 실패 - 원인: 본인 게시글만 수정 또는 삭제할 수 있습니다. memberAccountIdx={}, boardOwnerIdx={}, boardPostUuid={}",
                    account.getIdx(),
                    boardPost.getMbrAcctIdx(),
                    boardPost.getUuid()
            );
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 게시글만 수정 또는 삭제할 수 있습니다.");
        }
    }
}
