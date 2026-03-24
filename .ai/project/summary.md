# Project Summary

## 개요

개인 커뮤니티 플랫폼. Frontend(Nuxt3)와 Backend(Spring Boot)를 단일 레포에서 통합 관리한다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend Language | Java 21 |
| Backend Framework | Spring Boot 3.3.5 |
| DB Access | MyBatis 3.0.3 (JPA/Querydsl 금지) |
| Database | PostgreSQL 42.7.4 |
| Auth | Spring Security + jjwt 0.12.6 |
| Build | Gradle Multi-module |
| Frontend Framework | Nuxt3 3.16.x |
| Frontend Language | TypeScript |

## 모듈 구조

| 모듈 | 역할 |
|------|------|
| `{project}-share` | 공통 DTO, 예외, 유틸 (Backend 공유) |
| `{project}-api` | Controller, Service, Request/Response DTO |
| `{project}-infra-db` | Mapper interface, Mapper XML, DB 설정, DDL |
| `{project}-ui` | Nuxt3 프론트엔드 (pages, composables, stores, types) |

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

## Frontend 핵심 규칙

```
초기 라우트:  /login
로그인 후:    /dashboard
축약어:       금지 (full name 사용)
파일명:       kebab-case
컴포넌트:     PascalCase
타입:         any 금지, 명시적 TypeScript
```

## 핵심 설계 원칙

1. **MyBatis only** — JPA, Querydsl 절대 금지
2. **명시적 SQL** — ORM 추상화 없이 SQL 직접 작성
3. **도메인 경계 준수** — `account/auth/profile` 분리, `board/admin` 분리
4. **파일 분리** — `file` 도메인 독립, 타 도메인은 URL만 저장
5. **외부 노출 최소화** — 내부 PK(`_idx`) API 응답 노출 금지
6. **Full name** — Frontend는 축약어 없이 full name 사용

## Backend 패키지 규칙

```
com.gw.{module}.{layer}.{domain}
```

## Backend MyBatis 모델 규칙

- 기본 조회/저장 모델: `{Domain}Vo` (단일 테이블 컬럼 그대로)
- 조인 조회 모델: `{Domain}Jvo` (조인/확장 결과)
- `VO` / `JVO`는 `share` 모듈에서 관리
- DDL, `VO`, `JVO` 이름과 필드는 감사 컬럼 제외 축약형
- 공통 PK/UUID/감사 컬럼은 `BaseVo`로 관리
- `VO` / `JVO`는 Lombok `@SuperBuilder` 기반

## 네이밍 요약

| 대상 | 규칙 |
|------|------|
| DB 컬럼 | 축약 snake_case |
| Java 필드 (VO/JVO) | 축약 camelCase |
| Mapper 메서드 | select/insert/update/delete + 명사 |
| Backend DTO | `{동사}{명사}Request`, `{명사}Response` |
| Frontend 변수/함수 | full name camelCase (축약어 금지) |
| Frontend 파일 | kebab-case |
| Frontend 컴포넌트 | PascalCase |
