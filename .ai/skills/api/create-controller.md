# Skill: create-api-endpoint

## 입력

```
DOMAIN: {도메인명}
ENDPOINT: {HTTP메서드} {경로}
PURPOSE: {기능 설명}
REQUEST: {요청 파라미터/바디}
RESPONSE: {응답 구조}
```

## 출력

- `{Domain}Controller.java` — 엔드포인트 메서드
- `{Domain}Service.java` — 서비스 메서드
- `{Domain}Mapper.java` — Mapper 인터페이스 메서드
- `{Domain}Mapper.xml` — SQL

## 규칙

- [ ] Controller는 비즈니스 로직 없음 — Service 위임만
- [ ] `@Valid` 사용하여 요청 검증
- [ ] 응답은 `ApiResponse<T>` 래핑
- [ ] 인증 필요 엔드포인트: `@AuthenticationPrincipal` 사용
- [ ] API 경로: `/api/v1/{domains}` (복수형)
- [ ] `_idx` 응답 노출 금지

## 예시

```java
// Controller
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/{boardUuid}")
    public ApiResponse<BoardResponse> getBoard(@PathVariable String boardUuid) {
        return ApiResponse.ok(boardService.findBoard(boardUuid));
    }

    @PostMapping
    public ApiResponse<Void> createBoard(
            @Valid @RequestBody CreateBoardRequest req,
            @AuthenticationPrincipal UserDetails user) {
        boardService.saveBoard(req, user.getUsername());
        return ApiResponse.ok();
    }
}
```

## 구현 순서

1. DDL (스키마 변경 시)
2. Mapper XML (`infra-db`)
3. Mapper Interface (`api`)
4. Service
5. Controller
6. Request/Response DTO
