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

- Follow domain-based package structure: `com.gw.{module}.{domain}.{layer}`
- Mapper interface goes in `api` module
- Mapper XML goes in `infra-db` module

## Database

- Use `TIMESTAMPTZ` — never `TIMESTAMP`
- PK column: `{table}_idx` (BIGSERIAL)
- External ID column: `{table}_uuid` (UUID) — only this is exposed in API
- `created_by` = login ID (not internal index)
- Soft delete via `deleted_at TIMESTAMPTZ`

## API

- Never expose `_idx` in API responses
- Always use `_uuid` as external identifier

## Frontend

- No abbreviations (use full names: `button` not `btn`, `user` not `usr`)
- File names: kebab-case
- Component names: PascalCase
