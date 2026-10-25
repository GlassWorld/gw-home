# gw-home

Nuxt3 + Spring Boot 커뮤니티 플랫폼 (Front / Back 통합 관리)

`gradle.properties`의 `projectName` 값을 기준으로 루트 프로젝트명과 Gradle 모듈명이 결정된다.
프론트(`{project}-ui`)는 Nuxt3 기반으로 별도 디렉토리에서 관리하며, 동일 레포에서 함께 개발한다.

## 왜 프론트/백엔드를 하나의 레포에서 관리하는가

- 개인 프로젝트로 팀 간 분리 필요 없음
- API 연계를 바이브코딩으로 빠르게 진행하기 위해 UI 모듈을 동일 레포에 배치
- 백엔드 DTO 변경과 프론트 타입 변경을 한 커밋에서 추적 가능

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend Language | Java 21 |
| Backend Framework | Spring Boot 3.3.5 |
| DB Access | MyBatis 3.0.3 (JPA/Querydsl 금지) |
| Database | PostgreSQL 42.7.4 |
| Auth | Spring Security + jjwt 0.12.6 |
| Build | Gradle (Multi-module) |
| Frontend Framework | Nuxt3 3.16.x |
| Frontend Language | TypeScript |

## 실행 환경

- Node.js 22.20.0 이상
- npm 10.9.3 이상
- 루트 `.nvmrc` 기준으로 프론트 실행 환경을 맞춘다.

## 모듈 구조

```
gw-home/
├── gw-home-share/      # 공통 DTO, 유틸, 상수, 예외
├── gw-home-api/        # Controller, Service, DTO (도메인별)
├── gw-home-infra-db/   # MyBatis Mapper XML, Mapper, DB 설정
└── gw-home-ui/         # Nuxt3 프론트엔드
```

## Gradle 모듈명

`projectName=gw-home`일 때:

- `gw-home-share`
- `gw-home-api`
- `gw-home-infra-db`
- `gw-home-ui` (Nuxt3 — Gradle 빌드 외부, npm/node 관리)

## 도메인

`account` `auth` `profile` `board` `comment` `file` `tag` `favorite` `admin`

## 핵심 규칙

**Backend / DB**
- **MyBatis only** — JPA, Querydsl 절대 금지
- **DDL**: PK는 `{table}_idx` (BIGSERIAL), UUID는 `{table}_uuid` (외부 노출 전용)
- **timestamp**: `TIMESTAMPTZ` 사용, `created_by` = 로그인 ID
- **패키지**: `com.gw.{module}.{layer}.{domain}`
- **축약형**: DDL, VO, JVO는 감사 컬럼 제외 축약형 (`tb_mbr_acct`, `AcctVo`)
- **BaseVo**: 공통 PK/UUID/감사 컬럼은 `BaseVo` 상속

**Frontend**
- 축약어 사용 금지 (`button` not `btn`, `user` not `usr`)
- 파일명: kebab-case
- 컴포넌트명: PascalCase
- 초기 진입: 로그인 화면 → 로그인 성공 후 대시보드

## 실행

기존 DB에는 API 시작 전에 [리뉴얼 테이블 생성·적용 안내](docs/backend/database.md#리뉴얼-테이블-생성적용-안내)에 따라 [통합 적용 SQL](gw-home-infra-db/src/main/resources/sql/ddl/work/project-renewal-migrate.sql)을 실행하고 [확인 SQL](gw-home-infra-db/src/main/resources/sql/ddl/work/project-renewal-verify.sql)로 점검한다. 서버 시작만으로 DDL이 자동 반영되지 않는다. 메일 발송을 보류해도 현재 TODO 쓰기에 알림 테이블 3개가 필요하다.

일상 할 일은 별도 테이블을 사용하므로 [일상 할 일 적용 안내](docs/backend/database.md#일상-할-일-스키마-추가-2026-10-25)에 따라 [일상 마이그레이션 SQL](gw-home-infra-db/src/main/resources/sql/ddl/life/life-todos-migrate.sql)도 실행한다. 각 SQL의 로컬 검증과 실제 대상 DB 적용 여부는 구분하며, 대상 DB 적용·배포는 아직 수행하지 않았다.

로컬 백엔드 환경값은 저장소 루트 또는 실행 폴더의 `.env.local`에서 읽는다. `.env`와 `.env.local` 등 실제 환경 파일은 Git과 Docker 빌드 컨텍스트에서 제외한다. 공유용 예제는 비밀값 없이 `.env.example`로 관리한다.

```bash
# Backend
./gradlew :gw-home-api:bootRun

# Frontend
cd gw-home-ui && nvm use
cd gw-home-ui && npm install
cd gw-home-ui && npm run dev
```

## 문서 위치

| 경로 | 내용 |
|------|------|
| `docs/common/` | 전체 아키텍처, 프로젝트 구조 |
| `docs/backend/` | 백엔드 규칙, DB, 도메인 |
| `docs/frontend/` | 프론트 규칙, 페이지, 인증 흐름 |
| `.ai/` | AI 작업 가이드 |
| `.ai/AGENTS.md` | AI 진입점 |
| `todo/` | 작업 순서 및 완료 기준 |
