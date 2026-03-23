# Backend Rules

## 절대 규칙 (위반 금지)

- [ ] JPA 사용 금지 (`@Entity`, `@Repository`, `JpaRepository` 등)
- [ ] Querydsl 사용 금지
- [ ] MyBatis만 사용 — Mapper 인터페이스 + XML
- [ ] `_idx`를 API 응답에 노출 금지
- [ ] `TIMESTAMP` (without timezone) 사용 금지 → `TIMESTAMPTZ` 사용

## Mapper 규칙

- Mapper 인터페이스: `{project}-infra-db` 모듈 `mapper.{domain}` 패키지
- Mapper XML: `{project}-infra-db` 모듈 `resources/mapper/{domain}/`
- XML namespace = Mapper 인터페이스 fully qualified name
- 동적 쿼리는 XML `<if>`, `<choose>`, `<foreach>` 사용
- 조회 결과는 기본적으로 `resultMap` 대신 `resultType` 사용
- 단일 테이블 조회 모델: `{Domain}Vo`, 조인 조회 모델: `{Domain}Jvo`
- `resultType`에는 패키지명을 쓰지 않고 별칭만 사용
- `mapUnderscoreToCamelCase=true` 기본 적용
- `VO`는 단일 테이블 컬럼을 그대로 가져야 함
- `JVO`는 조인 등으로 확장되거나 변경된 조회 결과에만 사용
- DDL, `VO`, `JVO`는 감사 컬럼(`created_by`, `updated_by`, `created_at`, `updated_at`)을 제외하고 축약형 네이밍 사용
- 공통 PK/UUID/감사 컬럼은 `BaseVo`로 관리
- 기본 PK/UUID 컬럼은 `resultType` 매핑 시 `AS idx`, `AS uuid` alias 우선 사용
- `VO` / `JVO` 필드명은 테이블 컬럼 기준 camelCase (`idx`, `uuid`, `mbrAcctIdx`)
- `VO` / `JVO`는 Lombok(`@Getter`, `@Setter`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`) 기본 사용
- `VO` / `JVO` 필드에는 DB 컬럼 코멘트를 주석으로 남김

```
✅ infra-db/src/main/java/com/gw/infra/db/mapper/board/BoardMapper.java
✅ infra-db/src/main/resources/mapper/board/BoardMapper.xml
```

## Service 규칙

- `@Service` + `@Transactional` 기본 적용
- 조회 메서드: `@Transactional(readOnly = true)`
- Service는 다른 Service를 직접 호출 가능 (순환 금지)
- 페이징: `PageRequest` DTO 수신 → `PageResponse` 반환

## 패키지 규칙

```
com.gw.api.controller.{domain}   # @RestController
com.gw.api.service.{domain}      # @Service
com.gw.infra.db.mapper.{domain}  # MyBatis Mapper interface
com.gw.api.dto.{domain}          # Request/Response DTO
com.gw.share.common.{category}   # 공통
com.gw.share.vo.{domain}         # 기본 조회/저장 VO
com.gw.share.jvo.{domain}        # 조인 조회 JVO
```

도메인 기반 패키지는 모든 모듈에서 `com.gw.{module}.{layer}.{domain}` 순서를 기본 규칙으로 사용한다.

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| Controller 메서드 | 동사 + 명사 | `getBoard`, `createBoard` |
| Service 메서드 | 동사 + 명사 | `findBoard`, `saveBoard` |
| Mapper 메서드 | `select` / `insert` / `update` / `delete` + 명사 | `selectBoardByUuid` |
| DTO (Request) | `{동사}{명사}Request` | `CreateBoardRequest` |
| DTO (Response) | `{명사}Response` | `BoardResponse` |
| VO / JVO 공통 필드 | `BaseVo` 상속 | `idx`, `uuid`, `createdAt` |
| VO / JVO 도메인 필드 | 테이블 컬럼 camelCase | `mbrAcctIdx`, `brdCtgrIdx` |
| XML id | Mapper 메서드명과 동일 | `selectBoardByUuid` |
| DB 컬럼 | 혼합 snake_case | `brd_pst_idx`, `created_at` |
| Java 필드 | 혼합 camelCase | `brdPstIdx`, `createdAt` |
