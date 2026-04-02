# TASK ROUTER

This file is kept for backward compatibility.

Use these documents instead:

- Task routing and HEAVY flow: `.ai/index/routing.md`
- Work document rules: `.ai/index/work-docs.md`
- Task-specific references: `.ai/index/references.md`
- Skill lookup: `.ai/index/skills.md`

## DB Change Checklist

If the task includes DB changes, include this checklist in the related task document.

```text
- [ ] Write DDL change SQL
- [ ] Review existing data impact
- [ ] Confirm NULL / DEFAULT policy
- [ ] Check index impact
- [ ] Prepare rollback SQL
- [ ] Define deployment order
- [ ] Confirm whether data migration is required
```
