# SKILL INDEX

Reference paths:

- `.claude/skill/{name}/SKILL.md`
- `.ai/skill/{name}/SKILL.md`

Interpretation:

- Claude Code can use these as slash-command style skill documents
- Codex and other tools should treat them as local reference documents, not automatic execution units

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

## Input Format Example

```text
SKILL: create-api-endpoint
DOMAIN: board
ENDPOINT: GET /api/v1/boards
PURPOSE: list board posts
```

This format declares the reference guide to use. It is not an automatic skill invocation.
