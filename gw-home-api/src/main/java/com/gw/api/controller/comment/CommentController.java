package com.gw.api.controller.comment;

import com.gw.api.dto.comment.CommentResponse;
import com.gw.api.dto.comment.CreateCommentRequest;
import com.gw.api.dto.comment.UpdateCommentRequest;
import com.gw.api.service.comment.CommentService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/boards/{boardPostUuid}/comments")
    public ApiResponse<List<CommentResponse>> getComments(@PathVariable String boardPostUuid) {
        return ApiResponse.ok(commentService.getComments(boardPostUuid));
    }

    @PostMapping("/boards/{boardPostUuid}/comments")
    public ApiResponse<CommentResponse> createComment(
            Principal principal,
            @PathVariable String boardPostUuid,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ApiResponse.ok(commentService.createComment(getLoginId(principal), boardPostUuid, request));
    }

    @PutMapping("/comments/{commentUuid}")
    public ApiResponse<CommentResponse> updateComment(
            Principal principal,
            @PathVariable String commentUuid,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return ApiResponse.ok(commentService.updateComment(getLoginId(principal), commentUuid, request));
    }

    @DeleteMapping("/comments/{commentUuid}")
    public ApiResponse<Void> deleteComment(Principal principal, @PathVariable String commentUuid) {
        commentService.deleteComment(getLoginId(principal), commentUuid);
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
