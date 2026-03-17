# Project Summary

## 기술 스택

- Java 17, Spring Boot 3.x
- MyBatis (JPA/Querydsl 금지)
- PostgreSQL
- Gradle Multi-module

## 모듈 구조

| 모듈 | 역할 |
|------|------|
| `share` | 공통 DTO, 예외, 유틸 (의존 없음) |
| `api` | Controller, Service, Mapper interface |
| `infra-db` | Mapper XML, DB 설정, DDL 스크립트 |

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
Delete:   deleted_at TIMESTAMPTZ (소프트 삭제)
```

## 핵심 설계 철학

1. **단순성 우선** — 불필요한 추상화 금지
2. **명시적 SQL** — ORM 추상화 없이 SQL 직접 작성
3. **도메인 경계 준수** — 도메인 간 직접 참조 최소화
4. **파일 분리** — 파일 업로드는 `file` 도메인이 전담, 타 도메인은 URL만 저장
5. **외부 노출 최소화** — 내부 PK(`_idx`) API 응답 노출 금지

## 패키지 규칙

```
com.gw.{module}.{domain}.{layer}
```

## 네이밍

| 대상 | 규칙 |
|------|------|
| DB 컬럼 | snake_case |
| Java 필드 | camelCase |
| Mapper 메서드 | select/insert/update/delete + 명사 |
| DTO | `{동사}{명사}Request`, `{명사}Response` |
| Frontend 변수 | full name (축약어 금지) |
