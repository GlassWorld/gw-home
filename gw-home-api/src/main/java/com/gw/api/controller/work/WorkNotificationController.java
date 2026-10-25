package com.gw.api.controller.work;

import com.gw.api.dto.work.WorkNotificationDtos.*;
import com.gw.api.service.work.WorkNotificationService;
import com.gw.share.common.response.*;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1")
public class WorkNotificationController {
    private final WorkNotificationService service;
    // 로그인 사용자의 메일 설정을 조회한다.
    @GetMapping("/work-notifications/settings")
    public ApiResponse<SettingsResponse> settings(Principal user) { return ApiResponse.ok(service.getSettings(user.getName())); }
    // 로그인 사용자의 메일 설정을 변경한다.
    @PutMapping("/work-notifications/settings")
    public ApiResponse<SettingsResponse> saveSettings(Principal user,@Valid @RequestBody SettingsRequest request) {
        return ApiResponse.ok(service.saveSettings(user.getName(),request));
    }
    // 로그인 사용자의 저장된 수신 주소에 확인 코드를 발급한다.
    @PostMapping("/work-notifications/verification")
    public ApiResponse<String> requestVerification(Principal user) { return ApiResponse.ok(service.requestVerification(user.getName())); }
    // 로그인 사용자가 수신 주소 확인 코드를 제출한다.
    @PostMapping("/work-notifications/verification/confirm")
    public ApiResponse<VerifyResponse> verify(Principal user,@Valid @RequestBody VerifyRequest request) {
        return ApiResponse.ok(service.verifyAddress(user.getName(),request.code()));
    }
    // 로그인 사용자의 발송 이력을 조회한다.
    @GetMapping("/work-notifications/deliveries")
    public ApiResponse<PageResponse<DeliveryResponse>> history(Principal user,@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) { return ApiResponse.ok(service.getHistory(user.getName(),page,size)); }
    // 로그인 사용자의 TODO 개별 예약을 조회한다.
    @GetMapping("/work-todos/{todoUuid}/reminder")
    public ApiResponse<ReminderResponse> reminder(Principal user,@PathVariable String todoUuid) {
        return ApiResponse.ok(service.getReminder(user.getName(),todoUuid));
    }
    // 로그인 사용자의 TODO 개별 예약을 저장한다.
    @PutMapping("/work-todos/{todoUuid}/reminder")
    public ApiResponse<ReminderResponse> saveReminder(Principal user,@PathVariable String todoUuid,@Valid @RequestBody ReminderRequest request) {
        return ApiResponse.ok(service.saveReminder(user.getName(),todoUuid,request.sendAt()));
    }
    // 로그인 사용자의 TODO 개별 예약을 해제한다.
    @DeleteMapping("/work-todos/{todoUuid}/reminder")
    public ApiResponse<Void> cancelReminder(Principal user,@PathVariable String todoUuid) {
        service.cancelReminder(user.getName(),todoUuid); return ApiResponse.ok(null);
    }
}
