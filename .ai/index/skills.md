# SKILLS INDEX

This document is the single source of truth for skill lookup, interpretation, and reference paths.

## Reference Paths

- Primary path: `.claude/skills/{name}/SKILL.md`
- Optional local path: `.ai/skills/{name}/SKILL.md`

## Interpretation

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

## Doc-ops Candidates

| Candidate | Purpose |
|-------|------|
| `work-review-writer` | Structure `##검토` output from findings and scores |
| `work-todo-writer` | Structure `##계획` output from scope and execution steps |
| `doc-setting-review` | Review AI document structure, token efficiency, and duplication |
| `task-classifier` | Classify layer, complexity, and next reference path |

## Input Format Example

```text
LAYER: backend
SKILL: create-api-endpoint
DOMAIN: board
INPUT: GET /api/v1/boards list endpoint
```

This format declares the reference guide to use. It is not an automatic skill invocation.
