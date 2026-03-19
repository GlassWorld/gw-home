# CLAUDE.md

→ **`AGENTS.md` 를 먼저 읽어라.** 핵심 규칙과 context loading 정책이 모두 거기 있다.

---

## Claude Code 전용 참조

| 목적 | 파일 |
|------|------|
| 상세 규칙 | `.ai/CORE_RULES.md` |
| 작업 라우팅 | `.ai/TASK_ROUTER.md` |
| 스킬 목록 | `.ai/SKILL_INDEX.md` |
| 프로젝트 요약 | `.ai/project/summary.md` |

## Slash Commands

스킬은 `.claude/skills/{name}/SKILL.md` 에 있고 `/name` 으로 직접 호출한다.

```
/create-domain-structure {domain}
/create-service DOMAIN: board PURPOSE: 게시글 CRUD
/generate-ddl TABLE: tb_brd_pst DOMAIN: board COLUMNS: ...
/create-mapper DOMAIN: board TABLE: tb_brd_pst METHODS: ...
/create-api-endpoint DOMAIN: board ENDPOINT: GET /api/v1/boards PURPOSE: 목록 조회
/api-connect DOMAIN: board ENDPOINT: GET /api/v1/boards PAGING: true
/create-page DOMAIN: board PAGE: index AUTH: required
/create-component NAME: BoardListItem DOMAIN: board PURPOSE: 목록 아이템
```
