package com.gw.api.service.comment;

import com.gw.api.convert.comment.CommentConvert;
import com.gw.api.dto.comment.CommentResponse;
import com.gw.api.dto.comment.CreateCommentRequest;
import com.gw.api.dto.comment.UpdateCommentRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.comment.CommentMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.comment.BrdCmtJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.comment.BrdCmtVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CommentService {

    private static final String DELETED_COMMENT_MESSAGE = "삭제된 댓글입니다";

    private final CommentMapper commentMapper;
    private final BoardMapper boardMapper;
    private final AccountLookupService accountLookupService;

    public CommentService(
            CommentMapper commentMapper,
            BoardMapper boardMapper,
            AccountLookupService accountLookupService
    ) {
        this.commentMapper = commentMapper;
        this.boardMapper = boardMapper;
            this.accountLookupService = accountLookupService;
    }

    /** 게시글 댓글 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String boardPostUuid) {
        log.info("게시글 댓글 목록 조회를 시작합니다. boardPostUuid={}", boardPostUuid);
        BrdPstJvo boardPost = getBoardPostByUuid(boardPostUuid);
        List<BrdCmtJvo> comments = commentMapper.selectCommentsByBrdPstIdx(boardPost.getIdx());
        List<CommentResponse> response = CommentConvert.toTreeResponses(comments, DELETED_COMMENT_MESSAGE);
        log.info("게시글 댓글 목록 조회를 완료했습니다. boardPostUuid={}, count={}", boardPostUuid, response.size());
        return response;
    }

    /** 게시글 댓글을 등록한다. */
    public CommentResponse createComment(String loginId, String boardPostUuid, CreateCommentRequest request) {
        log.info("게시글 댓글 등록을 시작합니다. loginId={}, boardPostUuid={}", loginId, boardPostUuid);
        try {
            AcctVo account = accountLookupService.getAccountByLoginId(loginId);
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
            CommentResponse response = CommentConvert.toResponse(getCommentByIdx(comment.getIdx()), DELETED_COMMENT_MESSAGE);
            log.info("게시글 댓글 등록을 완료했습니다. loginId={}, commentUuid={}", loginId, response.boardCommentUuid());
            return response;
        } catch (BusinessException exception) {
            log.warn("게시글 댓글 등록에 실패했습니다. loginId={}, boardPostUuid={}, error={}",
                    loginId, boardPostUuid, exception.getMessage());
            throw exception;
        }
    }

    /** 게시글 댓글을 수정한다. */
    public CommentResponse updateComment(String loginId, String commentUuid, UpdateCommentRequest request) {
        log.info("게시글 댓글 수정을 시작합니다. loginId={}, commentUuid={}", loginId, commentUuid);
        try {
            AcctVo account = accountLookupService.getAccountByLoginId(loginId);
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
                log.warn("댓글 수정 대상이 없습니다. commentUuid={}", commentUuid);
                throw new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다.");
            }

            CommentResponse response = CommentConvert.toResponse(getCommentByUuid(commentUuid), DELETED_COMMENT_MESSAGE);
            log.info("게시글 댓글 수정을 완료했습니다. loginId={}, commentUuid={}", loginId, commentUuid);
            return response;
        } catch (BusinessException exception) {
            log.warn("게시글 댓글 수정에 실패했습니다. loginId={}, commentUuid={}, error={}",
                    loginId, commentUuid, exception.getMessage());
            throw exception;
        }
    }

    /** 게시글 댓글을 삭제한다. */
    public void deleteComment(String loginId, String commentUuid) {
        log.info("게시글 댓글 삭제를 시작합니다. loginId={}, commentUuid={}", loginId, commentUuid);
        try {
            AcctVo account = accountLookupService.getAccountByLoginId(loginId);
            BrdCmtJvo comment = getCommentByUuid(commentUuid);
            validateOwner(account, comment);

            int updatedCount = commentMapper.deleteComment(commentUuid);

            if (updatedCount == 0) {
                log.warn("댓글 삭제 대상이 없습니다. commentUuid={}", commentUuid);
                throw new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다.");
            }

            log.info("게시글 댓글 삭제를 완료했습니다. loginId={}, commentUuid={}", loginId, commentUuid);
        } catch (BusinessException exception) {
            log.warn("게시글 댓글 삭제에 실패했습니다. loginId={}, commentUuid={}, error={}",
                    loginId, commentUuid, exception.getMessage());
            throw exception;
        }
    }

    private BrdPstJvo getBoardPostByUuid(String boardPostUuid) {
        BrdPstJvo boardPost = boardMapper.selectBoardPostByUuid(boardPostUuid);

        if (boardPost == null) {
            log.warn("게시글 조회에 실패했습니다. boardPostUuid={}", boardPostUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return boardPost;
    }

    private BrdCmtJvo getCommentByUuid(String commentUuid) {
        BrdCmtJvo comment = commentMapper.selectCommentByUuid(commentUuid);

        if (comment == null) {
            log.warn("댓글 조회에 실패했습니다. commentUuid={}", commentUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }

        return comment;
    }

    private BrdCmtJvo getCommentByIdx(Long idx) {
        BrdCmtJvo comment = commentMapper.selectCommentByIdx(idx);

        if (comment == null) {
            log.warn("댓글 식별자 조회에 실패했습니다. commentIdx={}", idx);
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
            log.warn("부모 댓글이 다른 게시글에 속해 있습니다. boardPostUuid={}, parentCommentUuid={}",
                    boardPost.getUuid(), parentCommentUuid);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "부모 댓글이 다른 게시글에 속해 있습니다.");
        }

        return parentComment;
    }

    private void validateOwner(AcctVo account, BrdCmtJvo comment) {
        if (!account.getIdx().equals(comment.getMbrAcctIdx())) {
            log.warn("댓글 소유자 검증에 실패했습니다. loginId={}, commentUuid={}", account.getLgnId(), comment.getUuid());
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 댓글만 수정 또는 삭제할 수 있습니다.");
        }
    }
}
