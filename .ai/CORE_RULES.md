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
- Keep service methods focused on business flow and validation
- Move `toResponse`, `toVo`, and similar conversions out of services into `convert` classes by default
- Reuse `gw-home-share` utilities for null checks, simple validation, and simple type conversions when possible
- Place reusable technical utilities and security helpers in `gw-home-share` rather than `gw-home-api/util`
- Add a one-line Korean role comment above controller and service public methods

## Backend Service And Controller Policy

- Keep controllers focused on HTTP request handling and service delegation
- Keep services focused on business flow, validation, persistence orchestration, and transaction boundaries
- Move `toResponse`, `toVo`, and similar conversion methods out of services into `com.gw.api.convert.{domain}` by default
- Prefer `gw-home-share` for reusable null checks, simple validation, simple type conversions, and shared policy constants
- Prefer `ErrorCode` default messages for common failures; add detailed service messages only when the default is not sufficient
- Review service-level constants for promotion to shared policy when they are reused across services or domains
- Add one-line Korean role comments above controller `public` methods describing the request purpose
- Add one-line Korean role comments above service `public` methods describing the business purpose
- Keep controller comments request-oriented and service comments business-oriented
- Write entry, success, and failure logs in service `public` methods
- Structure service methods in this order when practical: log start -> load actor or prerequisite -> validate and normalize -> assemble model -> call mapper or integration -> enrich or reload -> convert -> log success -> return -> log and rethrow on failure
- Treat reusable technical helpers under `gw-home-api/util` as candidates for `gw-home-share`
- Do not create new shared package names such as `share.model` without first aligning them to the existing `gw-home-share` package structure

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
- Use kebab-case for route-oriented files and general TypeScript files
- Preserve the established naming convention inside existing component directories
- Use PascalCase for component names
- Use camelCase for function names
- Use `/login` as the initial route
- Redirect to `/dashboard` after login
- Use `definePageMeta({ middleware: 'auth' })` for authenticated pages
- Manage reusable buttons, modals, toasts, and similar primitives under `gw-home-ui/components/common`
- Keep page components focused on composition and page-specific exceptions
- When a page grows beyond 300 lines, review whether page-local logic, UI blocks, and types should be split before adding more code
- Move repeated page logic to `gw-home-ui/utils` or `gw-home-ui/features/{domain}/composables` when it is shared by more than one screen
- Move page-local interfaces, form state types, and reusable constants out of pages into `gw-home-ui/features/{domain}/types` or `gw-home-ui/types`
- Before creating a new shared UI wrapper, check whether an existing common component can be extended
- Shared API entry points should remain discoverable from `gw-home-ui/composables`, but feature-local API and type modules may live under `gw-home-ui/features/{domain}` when they are only used inside that feature
- Prefer file-based SVG assets or dedicated icon components over inline SVG when the graphic is reused or likely to be reused
- Promote repeated layout and panel styles to shared styles under `gw-home-ui/assets/styles`, and keep scoped page CSS focused on screen-specific presentation

## Comment And Log Policy

- Write all code comments in Korean
- Add comments only where they help:
  - one-line purpose summary above methods or functions
  - inline explanation for complex branching
  - purpose summary above SQL blocks
- Add entry, success, and failure logs to service methods
- Keep controller comments focused on request purpose and service comments focused on business purpose
- In frontend composables, use development-only logging instead of unrestricted `console.log`
- Add a Korean purpose comment above each SQL block

## Repository Exploration

- Do not analyze the entire repository
- Read only the files needed for the current task
- Load frontend files only for frontend work and backend files only for backend work unless the task is full-stack
