package com.gw.api.controller.board;

import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.dto.board.BoardPostResponse;
import com.gw.api.dto.board.BoardPostSummaryResponse;
import com.gw.api.dto.board.CreateBoardPostRequest;
import com.gw.api.dto.board.UpdateBoardPostRequest;
import com.gw.api.service.board.BoardService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public ApiResponse<PageResponse<BoardPostSummaryResponse>> getBoardPostList(BoardPostListRequest request) {
        return ApiResponse.ok(boardService.getBoardPostList(request));
    }

    @GetMapping("/{boardPostUuid}")
    public ApiResponse<BoardPostResponse> getBoardPost(@PathVariable String boardPostUuid) {
        return ApiResponse.ok(boardService.getBoardPost(boardPostUuid));
    }

    @PostMapping
    public ApiResponse<BoardPostResponse> createBoardPost(
            Principal principal,
            @Valid @RequestBody CreateBoardPostRequest request
    ) {
        return ApiResponse.ok(boardService.createBoardPost(getLoginId(principal), request));
    }

    @PutMapping("/{boardPostUuid}")
    public ApiResponse<BoardPostResponse> updateBoardPost(
            Principal principal,
            @PathVariable String boardPostUuid,
            @Valid @RequestBody UpdateBoardPostRequest request
    ) {
        return ApiResponse.ok(boardService.updateBoardPost(getLoginId(principal), boardPostUuid, request));
    }

    @DeleteMapping("/{boardPostUuid}")
    public ApiResponse<Void> deleteBoardPost(Principal principal, @PathVariable String boardPostUuid) {
        boardService.deleteBoardPost(getLoginId(principal), boardPostUuid);
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
