package com.gw.api.controller.work;

import com.gw.api.dto.work.CreateWorkTodoRequest;
import com.gw.api.dto.work.UpdateWorkTodoCompletionRequest;
import com.gw.api.dto.work.UpdateWorkTodoRequest;
import com.gw.api.dto.work.WorkTodoTreeResponse;
import com.gw.api.service.work.WorkTodoService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
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
@RequestMapping("/api/v1/work-units/{workUnitUuid}/todos")
public class WorkTodoController {

    private final WorkTodoService workTodoService;

    public WorkTodoController(WorkTodoService workTodoService) {
        this.workTodoService = workTodoService;
    }

    // 로그인 사용자의 업무 TODO 트리를 조회한다.
    @GetMapping
    public ApiResponse<WorkTodoTreeResponse> getWorkTodoTree(
            Principal principal,
            @PathVariable String workUnitUuid
    ) {
        return ApiResponse.ok(workTodoService.getWorkTodoTree(getLoginId(principal), workUnitUuid));
    }

    // 로그인 사용자가 업무 TODO를 추가한다.
    @PostMapping
    public ApiResponse<WorkTodoTreeResponse> createWorkTodo(
            Principal principal,
            @PathVariable String workUnitUuid,
            @Valid @RequestBody CreateWorkTodoRequest request
    ) {
        return ApiResponse.ok(workTodoService.createWorkTodo(getLoginId(principal), workUnitUuid, request));
    }

    // 로그인 사용자가 업무 TODO를 수정한다.
    @PutMapping("/{todoUuid}")
    public ApiResponse<WorkTodoTreeResponse> updateWorkTodo(
            Principal principal,
            @PathVariable String workUnitUuid,
            @PathVariable String todoUuid,
            @Valid @RequestBody UpdateWorkTodoRequest request
    ) {
        return ApiResponse.ok(workTodoService.updateWorkTodo(getLoginId(principal), workUnitUuid, todoUuid, request));
    }

    // 로그인 사용자가 최하위 업무 TODO 완료 여부를 변경한다.
    @PutMapping("/{todoUuid}/completion")
    public ApiResponse<WorkTodoTreeResponse> updateWorkTodoCompletion(
            Principal principal,
            @PathVariable String workUnitUuid,
            @PathVariable String todoUuid,
            @Valid @RequestBody UpdateWorkTodoCompletionRequest request
    ) {
        return ApiResponse.ok(workTodoService.updateWorkTodoCompletion(getLoginId(principal), workUnitUuid, todoUuid, request));
    }

    // 로그인 사용자가 업무 TODO를 삭제한다.
    @DeleteMapping("/{todoUuid}")
    public ApiResponse<WorkTodoTreeResponse> deleteWorkTodo(
            Principal principal,
            @PathVariable String workUnitUuid,
            @PathVariable String todoUuid
    ) {
        return ApiResponse.ok(workTodoService.deleteWorkTodo(getLoginId(principal), workUnitUuid, todoUuid));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
