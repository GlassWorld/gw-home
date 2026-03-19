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
| Backend Framework | Spring Boot 3.x |
| DB Access | MyBatis (JPA/Querydsl 금지) |
| Database | PostgreSQL |
| Build | Gradle (Multi-module) |
| Frontend Framework | Nuxt3 |
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
