# AGENTS

Project: **gw-home** - Nuxt3 + Spring Boot community platform

## Entry Flow

1. Read this file first for workflow and entry rules.
2. Read `.ai/project/summary.md` for stack and module overview.
3. Load only the documents needed for the current task. Do not scan the entire repository.

## Document Roles

| Document | Role |
|------|------|
| `.ai/AGENTS.md` | Entry point, document priority, trigger rules |
| `.ai/CORE_RULES.md` | Single source of truth for absolute rules |
| `.ai/TASK_ROUTER.md` | Task classification and execution flow |
| `.ai/SKILL_INDEX.md` | Skill list and reference format |
| `.ai/project/summary.md` | Stack, modules, and domain overview |

## Document Priority

System and developer instructions -> `.ai/CORE_RULES.md` -> task documents (`work/review`, `work/todo`) -> `.ai/TASK_ROUTER.md` -> `.ai/SKILL_INDEX.md`

## Core Working Principles

- Confirm scope from the `todo` document or the scope section in the current `work` document.
- Stop immediately and ask for approval if the task goes out of scope.
- Implement only what was requested. Do not add speculative extensions.
- Prefer editing existing files. Keep new file creation to a minimum.
- Follow `.ai/CORE_RULES.md` for all absolute rules.
- Use `.ai/TASK_ROUTER.md` as the source of truth for task routing, HEAVY flow, and work document rules.
- Use `.ai/SKILL_INDEX.md` as the source of truth for skill interpretation and reference format.

Out-of-scope response format:

```text
🚨 The task has gone out of scope. 🚨
- Change: {what changed}
- Likely reason: {reason}
Options: 1) extend the todo  2) split into a separate task  3) cancel the out-of-scope change
How would you like to proceed?
```

## Context Loading

| Task | Documents to Read |
|------|-----------|
| Backend API | `docs/backend/backend-rules.md`, `docs/common/architecture.md` |
| DB / DDL | `docs/backend/database.md`, `docs/backend/domain.md` |
| Frontend page | `docs/frontend/frontend-rules.md`, `docs/frontend/pages.md` |
| Frontend API integration | `docs/common/api-contract.md`, `docs/frontend/frontend-rules.md` |
| Auth (frontend) | `docs/frontend/auth-flow.md`, `docs/common/api-contract.md` |
| Overall structure | `docs/common/architecture.md`, `docs/common/project-structure.md` |

## Request Triggers

| Trigger | Action |
|--------|------|
| `##검토` | Write a review document under `work/review/` |
| `##계획` | Write a todo document under `work/todo/` |
| `##작업` | Route the task with `.ai/TASK_ROUTER.md` and execute |
| `##커밋` | Review changes and draft the commit message |

- Use `.ai/TASK_ROUTER.md` for detailed work document rules and lifecycle.

## Commit Rules

1. Inspect changes with `git diff --staged` and `git status`
2. Choose the right prefix: `fix.`, `feat.`, `refactor.`, `docs.`, `chore.`
3. Draft the commit message in Korean and ask for approval before committing
4. Clean up related work documents according to `.ai/TASK_ROUTER.md`

## References

- Routing and work documents: `.ai/TASK_ROUTER.md`
- Skill references and interpretation: `.ai/SKILL_INDEX.md`
