package com.gw.api.controller.work;

import com.gw.api.dto.work.CreateWorkUnitRequest;
import com.gw.api.dto.work.UpdateWorkUnitRequest;
import com.gw.api.dto.work.UpdateWorkUnitUseRequest;
import com.gw.api.dto.work.WorkUnitListRequest;
import com.gw.api.dto.work.WorkUnitOptionResponse;
import com.gw.api.dto.work.WorkUnitResponse;
import com.gw.api.service.work.WorkUnitService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/work-units")
public class WorkUnitController {

    private final WorkUnitService workUnitService;

    public WorkUnitController(WorkUnitService workUnitService) {
        this.workUnitService = workUnitService;
    }

    @GetMapping
    public ApiResponse<List<WorkUnitResponse>> getWorkUnitList(Principal principal, WorkUnitListRequest request) {
        return ApiResponse.ok(workUnitService.getWorkUnitList(getLoginId(principal), request));
    }

    @GetMapping("/options")
    public ApiResponse<List<WorkUnitOptionResponse>> getWorkUnitOptions(
            Principal principal,
            @RequestParam(required = false) Boolean includeInactive
    ) {
        return ApiResponse.ok(workUnitService.getWorkUnitOptions(getLoginId(principal), includeInactive));
    }

    @PostMapping
    public ApiResponse<WorkUnitResponse> createWorkUnit(
            Principal principal,
            @Valid @RequestBody CreateWorkUnitRequest request
    ) {
        return ApiResponse.ok(workUnitService.createWorkUnit(getLoginId(principal), request));
    }

    @PutMapping("/{uuid}")
    public ApiResponse<WorkUnitResponse> updateWorkUnit(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateWorkUnitRequest request
    ) {
        return ApiResponse.ok(workUnitService.updateWorkUnit(getLoginId(principal), uuid, request));
    }

    @PutMapping("/{uuid}/use")
    public ApiResponse<WorkUnitResponse> updateWorkUnitUse(
            Principal principal,
            @PathVariable String uuid,
            @Valid @RequestBody UpdateWorkUnitUseRequest request
    ) {
        return ApiResponse.ok(workUnitService.updateWorkUnitUse(getLoginId(principal), uuid, request.useYn()));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
