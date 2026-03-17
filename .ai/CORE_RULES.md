# CORE RULES

These rules are absolute. Never violate them.

## ORM / Data Access

- Do NOT use JPA (`@Entity`, `@Repository`, `JpaRepository`, `EntityManager`)
- Do NOT use Querydsl
- Use MyBatis ONLY (Mapper interface + XML)

## Repository Exploration

- Do NOT analyze the entire repository
- Read only files necessary for the current task
- Load domain files only when the task targets that domain

## Code Structure

- Follow domain-based package structure: `com.gw.{module}.{layer}.{domain}`
- Mapper interface goes in `{project}-infra-db` module
- Mapper XML goes in `{project}-infra-db` module
- Use `resultType` by default in Mapper XML
- Use `{Domain}Vo` for single-table models and `{Domain}Jvo` for join/expanded query models
- Manage shared `VO` / `JVO` classes in `{project}-share`
- Omit package names in `resultType` by relying on MyBatis type alias configuration
- Use abbreviated names across DDL, `VO`, and `JVO`, except audit fields
- Put shared PK/UUID/audit fields in `BaseVo`
- Alias primary table PK/UUID columns to `idx` / `uuid` in Mapper XML when mapping to `BaseVo`
- Keep `VO` fields aligned to table columns in camelCase, including names like `mbrAcctIdx`, `createdAt`
- Prefer Lombok `@SuperBuilder` for `VO` / `JVO` boilerplate and keep DB column comments as field comments when practical

## Database

- Use `TIMESTAMPTZ` — never `TIMESTAMP`
- PK column: `{table}_idx` (BIGSERIAL)
- External ID column: `{table}_uuid` (UUID) — only this is exposed in API
- `created_by` = login ID (not internal index)
- Soft delete via `del_at TIMESTAMPTZ`

## API

- Never expose `_idx` in API responses
- Always use `_uuid` as external identifier

## Frontend

- No abbreviations (use full names: `button` not `btn`, `user` not `usr`)
- File names: kebab-case
- Component names: PascalCase
