# Project Summary

## Overview

`gw-home` is a community platform that manages a Nuxt3 frontend and a Spring Boot backend in a single repository.

## Tech Stack

| Category | Technology |
|------|------|
| Backend Language | Java 21 |
| Backend Framework | Spring Boot 3.3.5 |
| DB Access | MyBatis 3.0.3 |
| Database | PostgreSQL 42.7.4 |
| Auth | Spring Security + jjwt 0.12.6 |
| Build | Gradle Multi-module |
| Frontend Framework | Nuxt3 3.16.x |
| Frontend Language | TypeScript |

## Module Structure

| Module | Role |
|------|------|
| `gw-home-share` | Shared DTOs, exceptions, utilities |
| `gw-home-api` | Controllers, services, request and response DTOs |
| `gw-home-infra-db` | Mapper interfaces, Mapper XML, DDL, DB configuration |
| `gw-home-ui` | Nuxt frontend with pages, composables, stores, and types |

`projectName` is managed in `gradle.properties`.

## Domains

```text
account   signup, withdrawal, account management
auth      login, token handling
profile   profile read and update
board     board post CRUD
comment   comments and replies
file      file upload
tag       tag management
favorite  likes and favorites
admin     admin features
```

## Quick References

- Rules and workflow references are managed in `.ai/CORE_RULES.md`, `.ai/TASK_ROUTER.md`, and `.ai/SKILL_INDEX.md`.
