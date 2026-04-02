# WORK DOCS INDEX

This document is the single source of truth for review and todo document rules.

## Document Lifecycle

- Review document: `work/review/{domain}/review-{subject}.md`
- Todo document: `work/todo/{domain}/todo-{subject}.md`
- Reuse existing documents when the topic matches
- Completed documents are not moved automatically
- Clean up related documents during `##커밋`

Naming examples:

- `work/review/auth/review-login-policy.md`
- `work/todo/board/todo-board-search.md`

## Output Rules

- `##검토` output: Scope, Summary, Findings or scores, Recommended next actions
- `##계획` output: Goal, Scope, Current state, Action items, Done criteria
- `##작업` may update code and documents, but should keep work documents aligned with the current scope

## Template References

- Review template: `work/review/_template.md`
- Todo template: `work/todo/_template.md`

## Commit Cleanup

- During `##커밋`, inspect related review and todo documents
- Keep documents that are still active
- Clean up documents that are complete and clearly tied to the committed scope
