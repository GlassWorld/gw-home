# AGENTS.md

프로젝트: **gw-home** — Nuxt3 + Spring Boot 커뮤니티 플랫폼 (Front/Back 통합)

---

## 진입 절차

1. 이 파일의 절대 규칙을 먼저 읽는다
2. `.ai/project/summary.md` 로 전체 스택 파악
3. Task에 맞는 문서만 선택 로딩 — **전체 스캔 금지**

---

## Context Loading

| Task | 읽을 문서 |
|------|-----------|
| Backend API | `docs/backend/backend-rules.md`, `docs/common/architecture.md` |
| DB / DDL | `docs/backend/database.md`, `docs/backend/domain.md` |
| Frontend page | `docs/frontend/frontend-rules.md`, `docs/frontend/pages.md` |
| Frontend API 연동 | `docs/common/api-contract.md`, `docs/frontend/frontend-rules.md` |
| Auth (frontend) | `docs/frontend/auth-flow.md`, `docs/common/api-contract.md` |
| 전체 구조 | `docs/common/architecture.md`, `docs/common/project-structure.md` |

---

## 절대 규칙

### Backend

- **MyBatis only** — JPA (`@Entity`, `JpaRepository`), Querydsl 절대 금지
- **`_idx` API 응답 노출 금지** — 외부 식별자는 `uuid` 전용
- **TIMESTAMPTZ** — `TIMESTAMP` (without timezone) 금지
- Java 21 / Spring Boot 3.x
- 패키지: `com.gw.{module}.{layer}.{domain}`
- DDL/VO/JVO: 감사 컬럼 제외 축약형 네이밍 (`AcctVo`, `tb_mbr_acct`)
- 공통 PK/UUID/감사 컬럼: `BaseVo` 상속

### Frontend

- **축약어 금지** (`button` not `btn`, `user` not `usr`, `index` not `idx`)
- **`any` 타입 금지** — 명시적 TypeScript 타입 필수
- 파일명: kebab-case / 컴포넌트명: PascalCase / 함수명: camelCase
- 초기 라우트: `/login` → 로그인 성공 후 `/dashboard`
- API 응답: `ApiResponse<T>` 래핑 기준 (`docs/common/api-contract.md` 참조)

### 도메인 경계

- `account` / `auth` / `profile` 분리 — `auth`→`account` 참조 허용, 역방향 금지
- `board` / `admin` 분리
- `file` 독립 — 타 도메인은 URL만 저장, 게시글 본문 이미지도 URL

---

## 모듈 구조

```
gw-home-share/      공통 DTO, 예외, 유틸 (Backend 공유)
gw-home-api/        Controller, Service, DTO
gw-home-infra-db/   Mapper interface, Mapper XML, DDL
gw-home-ui/         Nuxt3 Frontend (pages, composables, stores, types)
```

---

## Skill 사용

스킬 파일 위치: `.claude/skills/{name}/SKILL.md`
스킬 목록: `.ai/SKILL_INDEX.md`

| Skill | 설명 |
|-------|------|
| `create-domain-structure` | 백엔드 도메인 구조 전체 생성 |
| `create-service` | Service 클래스 생성 |
| `generate-ddl` | DDL 스크립트 생성 |
| `create-mapper` | Mapper 인터페이스 + XML 생성 |
| `create-api-endpoint` | 백엔드 API 엔드포인트 전체 생성 |
| `api-connect` | 프론트 API 연동 타입 + composable |
| `create-page` | Nuxt3 페이지 생성 |
| `create-component` | Vue3 컴포넌트 생성 |

```
SKILL: create-api-endpoint
DOMAIN: board
ENDPOINT: GET /api/v1/boards
PURPOSE: 게시글 목록 조회 (페이징)
```

---

## 작업 분류 (TASK_ROUTER)

| 유형 | 조건 |
|------|------|
| SIMPLE | 단일 파일 변경 |
| NORMAL | 단일 도메인 내 복수 파일 |
| HEAVY | 스키마 변경, 도메인 간 영향, share 모듈 변경, Full-stack 동시 작업 |

HEAVY 작업 시 반드시: 영향 범위 먼저 출력 → 파일 목록 나열 → 구현 시작
