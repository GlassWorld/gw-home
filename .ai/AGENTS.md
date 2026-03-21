# AGENTS

프로젝트: **gw-home** — Nuxt3 + Spring Boot 커뮤니티 플랫폼 (Front/Back 통합)

---

## 진입 절차

1. 이 파일의 기본 작업 원칙을 먼저 읽는다
2. `.ai/project/summary.md` 로 전체 스택 파악
3. Task에 맞는 문서만 선택 로딩 — **전체 스캔 금지**

---

## ⚠️ 기본 작업 원칙 — 반드시 준수

> 아래 원칙은 모든 작업에 예외 없이 적용된다.

### 1. 작업 범위 이탈 금지

- 작업 전 **todo 문서** 또는 **work 문서의 작업 범위 섹션**을 기준으로 범위를 확정한다.
- 다음 중 하나라도 해당하면 **즉시 작업 중단 → 사용자 승인 요청**:
  - 다른 도메인/모듈 수정
  - work 문서에 명시되지 않은 파일 변경
  - todo에 없는 기능 추가
  - 명시되지 않은 API 스펙 / DB 구조 변경
  - `config`, `shared`, `util` 등 공통 모듈 수정
- 범위 이탈 감지 시 출력:
  ```
  🚨 작업 범위를 벗어났습니다.🚨
  - 이탈 내용: {변경 설명}
  - 이유(추정): {이유}
  선택지: 1) todo 확장 후 진행  2) 별도 작업으로 분리  3) 이탈 작업 취소
  어떻게 진행할까요?
  ```
- **승인 전까지 해당 변경 절대 진행 금지**

### 2. DB / 공통 모듈 / 인증·보안 변경 → HEAVY 자동 재분류

- 위 항목이 범위 이탈로 확인되면 HEAVY로 재분류하고:
  - 영향 범위 먼저 출력
  - 파일 목록 나열
  - 승인 대기 후 구현 시작

### 3. 과잉 구현 금지

- 요청된 것만 구현한다. 미래 확장성 가정 금지.
- 주석, docstring, 타입 어노테이션은 변경한 코드에만 추가.
- 에러 핸들링은 시스템 경계(사용자 입력, 외부 API)에서만.

### 4. 파일 생성 최소화

- 기존 파일 수정을 우선한다. 반드시 필요한 경우에만 신규 파일 생성.

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

## 작업 분류

| 유형 | 조건 |
|------|------|
| SIMPLE | 단일 파일 변경 |
| NORMAL | 단일 도메인 내 복수 파일 |
| HEAVY | 스키마 변경, 도메인 간 영향, share 모듈 변경, Full-stack 동시 작업 |

HEAVY 작업 시 반드시: 영향 범위 먼저 출력 → 파일 목록 나열 → 구현 시작

---

## Skill 사용

스킬 파일 위치: `.claude/skills/{name}/SKILL.md`

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
/create-domain-structure {domain}
/create-service DOMAIN: board PURPOSE: 게시글 CRUD
/generate-ddl TABLE: tb_brd_pst DOMAIN: board COLUMNS: ...
/create-mapper DOMAIN: board TABLE: tb_brd_pst METHODS: ...
/create-api-endpoint DOMAIN: board ENDPOINT: GET /api/v1/boards PURPOSE: 목록 조회
/api-connect DOMAIN: board ENDPOINT: GET /api/v1/boards PAGING: true
/create-page DOMAIN: board PAGE: index AUTH: required
/create-component NAME: BoardListItem DOMAIN: board PURPOSE: 목록 아이템
```

---

## 문서 구조

```
.ai/
├── AGENTS.md           진입점 — 기본 작업 원칙, 절대 규칙, Context Loading
├── CORE_RULES.md       절대 규칙 (상세)
├── TASK_ROUTER.md      작업 분류 및 실행 절차
├── SKILL_INDEX.md      스킬 목록
└── project/
    └── summary.md      프로젝트 스택 요약
```

충돌 시 우선순위: 시스템/개발자 지침 → `.ai/CORE_RULES.md` → 작업 관련 문서 → `.claude/skills/*` 순서

---

## 작업 요청 시작어 규칙

### 트리거 인식

| 트리거 | 동작 |
|--------|------|
| `**리뷰` | review 문서 생성/업데이트 → "투두 생성할까요?" 질문 |
| `**투두` | todo 문서 생성/업데이트 |
| `**작업` | 아래 작업 흐름 실행 |

> `**리뷰`, `**투두` 요청 시 코드 변경 금지. `**작업` 요청 시만 문서 기반으로 코드 수정.

---

### 작업 레벨 자동 분류

모든 `**작업` 요청은 실행 전 반드시 작업 레벨을 분류한다.

| 레벨 | 조건 |
|------|------|
| LIGHT | 단일 파일 수정, UI 텍스트 변경, 간단한 로직 수정 |
| NORMAL | 기능 단위 작업, API 추가/수정, 화면 + API 연동 |
| HEAVY | 여러 도메인 영향 / 공통 모듈 변경 / DB 스키마 변경 / 인증·보안 구조 변경 / API 스펙 변경 / 대규모 리팩토링 / 외부 시스템 연동 구조 변경 |

---

### LIGHT / NORMAL 처리

간단한 작업 요약 후 즉시 진행.

```
NORMAL 작업으로 분류되었습니다. 진행합니다.
```

---

### HEAVY 처리 절차

HEAVY로 분류되면 즉시 진행하지 않는다.

**Step 0 — review 문서 강제 생성**
- `review/` 문서를 먼저 생성 또는 업데이트
- 포함 내용: 작업 개요 / 변경 목적 / 예상 영향 범위 / 리스크 분석 / 대안
- **리뷰 없이 작업 진행 금지**

**Step 1 — 경고 출력**
```
⚠️ 이 작업은 HEAVY 작업입니다.
```

**Step 2 — 영향 범위 분석**
- 영향 받는 모듈 / 변경 레이어 / 영향 받는 API / 프론트 영향 여부 / 데이터 영향 여부

**Step 3 — 작업 계획**
- 단계별 실행 계획 / 롤백 가능 여부 / 테스트 필요 항목

**Step 4 — 사용자 승인**
```
리뷰 및 영향 분석이 완료되었습니다. 계속 진행할까요?
```
→ **승인 전 작업 금지**

---

### DB 변경 추가 규칙

다음 중 하나라도 해당하면 DB 변경으로 간주:
- CREATE / ALTER / DROP
- 컬럼 추가/삭제/변경
- 인덱스/제약조건 변경

work 문서 또는 todo 문서에 마이그레이션 체크리스트 반드시 포함:

```
- [ ] DDL 변경 SQL 작성
- [ ] 기존 데이터 영향 검토
- [ ] NULL / DEFAULT 정책 확인
- [ ] 인덱스 영향 확인
- [ ] 롤백 SQL 작성
- [ ] 운영 반영 순서 정의
- [ ] 데이터 마이그레이션 필요 여부 확인
```

---

### `**작업` 흐름

1. todo 확인
2. 없으면 자동 생성
3. 작업 레벨 분류
4. LIGHT/NORMAL → 진행 / HEAVY → 리뷰 강제 생성 → 승인 대기
5. work 문서 생성/업데이트 후 작업 진행

---

### 문서 관리 규칙

- **재사용 우선**: 동일 주제 → update / 일부 연관 → append / 신규 → 생성
- **중복 생성 금지**
- **문서 연결 유지**: review → todo → work 흐름. 각 문서에 관련 문서 참조 포함.
