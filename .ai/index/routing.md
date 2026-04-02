# ROUTING INDEX

This document is the single source of truth for task routing and HEAVY flow.

## Request Triggers

| Trigger | Action |
|--------|------|
| `##검토` | Write or update a review document under `work/review/` |
| `##계획` | Write or update a todo document under `work/todo/` |
| `##작업` | Classify the task, load only the needed references, and execute |
| `##커밋` | Review changes and draft the commit message |

## Layer Classification

Classify each task first as `Backend`, `Frontend`, `Full-stack`, or `Doc-ops`.

| Layer | Trigger |
|-------|---------|
| Backend | Controller, Service, Mapper, VO, JVO, DDL, DB |
| Frontend | Page, Component, Composable, Store, Type, UI |
| Full-stack | API integration plus UI work, or endpoint and screen changes together |
| Doc-ops | AI docs, work docs, templates, skills, review or planning system changes |

## Complexity Classification

| Level | Criteria |
|------|------|
| SIMPLE | Single-file change, single layer, no shared rule impact |
| NORMAL | Multiple files inside one area or one document group |
| HEAVY | Shared rule change, index structure change, schema change, cross-domain impact, auth or security change, or full-stack work |

## SIMPLE

- Examples: fix a typo, correct one reference path, update one template field
- Action: inspect the relevant file and implement directly

## NORMAL

- Examples: update one document group, add one index, revise one workflow
- Action:
  1. Read only the relevant files
  2. Select the needed references or skills
  3. Verify compliance with `.ai/CORE_RULES.md`

## HEAVY

- Examples: restructure `.ai` indexes, change shared workflow rules, add a new shared task mode, schema migration, full-stack feature
- Action:
  1. Do not implement immediately
  2. Describe the impact scope first
  3. List files to create or modify
  4. Create or update the review document
  5. Create or update the todo document if implementation work will follow
  6. Wait for user approval before implementation

Warning format:

```text
⚠️ This task is classified as HEAVY.
```

Approval request format:

```text
Review and impact analysis are complete. Would you like to continue?
```

## Routing Checklist

- Does it change DB schema -> `HEAVY`
- Does it affect more than one domain -> `HEAVY`
- Does it change shared modules or shared rules -> `HEAVY`
- Does it require both backend and frontend changes -> `HEAVY`
- Does it change `.ai` entry or index structure -> `HEAVY`
- Does it modify multiple files in one area -> `NORMAL`
- Does it modify only one file -> `SIMPLE`

## Skill Selection

After classification, select the needed skill or reference from `.ai/index/skills.md`.

```text
LAYER: backend | frontend | common | doc-ops
SKILL: {skill-name}
DOMAIN: {domain}
INPUT: {task}
```
