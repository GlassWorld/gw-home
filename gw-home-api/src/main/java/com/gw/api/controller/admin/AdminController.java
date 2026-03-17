package com.gw.api.controller.admin;

import com.gw.api.dto.admin.AdminBoardPostListRequest;
import com.gw.api.dto.admin.AdminBoardPostResponse;
import com.gw.api.dto.admin.AdminMemberListRequest;
import com.gw.api.dto.admin.AdminMemberResponse;
import com.gw.api.dto.admin.AdminSummaryResponse;
import com.gw.api.service.admin.AdminService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import com.gw.share.common.response.PageResponse;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/members")
    public ApiResponse<PageResponse<AdminMemberResponse>> getMembers(AdminMemberListRequest request) {
        return ApiResponse.ok(adminService.getMemberList(request));
    }

    @GetMapping("/members/{memberAccountUuid}")
    public ApiResponse<AdminMemberResponse> getMember(@PathVariable String memberAccountUuid) {
        return ApiResponse.ok(adminService.getMember(memberAccountUuid));
    }

    @DeleteMapping("/members/{memberAccountUuid}")
    public ApiResponse<Void> forceDeleteMember(Principal principal, @PathVariable String memberAccountUuid) {
        adminService.forceDeleteMember(getLoginId(principal), memberAccountUuid);
        return ApiResponse.ok();
    }

    @GetMapping("/boards")
    public ApiResponse<PageResponse<AdminBoardPostResponse>> getBoardPosts(AdminBoardPostListRequest request) {
        return ApiResponse.ok(adminService.getBoardPostList(request));
    }

    @DeleteMapping("/boards/{boardPostUuid}")
    public ApiResponse<Void> forceDeleteBoardPost(Principal principal, @PathVariable String boardPostUuid) {
        adminService.forceDeleteBoardPost(getLoginId(principal), boardPostUuid);
        return ApiResponse.ok();
    }

    @GetMapping("/summary")
    public ApiResponse<AdminSummaryResponse> getSummary() {
        return ApiResponse.ok(adminService.getSummary());
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
