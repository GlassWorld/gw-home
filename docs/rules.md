# Rules

## 절대 규칙 (위반 금지)

- [ ] JPA 사용 금지 (`@Entity`, `@Repository`, `JpaRepository` 등)
- [ ] Querydsl 사용 금지
- [ ] MyBatis만 사용 — Mapper 인터페이스 + XML
- [ ] `_idx`를 API 응답에 노출 금지
- [ ] `TIMESTAMP` (without timezone) 사용 금지 → `TIMESTAMPTZ` 사용

## Mapper 규칙

- Mapper 인터페이스: `api` 모듈 `{domain}.mapper` 패키지
- Mapper XML: `infra-db` 모듈 `resources/mapper/{domain}/`
- XML namespace = Mapper 인터페이스 fully qualified name
- 동적 쿼리는 XML `<if>`, `<choose>`, `<foreach>` 사용

```
✅ api/src/.../board/mapper/BoardMapper.java
✅ infra-db/src/resources/mapper/board/BoardMapper.xml
```

## Service 규칙

- `@Service` + `@Transactional` 기본 적용
- 조회 메서드는 `@Transactional(readOnly = true)`
- Service는 다른 Service를 직접 호출 가능 (순환 금지)
- 페이징: `PageRequest` DTO 수신 → `PageResponse` 반환

## 패키지 규칙

```
com.gw.api.{domain}.controller   # @RestController
com.gw.api.{domain}.service      # @Service
com.gw.api.{domain}.mapper       # MyBatis Mapper interface
com.gw.api.{domain}.dto          # Request/Response DTO
com.gw.share.common.{category}   # 공통
```

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| Controller 메서드 | 동사 + 명사 | `getBoard`, `createBoard` |
| Service 메서드 | 동사 + 명사 | `findBoard`, `saveBoard` |
| Mapper 메서드 | `select` / `insert` / `update` / `delete` + 명사 | `selectBoardByUuid` |
| DTO (Request) | `{동사}{명사}Request` | `CreateBoardRequest` |
| DTO (Response) | `{명사}Response` | `BoardResponse` |
| XML id | Mapper 메서드명과 동일 | `selectBoardByUuid` |
| DB 컬럼 | snake_case | `board_post_idx` |
| Java 필드 | camelCase | `boardPostIdx` |

## Frontend 규칙

- 축약어 사용 금지 (`btn` → `button`, `usr` → `user`, `idx` → `index`)
- 컴포넌트명: PascalCase
- 함수명: camelCase (동사로 시작)
- 파일명: kebab-case
