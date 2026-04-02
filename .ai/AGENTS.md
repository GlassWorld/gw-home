# AGENTS

Project: **gw-home** - Nuxt3 + Spring Boot community platform

## Entry Flow

1. Read this file first.
2. Read `.ai/CORE_RULES.md` for absolute rules.
3. If there is a related `work/review` or `work/todo` document, read it before implementation.
4. Read only the index or reference documents needed for the current task.
5. Read `.ai/project/summary.md` only when stack, module, or domain context is needed.

## Document Priority

System and developer instructions -> `.ai/CORE_RULES.md` -> task documents (`work/review`, `work/todo`) -> `.ai/index/routing.md` -> `.ai/index/work-docs.md` -> `.ai/index/references.md` -> `.ai/index/skills.md`

## Core Working Principles

- Treat `work/review` and `work/todo` as task support documents for the user's requested work, not as standalone goals.
- Confirm scope from the current todo document or work document before changing code or documents.
- Stop immediately and ask for approval if the task goes out of scope.
- Implement only what was requested. Do not add speculative extensions.
- Prefer editing existing files. Keep new file creation to a minimum.
- Do not scan the entire repository. Load only the files needed for the current task.
- Use `.ai/index/routing.md` for task routing and HEAVY flow.
- Use `.ai/index/work-docs.md` for review and todo document rules.
- Use `.ai/index/references.md` for task-specific context loading.
- Use `.ai/index/skills.md` for skill lookup and reference paths.

Out-of-scope response format:

```text
🚨 The task has gone out of scope. 🚨
- Change: {what changed}
- Likely reason: {reason}
Options: 1) extend the todo  2) split into a separate task  3) cancel the out-of-scope change
How would you like to proceed?
```

## Request Triggers

| Trigger | First Reference |
|--------|------|
| `##검토` | `.ai/index/routing.md`, `.ai/index/work-docs.md` |
| `##계획` | `.ai/index/routing.md`, `.ai/index/work-docs.md` |
| `##작업` | `.ai/index/routing.md`, `.ai/index/references.md`, `.ai/index/skills.md` |
| `##커밋` | `.ai/index/routing.md`, `.ai/index/work-docs.md` |

- `##검토` creates a review document for the requested task.
- `##계획` creates a todo or plan document for the requested task.

## Commit Rules

1. Inspect changes with `git diff --staged` and `git status`
2. Choose the right prefix: `fix.`, `feat.`, `refactor.`, `docs.`, `chore.`
3. Draft the commit message in Korean and ask for approval before committing
4. Clean up related work documents according to `.ai/index/work-docs.md`

## References

- Routing: `.ai/index/routing.md`
- Work documents: `.ai/index/work-docs.md`
- Context loading: `.ai/index/references.md`
- Skill lookup: `.ai/index/skills.md`
- Project summary: `.ai/project/summary.md`
