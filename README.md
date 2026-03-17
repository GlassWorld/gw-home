# gw-home

Spring Boot 기반 커뮤니티 플랫폼 (MyBatis + PostgreSQL)

`gradle.properties`의 `projectName` 값을 기준으로 루트 프로젝트명과 Gradle 모듈명이 함께 결정된다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| DB Access | MyBatis (JPA/Querydsl 금지) |
| Database | PostgreSQL |
| Build | Gradle (Multi-module) |

## 모듈 구조

``` 
gw-home/
├── gw-home-share/      # 공통 DTO, 유틸, 상수, 예외
├── gw-home-api/        # Controller, Service, DTO (도메인별)
└── gw-home-infra-db/   # MyBatis Mapper XML, Mapper, DB 설정
```

## Gradle 모듈명

`projectName=gw-home`일 때 아래 형식을 사용한다.

- `gw-home-share`
- `gw-home-api`
- `gw-home-infra-db`

현재 디렉토리도 같은 이름 규칙을 사용한다.

## 도메인

`account` `auth` `profile` `board` `comment` `file` `tag` `favorite` `admin`

## 핵심 규칙

- **MyBatis only** — JPA, Querydsl 절대 금지
- **DDL**: PK는 `{table}_idx` (BIGSERIAL), UUID는 `{table}_uuid` (외부 노출 전용)
- **timestamp**: `timestamptz` 사용, `created_by` = 로그인 ID
- **패키지**: `com.gw.{module}.{layer}.{domain}`
- **MyBatis 조회 모델**: `VO`는 단일 테이블, `JVO`는 조인/확장 조회이며 공용 모델은 `share` 모듈에서 관리
- **내부 축약형 규칙**: DDL, VO, JVO는 감사 컬럼을 제외하고 축약형 네이밍을 사용 (`tb_mbr_acct`, `AcctVo`, `lgnId`)
- **BaseVo**: 공통 PK/UUID/감사 컬럼(`idx`, `uuid`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`, `delAt`)은 `BaseVo`로 통합
- **파일 업로드**: `file` 도메인으로 분리, 게시글 본문은 이미지 URL 저장

## 실행

```bash
./gradlew :gw-home-api:bootRun
```

## 문서

| 경로 | 내용 |
|------|------|
| `docs/` | 설계 기준 문서 |
| `.ai/` | AI 작업 가이드 |
| `.ai/AGENTS.md` | AI 진입점 |
