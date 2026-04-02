package com.gw.api.controller.work;

import com.gw.api.dto.work.WorkGitAccountRequest;
import com.gw.api.dto.work.WorkGitAccountResponse;
import com.gw.api.dto.work.WorkGitConnectionTestResponse;
import com.gw.api.dto.work.WorkGitProjectRequest;
import com.gw.api.dto.work.WorkGitProjectResponse;
import com.gw.api.service.work.WorkGitService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/work-git")
public class WorkGitController {

    private final WorkGitService workGitService;

    public WorkGitController(WorkGitService workGitService) {
        this.workGitService = workGitService;
    }

    // 로그인 사용자의 Git 계정 목록을 조회한다.
    @GetMapping("/accounts")
    public ApiResponse<List<WorkGitAccountResponse>> getGitAccounts(Principal principal) {
        return ApiResponse.ok(workGitService.getGitAccounts(getLoginId(principal)));
    }

    // 로그인 사용자가 Git 계정을 생성한다.
    @PostMapping("/accounts")
    public ApiResponse<WorkGitAccountResponse> createGitAccount(
            Principal principal,
            @Valid @RequestBody WorkGitAccountRequest request
    ) {
        return ApiResponse.ok(workGitService.createGitAccount(getLoginId(principal), request));
    }

    // 로그인 사용자가 Git 계정을 수정한다.
    @PutMapping("/accounts/{uuid}")
    public ApiResponse<WorkGitAccountResponse> updateGitAccount(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody WorkGitAccountRequest request
    ) {
        return ApiResponse.ok(workGitService.updateGitAccount(getLoginId(principal), uuid, request));
    }

    // 로그인 사용자가 Git 계정을 삭제한다.
    @DeleteMapping("/accounts/{uuid}")
    public ApiResponse<Void> deleteGitAccount(
            Principal principal,
            @PathVariable String uuid
    ) {
        workGitService.deleteGitAccount(getLoginId(principal), uuid);
        return ApiResponse.ok(null);
    }

    // 로그인 사용자의 Git 프로젝트 목록을 조회한다.
    @GetMapping("/projects")
    public ApiResponse<List<WorkGitProjectResponse>> getGitProjects(
            Principal principal,
            @RequestParam(required = false) String gitAccountUuid
    ) {
        return ApiResponse.ok(workGitService.getGitProjects(getLoginId(principal), gitAccountUuid));
    }

    // 로그인 사용자의 Git 프로젝트 선택 옵션을 조회한다.
    @GetMapping("/projects/options")
    public ApiResponse<List<WorkGitProjectResponse>> getGitProjectOptions(Principal principal) {
        return ApiResponse.ok(workGitService.getGitProjectOptions(getLoginId(principal)));
    }

    // 로그인 사용자가 Git 프로젝트를 생성한다.
    @PostMapping("/projects")
    public ApiResponse<WorkGitProjectResponse> createGitProject(
            Principal principal,
            @Valid @RequestBody WorkGitProjectRequest request
    ) {
        return ApiResponse.ok(workGitService.createGitProject(getLoginId(principal), request));
    }

    // 로그인 사용자가 Git 프로젝트를 수정한다.
    @PutMapping("/projects/{uuid}")
    public ApiResponse<WorkGitProjectResponse> updateGitProject(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody WorkGitProjectRequest request
    ) {
        return ApiResponse.ok(workGitService.updateGitProject(getLoginId(principal), uuid, request));
    }

    // 로그인 사용자가 Git 프로젝트를 삭제한다.
    @DeleteMapping("/projects/{uuid}")
    public ApiResponse<Void> deleteGitProject(
            Principal principal,
            @PathVariable String uuid
    ) {
        workGitService.deleteGitProject(getLoginId(principal), uuid);
        return ApiResponse.ok(null);
    }

    // 로그인 사용자가 Git 프로젝트 연결을 테스트한다.
    @PostMapping("/projects/{uuid}/connection-test")
    public ApiResponse<WorkGitConnectionTestResponse> testGitProjectConnection(
            Principal principal,
            @PathVariable String uuid
    ) {
        return ApiResponse.ok(workGitService.testGitProjectConnection(getLoginId(principal), uuid));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getName();
    }
}
