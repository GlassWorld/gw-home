---
name: create-api-endpoint
description: Create full backend API endpoint including Controller, Service, Mapper, XML, and DTOs.
tags:
  - backend
  - api
  - spring
  - controller
  - service
  - mybatis
---

# create-api-endpoint

## Use When

- A new REST API endpoint is needed end-to-end
- Controller + Service + Mapper + DTO must all be created together
- A CRUD operation needs full backend implementation

## Read First

- `docs/backend/backend-rules.md`
- `docs/common/architecture.md`
- `docs/common/api-contract.md`

## Input

```
DOMAIN: {도메인명}
ENDPOINT: {HTTP메서드} {경로}
PURPOSE: {기능 설명}
REQUEST: {요청 파라미터/바디}
RESPONSE: {응답 구조}
```

## Output

Provide full implementation plan and diff:

- `{project}-api/.../controller/{domain}/{Domain}Controller.java`
- `{project}-api/.../service/{domain}/{Domain}Service.java`
- `{project}-api/.../dto/{domain}/Create{Domain}Request.java`
- `{project}-api/.../dto/{domain}/{Domain}Response.java`
- `{project}-infra-db/.../mapper/{domain}/{Domain}Mapper.java`
- `{project}-infra-db/.../mapper/{domain}/{Domain}Mapper.xml`

Schema change → also produce DDL file (`generate-ddl` pattern)

## Implementation Order

1. DDL (schema change only) → `generate-ddl`
2. VO / JVO in `{project}-share`
3. Mapper XML
4. Mapper Interface
5. Service
6. Controller + DTOs

## Must Follow

- Controller: no business logic — delegate to Service only
- Validate request with `@Valid`
- Wrap response in `ApiResponse<T>`
- Auth-required endpoints: use `@AuthenticationPrincipal`
- API path: `/api/v1/{domains}` (plural)
- Package order: `com.gw.{module}.{layer}.{domain}`

## Never

- ❌ Expose `_idx` in any response
- ❌ Put business logic in Controller
- ❌ Use JPA or `@Entity`
- ❌ Return raw VO from Controller — always map to Response DTO

## Controller Example

```java
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

    @DeleteMapping("/{boardUuid}")
    public ApiResponse<Void> deleteBoard(
            @PathVariable String boardUuid,
            @AuthenticationPrincipal UserDetails user) {
        boardService.deleteBoard(boardUuid, user.getUsername());
        return ApiResponse.ok();
    }
}
```
