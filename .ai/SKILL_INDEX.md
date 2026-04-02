# SKILL INDEX

This document is the single source of truth for skill interpretation and reference format.

Reference paths:

- `.claude/skill/{name}/SKILL.md`
- `.ai/skill/{name}/SKILL.md`

Interpretation:

- Claude Code can use these as slash-command style skill documents
- Codex and other tools should treat them as local reference documents, not automatic execution units
- If a skill document conflicts with code, API specs, or higher-priority rules, follow the source of truth

## Backend

| Skill | Description |
|-------|------|
| `create-domain-structure` | Guide for creating a new domain package structure |
| `create-service` | Guide for creating a service class |

## Database

| Skill | Description |
|-------|------|
| `generate-ddl` | Guide for writing DDL scripts |
| `create-mapper` | Guide for writing Mapper interfaces and XML |

## Frontend

| Skill | Description |
|-------|------|
| `apply-searchable-select` | Guide for reusable searchable select components |
| `create-page` | Guide for building a Nuxt page and composable |
| `create-component` | Guide for building a Vue component |

## Common

| Skill | Description |
|-------|------|
| `create-api-endpoint` | Guide for implementing a backend API endpoint |
| `api-connect` | Guide for frontend API types and composables |
| `review-doc-template` | Template reference for `work/review` documents |
| `todo-doc-template` | Template reference for `work/todo` documents |

## Work Document References

| Reference | Path | Purpose |
|-------|------|------|
| `review-doc-template` | `work/review/_template.md` | Review template reference |
| `todo-doc-template` | `work/todo/_template.md` | Todo template reference |

## Input Format Example

```text
SKILL: create-api-endpoint
DOMAIN: board
ENDPOINT: GET /api/v1/boards
PURPOSE: list board posts
```

This format declares the reference guide to use. It is not an automatic skill invocation.
