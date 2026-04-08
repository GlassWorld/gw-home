package com.gw.api.controller.board;

import com.gw.api.dto.board.BoardShareAccessRequest;
import com.gw.api.dto.board.BoardShareAccessStatusResponse;
import com.gw.api.dto.board.BoardShareSettingsResponse;
import com.gw.api.dto.board.PublicBoardShareResponse;
import com.gw.api.dto.board.SaveBoardShareRequest;
import com.gw.api.service.board.BoardShareService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
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
public class BoardShareController {

    private final BoardShareService boardShareService;

    public BoardShareController(BoardShareService boardShareService) {
        this.boardShareService = boardShareService;
    }

    // 로그인 사용자가 게시글 외부 공유 설정을 조회한다.
    @GetMapping("/api/v1/boards/{boardPostUuid}/share")
    public ApiResponse<BoardShareSettingsResponse> getBoardShare(Principal principal, @PathVariable String boardPostUuid) {
        return ApiResponse.ok(boardShareService.getBoardShare(getLoginId(principal), boardPostUuid));
    }

    // 로그인 사용자가 게시글 외부 공유 설정을 저장한다.
    @PutMapping("/api/v1/boards/{boardPostUuid}/share")
    public ApiResponse<BoardShareSettingsResponse> saveBoardShare(
            Principal principal,
            @PathVariable String boardPostUuid,
            @RequestBody SaveBoardShareRequest request
    ) {
        return ApiResponse.ok(boardShareService.saveBoardShare(getLoginId(principal), boardPostUuid, request));
    }

    // 로그인 사용자가 게시글 외부 공유 링크를 재발급한다.
    @PostMapping("/api/v1/boards/{boardPostUuid}/share/reissue")
    public ApiResponse<BoardShareSettingsResponse> reissueBoardShare(Principal principal, @PathVariable String boardPostUuid) {
        return ApiResponse.ok(boardShareService.reissueBoardShare(getLoginId(principal), boardPostUuid));
    }

    // 로그인 사용자가 게시글 외부 공유를 해제한다.
    @DeleteMapping("/api/v1/boards/{boardPostUuid}/share")
    public ApiResponse<BoardShareSettingsResponse> deactivateBoardShare(Principal principal, @PathVariable String boardPostUuid) {
        return ApiResponse.ok(boardShareService.deactivateBoardShare(getLoginId(principal), boardPostUuid));
    }

    // 외부 사용자가 공유 링크 접근 상태를 조회한다.
    @GetMapping("/api/v1/board-shares/{shareToken}/status")
    public ApiResponse<BoardShareAccessStatusResponse> getBoardShareStatus(@PathVariable String shareToken) {
        return ApiResponse.ok(boardShareService.getBoardShareAccessStatus(shareToken));
    }

    // 외부 사용자가 공유 링크로 게시글을 읽기 전용 조회한다.
    @PostMapping("/api/v1/board-shares/{shareToken}/access")
    public ApiResponse<PublicBoardShareResponse> accessBoardShare(
            @PathVariable String shareToken,
            @RequestBody(required = false) BoardShareAccessRequest request
    ) {
        return ApiResponse.ok(boardShareService.accessBoardShare(shareToken, request));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
