# Project Summary

## 기술 스택

- Java 17, Spring Boot 3.x
- MyBatis (JPA/Querydsl 금지)
- PostgreSQL
- Gradle Multi-module

## 모듈 구조

| 모듈 | 역할 |
|------|------|
| `{project}-share` | 공통 DTO, 예외, 유틸 (디렉토리: `{project}-share/`) |
| `{project}-api` | Controller, Service, Request/Response DTO (디렉토리: `{project}-api/`) |
| `{project}-infra-db` | Mapper interface, Mapper XML, DB 설정, DDL 스크립트 (디렉토리: `{project}-infra-db/`) |

`projectName`은 `gradle.properties`에서 관리한다.

## 도메인

```
account   회원 가입/탈퇴/계정
auth      로그인/토큰
profile   프로필 조회/수정
board     게시글 CRUD
comment   댓글/대댓글
file      파일 업로드 (독립)
tag       태그 관리
favorite  좋아요
admin     관리자
```

## DB 핵심 규칙

```
PK:       {table}_idx  BIGSERIAL
UUID:     {table}_uuid UUID (외부 노출 전용)
Time:     TIMESTAMPTZ (NOT TIMESTAMP)
Audit:    created_by = 로그인 ID
Delete:   del_at TIMESTAMPTZ (소프트 삭제)
```

## 핵심 설계 철학

1. **단순성 우선** — 불필요한 추상화 금지
2. **명시적 SQL** — ORM 추상화 없이 SQL 직접 작성
3. **도메인 경계 준수** — 도메인 간 직접 참조 최소화
4. **파일 분리** — 파일 업로드는 `file` 도메인이 전담, 타 도메인은 URL만 저장
5. **외부 노출 최소화** — 내부 PK(`_idx`) API 응답 노출 금지

## 패키지 규칙

```
com.gw.{module}.{layer}.{domain}
```

## MyBatis 모델 규칙

- 기본 조회/저장 모델: `{Domain}Vo` (단일 테이블 컬럼 그대로)
- 조인 조회 모델: `{Domain}Jvo` (조인/확장 결과)
- `VO` / `JVO`는 `share` 모듈에서 관리
- DDL, `VO`, `JVO` 이름과 필드는 감사 컬럼을 제외하고 축약형으로 통일
- 공통 PK/UUID/감사 컬럼은 `BaseVo`로 관리
- Mapper XML은 기본적으로 `resultType` 사용
- `resultType`에는 패키지명을 쓰지 않음
- `BaseVo` 매핑 시 대표 PK/UUID 컬럼은 `AS idx`, `AS uuid` alias 사용
- `VO` / `JVO`는 테이블 컬럼 기준 camelCase를 사용한다 (`mbrAcctIdx`, `createdAt`)
- `VO` / `JVO`는 Lombok `@SuperBuilder` 기반으로 작성

## 네이밍

| 대상 | 규칙 |
|------|------|
| DB 컬럼 | snake_case |
| Java 필드 | camelCase |
| Mapper 메서드 | select/insert/update/delete + 명사 |
| DTO | `{동사}{명사}Request`, `{명사}Response` |
| Frontend 변수 | full name (축약어 금지) |
