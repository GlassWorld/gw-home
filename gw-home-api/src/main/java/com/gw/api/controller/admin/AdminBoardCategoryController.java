package com.gw.api.controller.admin;

import com.gw.api.dto.board.BoardCategoryResponse;
import com.gw.api.dto.board.SaveBoardCategoryRequest;
import com.gw.api.service.board.BoardCategoryAdminService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/board-categories")
public class AdminBoardCategoryController {

    private final BoardCategoryAdminService boardCategoryAdminService;

    public AdminBoardCategoryController(BoardCategoryAdminService boardCategoryAdminService) {
        this.boardCategoryAdminService = boardCategoryAdminService;
    }

    // 관리자용 게시글 카테고리 목록을 조회한다.
    @GetMapping
    public ApiResponse<List<BoardCategoryResponse>> getBoardCategories() {
        return ApiResponse.ok(boardCategoryAdminService.getBoardCategories());
    }

    // 관리자가 게시글 카테고리를 등록한다.
    @PostMapping
    public ApiResponse<BoardCategoryResponse> createBoardCategory(
            Principal principal,
            @Valid @RequestBody SaveBoardCategoryRequest request
    ) {
        return ApiResponse.ok(boardCategoryAdminService.createBoardCategory(getLoginId(principal), request));
    }

    // 관리자가 게시글 카테고리를 수정한다.
    @PutMapping("/{categoryUuid}")
    public ApiResponse<BoardCategoryResponse> updateBoardCategory(
            Principal principal,
            @PathVariable String categoryUuid,
            @Valid @RequestBody SaveBoardCategoryRequest request
    ) {
        return ApiResponse.ok(boardCategoryAdminService.updateBoardCategory(getLoginId(principal), categoryUuid, request));
    }

    // 관리자가 게시글 카테고리를 삭제한다.
    @DeleteMapping("/{categoryUuid}")
    public ApiResponse<Void> deleteBoardCategory(Principal principal, @PathVariable String categoryUuid) {
        boardCategoryAdminService.deleteBoardCategory(getLoginId(principal), categoryUuid);
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
