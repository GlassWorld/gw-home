# AGENTS

Initialize this repository.

1. Read `.ai/CORE_RULES.md`
2. Load `.ai/project/summary.md`
3. Follow `.ai/TASK_ROUTER.md`
4. Use `.ai/SKILL_INDEX.md`

---

## Context Loading Policy

- Do NOT scan the entire repository
- Load only files relevant to the current task domain
- When working on `board`: read only `board`-related files
- Check `docs/rules.md` before generating any code
- Respect Gradle module naming: `{project}-share`, `{project}-api`, `{project}-infra-db`
