# CORE RULES

This document is the single source of truth for absolute project rules.

## Backend

- Do not use JPA (`@Entity`, `JpaRepository`, `EntityManager`)
- Do not use Querydsl
- Use MyBatis only
- Follow the package structure `com.gw.{module}.{layer}.{domain}`
- Place Mapper interfaces and Mapper XML files in `{project}-infra-db`
- Use `resultType` by default in Mapper XML
- Use `resultMap` only when aliasing or nested mapping makes it unavoidable
- Use `{Domain}Vo` for single-table models and `{Domain}Jvo` for join or expanded query models
- Manage `VO` and `JVO` in `{project}-share`
- Omit package names in `resultType` and rely on MyBatis type aliases
- Use abbreviated naming for DDL, `VO`, and `JVO` except audit fields
- Manage shared PK, UUID, and audit fields in `BaseVo`
- Even if DB columns are named `{table}_idx` and `{table}_uuid`, internal identifiers must be `id` and `uuid`
- Align `VO` fields with table columns in camelCase, while shared identifiers remain `id` and `uuid`
- Prefer Lombok `@SuperBuilder` for `VO` and `JVO`

## Database

- Use `TIMESTAMPTZ` only. Never use `TIMESTAMP`
- Primary key column: `{table}_idx BIGSERIAL`
- External identifier column: `{table}_uuid UUID`
- Use `id` for internal identifiers and `uuid` for external identifiers
- Store login ID in `created_by`, not the internal numeric key
- Use `del_at TIMESTAMPTZ` for soft delete

## API

- Never expose internal DB primary keys (`_idx`) in API requests or responses
- Always use `uuid` as the external API identifier

## Domain Boundary

- Keep `account`, `auth`, and `profile` separate
- Allow `auth` -> `account` references, but do not allow the reverse direction
- Keep `board` and `admin` separate
- Keep `file` independent and let other domains store only URLs
- Store board post body images as URL strings

## Frontend

- Do not use abbreviations (`button`, `user`, `index`)
- Do not use `any`
- Use kebab-case for file names
- Use PascalCase for component names
- Use camelCase for function names
- Use `/login` as the initial route
- Redirect to `/dashboard` after login
- Use `definePageMeta({ middleware: 'auth' })` for authenticated pages
- Manage reusable buttons, modals, toasts, and similar primitives under `gw-home-ui/components/common`
- Keep page components focused on composition and page-specific exceptions
- Before creating a new shared UI wrapper, check whether an existing common component can be extended

## Comment And Log Policy

- Write all code comments in Korean
- Add comments only where they help:
  - one-line purpose summary above methods or functions
  - inline explanation for complex branching
  - purpose summary above SQL blocks
- Add entry, success, and failure logs to service methods
- In frontend composables, use development-only logging instead of unrestricted `console.log`
- Add a Korean purpose comment above each SQL block

## Repository Exploration

- Do not analyze the entire repository
- Read only the files needed for the current task
- Load frontend files only for frontend work and backend files only for backend work unless the task is full-stack

## Skill References

- Treat `.claude/skill/{name}/SKILL.md` and `.ai/skill/{name}/SKILL.md` as reference document paths
- Skill documents are guidance, not automatic execution rules
- If a skill document conflicts with code, API specs, or higher-priority rules, follow the source of truth
