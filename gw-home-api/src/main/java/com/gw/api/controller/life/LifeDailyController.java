package com.gw.api.controller.life;

import com.gw.api.dto.life.LifeDailyDtos.*;
import com.gw.api.service.life.LifeDailyService;
import com.gw.share.common.exception.*;
import com.gw.share.common.policy.LifeTodoPolicy;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/life-daily")
public class LifeDailyController {
    private final LifeDailyService service;

    // 잘못된 JSON과 날짜 경로를 입력 오류로 반환한다.
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidInput(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.BAD_REQUEST.getMessage()));
    }

    // 인증 사용자의 월간 체크 캘린더를 요청한다.
    @GetMapping
    public ApiResponse<Month> getMonth(Principal principal, @RequestParam String month,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        return ApiResponse.ok(service.getMonth(loginId(principal), month, timeZone));
    }

    // 오늘부터 반복할 항목 등록을 요청한다.
    @PostMapping
    public ApiResponse<Item> createItem(Principal principal, @Valid @RequestBody SaveItem request,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        return ApiResponse.ok(service.createItem(loginId(principal), request, timeZone));
    }

    // 반복 항목의 제목 수정을 요청한다.
    @PutMapping("/{uuid}")
    public ApiResponse<Item> updateItem(Principal principal, @PathVariable String uuid, @Valid @RequestBody SaveItem request) {
        return ApiResponse.ok(service.updateItem(loginId(principal), uuid, request));
    }

    // 기록을 보존하는 반복 종료를 요청한다.
    @PutMapping("/{uuid}/end")
    public ApiResponse<Void> endItem(Principal principal, @PathVariable String uuid,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        service.endItem(loginId(principal), uuid, timeZone);
        return ApiResponse.ok(null);
    }

    // 선택 날짜의 완료 또는 취소를 요청한다.
    @PutMapping("/{uuid}/checks/{date}")
    public ApiResponse<Void> saveCheck(Principal principal, @PathVariable String uuid,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody Completion request,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        service.saveCheck(loginId(principal), uuid, date, request, timeZone);
        return ApiResponse.ok(null);
    }

    private String loginId(Principal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return principal.getName();
    }
}
