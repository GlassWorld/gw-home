package com.gw.api.service.board;

import com.gw.api.convert.board.BoardConvert;
import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.dto.board.BoardPostResponse;
import com.gw.api.dto.board.BoardPostSummaryResponse;
import com.gw.api.dto.board.CreateBoardPostRequest;
import com.gw.api.dto.board.UpdateBoardPostRequest;
import com.gw.api.service.tag.TagService;
import com.gw.infra.db.mapper.account.AccountMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BoardService {

    private static final Map<String, String> BRD_PST_SORT_FIELD_MAP = Map.of(
            "createdAt", "bp.created_at",
            "viewCnt", "bp.view_cnt",
            "ttl", "bp.ttl"
    );

    private final BoardMapper boardMapper;
    private final AccountMapper accountMapper;
    private final TagService tagService;

    public BoardService(BoardMapper boardMapper, AccountMapper accountMapper, TagService tagService) {
        this.boardMapper = boardMapper;
        this.accountMapper = accountMapper;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BoardPostSummaryResponse> getBoardPostList(BoardPostListRequest request) {
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

        return new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
    }

    public BoardPostResponse getBoardPost(String boardPostUuid) {
        BrdPstJvo boardPost = getBrdPstJvo(boardPostUuid);
        boardMapper.incrementViewCount(boardPostUuid);
        return BoardConvert.toResponse(getBrdPstJvo(boardPostUuid), tagService.getTagsByBrdPstUuid(boardPostUuid));
    }

    public BoardPostResponse createBoardPost(String loginId, CreateBoardPostRequest request) {
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
        return BoardConvert.toResponse(savedBoardPost, tagService.getTagsByBrdPstUuid(savedBoardPost.getUuid()));
    }

    public BoardPostResponse updateBoardPost(String loginId, String boardPostUuid, UpdateBoardPostRequest request) {
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

        return BoardConvert.toResponse(getBrdPstJvo(boardPostUuid), tagService.getTagsByBrdPstUuid(boardPostUuid));
    }

    public void deleteBoardPost(String loginId, String boardPostUuid) {
        AcctVo account = getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBrdPstJvo(boardPostUuid);
        validateOwner(account, boardPost);
        boardMapper.deleteBoardPost(boardPostUuid);
    }

    private BrdPstJvo getBrdPstJvo(String boardPostUuid) {
        BrdPstJvo boardPost = boardMapper.selectBoardPostByUuid(boardPostUuid);

        if (boardPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return boardPost;
    }

    private BrdCtgrVo getCtgrByUuid(String categoryUuid) {
        BrdCtgrVo category = boardMapper.selectBoardCategoryByUuid(categoryUuid);

        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }

    private void validateOwner(AcctVo account, BrdPstJvo boardPost) {
        if (!account.getIdx().equals(boardPost.getMbrAcctIdx())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 게시글만 수정 또는 삭제할 수 있습니다.");
        }
    }
}
