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
| `##검토` | Create or update `work/review/{domain}/review-{subject}.md` |
| `##계획` | Create or update `work/todo/{domain}/todo-{subject}.md` |
| `##작업` | Classify the task using `.ai/TASK_ROUTER.md` and execute |
| `##커밋` | Review changes and propose a commit message draft |

- Do not change code for `##검토` or `##계획`
- Change code only for `##작업`

## Work Document Rules

- Active work documents live under `work/review/` and `work/todo/`
- Reuse an existing document when the topic matches
- Completed documents are not moved automatically
- Clean up related work documents during `##커밋`

## Commit Rules

1. Inspect changes with `git diff --staged` and `git status`
2. Choose the right prefix: `fix.`, `feat.`, `refactor.`, `docs.`, `chore.`
3. Draft the commit message in Korean and ask for approval before committing
4. Clean up directly related `work/review` and `work/todo` documents together

## References

- Use `.ai/TASK_ROUTER.md` for task levels and HEAVY flow
- Use `.ai/SKILL_INDEX.md` for skill references
