# TASK ROUTER

This document is the single source of truth for task routing, HEAVY flow, and work document rules.

## Layer Classification

Classify each task first as `Backend`, `Frontend`, or `Full-stack`.

| Layer | Trigger |
|-------|---------|
| Backend | Controller, Service, Mapper, VO, JVO, DDL, DB |
| Frontend | Page, Component, Composable, Store, Type, UI |
| Full-stack | API integration plus UI work, or endpoint and screen changes together |

## Complexity Classification

| Level | Criteria |
|------|------|
| SIMPLE | Single-file change, single layer, no cross-domain impact |
| NORMAL | Multiple files inside one domain |
| HEAVY | Schema change, shared module change, auth or security change, cross-domain impact, or full-stack work |

## SIMPLE

- Examples: add a DTO field, fix a query typo, update a type definition
- Action: inspect the relevant file and implement directly

## NORMAL

- Examples: add an API endpoint, add a Nuxt page with a composable, extend one feature inside one domain
- Action:
  1. Read only relevant domain files
  2. Select the needed reference documents or skill documents
  3. Verify compliance with `.ai/CORE_RULES.md`

## HEAVY

- Examples: schema migration, `share` module change, auth structure change, new domain, full-stack feature
- Action:
  1. Do not implement immediately
  2. Describe the impact scope first
  3. List files to create or modify
  4. Create or update the review document
  5. Wait for user approval before implementation

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
- Does it change `share` or another shared module -> `HEAVY`
- Does it require both backend and frontend changes -> `HEAVY`
- Does it modify multiple files in one domain -> `NORMAL`
- Does it modify only one file -> `SIMPLE`

## Skill Selection

After classification, select the needed skill or reference document from `.ai/SKILL_INDEX.md`.

```text
LAYER: backend | frontend | common
SKILL: {skill-name}
DOMAIN: {domain}
INPUT: {task}
```

## Document Lifecycle

- Review document: `work/review/{domain}/review-{subject}.md`
- Todo document: `work/todo/{domain}/todo-{subject}.md`
- Reuse existing documents when the topic matches
- Completed documents are not moved automatically
- Clean up related documents during `##커밋`

Naming examples:

- `work/review/auth/review-login-policy.md`
- `work/todo/board/todo-board-search.md`

## Work Document Rules

- `##검토` output: Scope, Summary, Findings or scores, Recommended next actions
- `##계획` output: Goal, Scope, Current state, Action items, Done criteria
- `##작업` may update code and documents, but should follow this file for work document handling

Template references:

- Review template: `work/review/_template.md`
- Todo template: `work/todo/_template.md`

## DB Change Checklist

If the task includes DB changes, include this checklist in the task document.

```text
- [ ] Write DDL change SQL
- [ ] Review existing data impact
- [ ] Confirm NULL / DEFAULT policy
- [ ] Check index impact
- [ ] Prepare rollback SQL
- [ ] Define deployment order
- [ ] Confirm whether data migration is required
```
