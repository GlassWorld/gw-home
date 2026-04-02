package com.gw.api.service.comment;

import com.gw.api.convert.comment.CommentConvert;
import com.gw.api.dto.comment.CommentResponse;
import com.gw.api.dto.comment.CreateCommentRequest;
import com.gw.api.dto.comment.UpdateCommentRequest;
import com.gw.api.service.account.AccountService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.comment.CommentMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.comment.BrdCmtJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.comment.BrdCmtVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private static final String DELETED_COMMENT_MESSAGE = "삭제된 댓글입니다";

    private final CommentMapper commentMapper;
    private final BoardMapper boardMapper;
    private final AccountService accountService;

    public CommentService(
            CommentMapper commentMapper,
            BoardMapper boardMapper,
            AccountService accountService
    ) {
        this.commentMapper = commentMapper;
        this.boardMapper = boardMapper;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String boardPostUuid) {
        BrdPstJvo boardPost = getBoardPostByUuid(boardPostUuid);
        List<BrdCmtJvo> comments = commentMapper.selectCommentsByBrdPstIdx(boardPost.getIdx());
        return CommentConvert.toTreeResponses(comments, DELETED_COMMENT_MESSAGE);
    }

    public CommentResponse createComment(String loginId, String boardPostUuid, CreateCommentRequest request) {
        AcctVo account = accountService.getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBoardPostByUuid(boardPostUuid);
        BrdCmtJvo parentComment = getParentComment(boardPost, request.parentCommentUuid());

        BrdCmtVo comment = BrdCmtVo.builder()
                .brdPstIdx(boardPost.getIdx())
                .prntBrdCmtIdx(parentComment == null ? null : parentComment.getIdx())
                .mbrAcctIdx(account.getIdx())
                .cntnt(request.content())
                .createdBy(loginId)
                .build();

        commentMapper.insertComment(comment);
        return CommentConvert.toResponse(getCommentByIdx(comment.getIdx()), DELETED_COMMENT_MESSAGE);
    }

    public CommentResponse updateComment(String loginId, String commentUuid, UpdateCommentRequest request) {
        AcctVo account = accountService.getAccountByLoginId(loginId);
        BrdCmtJvo comment = getCommentByUuid(commentUuid);
        validateOwner(account, comment);

        BrdCmtVo commentVo = BrdCmtVo.builder()
                .uuid(comment.getUuid())
                .brdPstIdx(comment.getBrdPstIdx())
                .prntBrdCmtIdx(comment.getPrntBrdCmtIdx())
                .mbrAcctIdx(comment.getMbrAcctIdx())
                .cntnt(request.content())
                .createdBy(comment.getCreatedBy())
                .updatedBy(loginId)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();

        int updatedCount = commentMapper.updateComment(commentVo);

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }

        return CommentConvert.toResponse(getCommentByUuid(commentUuid), DELETED_COMMENT_MESSAGE);
    }

    public void deleteComment(String loginId, String commentUuid) {
        AcctVo account = accountService.getAccountByLoginId(loginId);
        BrdCmtJvo comment = getCommentByUuid(commentUuid);
        validateOwner(account, comment);

        int updatedCount = commentMapper.deleteComment(commentUuid);

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
    }

    private BrdPstJvo getBoardPostByUuid(String boardPostUuid) {
        BrdPstJvo boardPost = boardMapper.selectBoardPostByUuid(boardPostUuid);

        if (boardPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return boardPost;
    }

    private BrdCmtJvo getCommentByUuid(String commentUuid) {
        BrdCmtJvo comment = commentMapper.selectCommentByUuid(commentUuid);

        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }

        return comment;
    }

    private BrdCmtJvo getCommentByIdx(Long idx) {
        BrdCmtJvo comment = commentMapper.selectCommentByIdx(idx);

        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }

        return comment;
    }

    private BrdCmtJvo getParentComment(BrdPstJvo boardPost, String parentCommentUuid) {
        if (parentCommentUuid == null || parentCommentUuid.isBlank()) {
            return null;
        }

        BrdCmtJvo parentComment = getCommentByUuid(parentCommentUuid);

        if (!boardPost.getIdx().equals(parentComment.getBrdPstIdx())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "부모 댓글이 다른 게시글에 속해 있습니다.");
        }

        return parentComment;
    }

    private void validateOwner(AcctVo account, BrdCmtJvo comment) {
        if (!account.getIdx().equals(comment.getMbrAcctIdx())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 댓글만 수정 또는 삭제할 수 있습니다.");
        }
    }
}
