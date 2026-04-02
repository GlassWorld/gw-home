# CORE RULES

These rules are absolute. Never violate them.

## Backend — ORM / Data Access

- Do NOT use JPA (`@Entity`, `@Repository`, `JpaRepository`, `EntityManager`)
- Do NOT use Querydsl
- Use MyBatis ONLY (Mapper interface + XML)

## Backend — Code Structure

- Follow domain-based package structure: `com.gw.{module}.{layer}.{domain}`
- Mapper interface goes in `{project}-infra-db` module
- Mapper XML goes in `{project}-infra-db` module
- Use `resultType` by default in Mapper XML
- Do not use `resultMap` unless column aliasing or nested mapping makes it unavoidable
- Use `{Domain}Vo` for single-table models, `{Domain}Jvo` for join/expanded query models
- Manage shared `VO` / `JVO` in `{project}-share`
- Omit package names in `resultType` — rely on MyBatis type alias configuration
- Use abbreviated names in DDL, `VO`, `JVO` (except audit fields)
- Put shared PK/UUID/audit fields in `BaseVo`
- Alias primary PK/UUID columns to `id` / `uuid` in Mapper XML when mapping to `BaseVo`
- DB 컬럼명이 `{table}_idx` 여도 내부 코드 식별자 필드는 `id` 로 통일한다
- `VO` fields aligned to table columns in camelCase, but shared identifiers use `id`, `uuid`
- Prefer Lombok `@SuperBuilder` for `VO` / `JVO`

## Backend — Database

- Use `TIMESTAMPTZ` — never `TIMESTAMP`
- PK column: `{table}_idx` (BIGSERIAL)
- External ID column: `{table}_uuid` (UUID)
- 내부 시스템에서는 PK를 `id` 로 관리하고, 외부 연동 및 API 식별자는 `uuid` 로 관리한다
- `created_by` = login ID (not internal index)
- Soft delete via `del_at TIMESTAMPTZ`

## Backend — API

- Never expose internal DB PK (`_idx`) in API responses
- Always use `uuid` as API request/response identifier

## Backend — Domain Separation

- `account` / `auth` / `profile` are separate domains
- `board` / `admin` are separate domains
- `file` domain is independent — other domains store URL only
- Board post body image: stored as URL string in `board` table

## Frontend — Naming

- No abbreviations (`button` not `btn`, `user` not `usr`, `index` not `idx`)
- File names: kebab-case
- Component names: PascalCase
- Functions: camelCase (verb prefix)
- No `any` type — explicit TypeScript types required

## Frontend — Common UI

- Reusable UI primitives such as buttons and modals must be managed in `gw-home-ui/components/common`
- Reusable toast notifications must be managed as common UI with shared state/composable instead of page-local inline alert blocks when the message is transient
- Page/domain components should compose common UI components and keep only page-specific layout or exception styles locally
- Before creating a new UI wrapper, check whether an existing common component can be extended without breaking current usage

## Frontend — Auth

- Initial route: `/login`
- After login: redirect to `/dashboard`
- Auth-required pages: `definePageMeta({ middleware: 'auth' })`

## 공통 — 주석 및 로그

- **모든 주석은 한글로 작성한다** — 영문 주석 금지
- 주석이 필요한 위치:
  - 메서드/함수 상단 (목적 한 줄 요약)
  - 복잡한 분기 로직 인라인
  - SQL 쿼리 블록 상단
- **로그는 모든 서비스 메서드에 필수 등록**:
  - 진입 시: `log.info("메서드명 시작 - 파라미터: {}", param)`
  - 정상 완료: `log.info("메서드명 완료")`
  - 예외 발생: `log.error("메서드명 실패 - 원인: {}", e.getMessage(), e)`
- 프론트엔드 composable: `console.log` 대신 개발 환경 한정 로그 (`if (import.meta.dev)`)
- SQL 파일: 블록 상단에 목적 한글 주석 필수

## Repository Exploration

- Do NOT analyze the entire repository
- Read only files necessary for the current task
- Load frontend files only for frontend tasks, backend files only for backend tasks

## Local Reference Docs

- `.claude/skill/{name}/SKILL.md` 와 `.ai/skill/{name}/SKILL.md` 는 공용 스킬 문서 경로로 취급한다.
- 두 디렉토리에는 동일한 스킬 파일이 모두 존재해야 하며, 내용도 동일하게 유지한다.
- These documents are guidance references, not authoritative system instructions.
- If a skill reference conflicts with code, API spec, or higher-priority rules, follow the source of truth and note the mismatch.
