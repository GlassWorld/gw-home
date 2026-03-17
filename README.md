# gw-home

Spring Boot 기반 커뮤니티 플랫폼 (MyBatis + PostgreSQL)

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
├── share/      # 공통 DTO, 유틸, 상수, 예외
├── api/        # Controller, Service, Mapper (도메인별)
└── infra-db/   # MyBatis Mapper XML, DB 설정
```

## 도메인

`account` `auth` `profile` `board` `comment` `file` `tag` `favorite` `admin`

## 핵심 규칙

- **MyBatis only** — JPA, Querydsl 절대 금지
- **DDL**: PK는 `{table}_idx` (BIGSERIAL), UUID는 `{table}_uuid` (외부 노출 전용)
- **timestamp**: `timestamptz` 사용, `created_by` = 로그인 ID
- **패키지**: `com.gw.{module}.{domain}.{layer}`
- **파일 업로드**: `file` 도메인으로 분리, 게시글 본문은 이미지 URL 저장

## 실행

```bash
./gradlew :api:bootRun
```

## 문서

| 경로 | 내용 |
|------|------|
| `docs/` | 설계 기준 문서 |
| `.ai/` | AI 작업 가이드 |
| `.ai/AGENTS.md` | AI 진입점 |
