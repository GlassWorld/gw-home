package com.gw.api.controller.life;

import com.gw.api.dto.life.LifeTodoDtos.*;
import com.gw.api.dto.life.LifeTodoListRequest;
import com.gw.api.service.life.LifeTodoService;
import com.gw.share.common.exception.*;
import com.gw.share.common.policy.LifeTodoPolicy;
import com.gw.share.common.response.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/life-todos")
public class LifeTodoController {
    private final LifeTodoService service;

    // 일상 API의 잘못된 JSON·날짜 입력을 서버 오류 대신 400 응답으로 처리한다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.BAD_REQUEST.getMessage()));
    }

    // 인증 사용자의 일상 할 일 목록을 요청한다.
    @GetMapping
    public ApiResponse<PageResponse<Todo>> getTodos(Principal principal, @Valid @ModelAttribute LifeTodoListRequest request) {
        return ApiResponse.ok(service.getTodos(loginId(principal), request));
    }

    // 인증 사용자의 일상 할 일 분류별 집계를 요청한다.
    @GetMapping("/summary")
    public ApiResponse<Summary> getSummary(Principal principal, @Valid @ModelAttribute LifeTodoListRequest request) {
        return ApiResponse.ok(service.getSummary(loginId(principal), request));
    }

    // 본인의 일상 할 일 상세를 요청한다.
    @GetMapping("/{uuid}")
    public ApiResponse<Todo> getTodo(Principal principal, @PathVariable String uuid,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        return ApiResponse.ok(service.getTodo(loginId(principal), uuid, timeZone));
    }

    // 본인의 일상 할 일 생성을 요청한다.
    @PostMapping
    public ApiResponse<Todo> createTodo(Principal principal, @Valid @RequestBody SaveTodo request,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        return ApiResponse.ok(service.createTodo(loginId(principal), request, timeZone));
    }

    // 본인의 일상 할 일 수정을 요청한다.
    @PutMapping("/{uuid}")
    public ApiResponse<Todo> updateTodo(Principal principal, @PathVariable String uuid, @Valid @RequestBody SaveTodo request,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        return ApiResponse.ok(service.updateTodo(loginId(principal), uuid, request, timeZone));
    }

    // 본인의 일상 할 일 완료·취소를 요청한다.
    @PutMapping("/{uuid}/completion")
    public ApiResponse<Todo> completeTodo(Principal principal, @PathVariable String uuid, @Valid @RequestBody Completion request,
            @RequestParam(defaultValue = LifeTodoPolicy.DEFAULT_TIME_ZONE) String timeZone) {
        return ApiResponse.ok(service.completeTodo(loginId(principal), uuid, request, timeZone));
    }

    // 본인의 일상 할 일 삭제를 요청한다.
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteTodo(Principal principal, @PathVariable String uuid) {
        service.deleteTodo(loginId(principal), uuid);
        return ApiResponse.ok(null);
    }

    // 본인의 생활 카테고리 목록을 요청한다.
    @GetMapping("/categories")
    public ApiResponse<List<Category>> getCategories(Principal principal) {
        return ApiResponse.ok(service.getCategories(loginId(principal)));
    }

    // 본인의 생활 카테고리 생성을 요청한다.
    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(Principal principal, @Valid @RequestBody SaveCategory request) {
        return ApiResponse.ok(service.createCategory(loginId(principal), request));
    }

    // 본인의 생활 카테고리 이름 변경을 요청한다.
    @PutMapping("/categories/{uuid}")
    public ApiResponse<Category> updateCategory(Principal principal, @PathVariable String uuid, @Valid @RequestBody SaveCategory request) {
        return ApiResponse.ok(service.updateCategory(loginId(principal), uuid, request));
    }

    // 본인의 생활 카테고리를 삭제하고 할 일의 미분류 전환을 요청한다.
    @DeleteMapping("/categories/{uuid}")
    public ApiResponse<Void> deleteCategory(Principal principal, @PathVariable String uuid) {
        service.deleteCategory(loginId(principal), uuid);
        return ApiResponse.ok(null);
    }

    // 미인증 요청은 사용자 조회 전에 차단한다.
    private String loginId(Principal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return principal.getName();
    }
}
