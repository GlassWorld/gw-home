# TASK ROUTER

## Layer Classification

작업 요청을 받으면 먼저 **Backend** / **Frontend** / **Full-stack** 중 하나로 분류한다.

| Layer | Trigger |
|-------|---------|
| Backend | Controller, Service, Mapper, VO, JVO, DDL, DB |
| Frontend | Page, Component, Composable, Store, Type, UI |
| Full-stack | API 연계, 엔드포인트 신규 + UI 화면 동시 작업 |

---

## Complexity Classification

### SIMPLE
Single file change. No cross-domain impact.

Examples:
- Add a field to a DTO
- Fix a typo in a query
- Add a single Mapper method + XML
- Add a TypeScript type

Action: Implement directly. No analysis needed.

---

### NORMAL
Multiple files in one domain, one layer.

Examples:
- Add a new API endpoint (Controller + Service + Mapper + XML)
- Create a new Nuxt3 page + composable
- Add pagination to an existing query

Action:
1. Read relevant domain files
2. Implement following skill template or matching local reference doc
3. Verify rule compliance (`CORE_RULES.md`)

---

### HEAVY
Cross-domain changes, schema changes, or architectural impact.

Examples:
- New domain from scratch (DDL + backend + frontend)
- Schema migration affecting multiple tables
- Shared module changes in `share`
- Auth/security changes
- Full-stack feature (API + UI 동시 작업)

Action:
1. **WARN**: Output affected scope before starting
2. List all files that will be created or modified
3. Confirm domain boundaries are respected
4. Backend order: DDL → Mapper → Service → Controller
5. Frontend order: type → composable → store → page/component
6. Verify rule compliance after each layer

---

## Routing Checklist

- [ ] Does this change a DB schema? → HEAVY
- [ ] Does this affect more than one domain? → HEAVY
- [ ] Does this change `share` module? → HEAVY
- [ ] Does this require both backend + frontend changes? → HEAVY (Full-stack)
- [ ] Does this add one endpoint in one domain? → NORMAL
- [ ] Does this add one page in one domain? → NORMAL
- [ ] Does this change a single file? → SIMPLE

## Skill Selection

작업 분류 후 `.ai/SKILL_INDEX.md` 에서 해당 skill 또는 참조 문서를 선택한다.

```
LAYER: backend | frontend | common
SKILL: {skill-name}
DOMAIN: {도메인명}
INPUT: {작업 내용}
```

참고:
- `.claude/skill/{name}/SKILL.md` 와 `.ai/skill/{name}/SKILL.md` 는 작업 템플릿/참조 문서로 활용할 수 있다.
- 두 경로의 스킬 파일은 동일한 내용을 유지해야 한다.
- Codex에서 이것은 자동 실행 단위가 아니라 사람이 읽고 따르는 가이드 문서다.

## Document Lifecycle

- 활성 작업 문서 위치:
  - `work/review/`
  - `work/todo/`
- 완료된 작업 문서는 자동 이동하지 않는다.
- 관련 `work/review`, `work/todo` 문서 정리는 `##커밋` 진행 시 함께 삭제하는 것을 기본으로 한다.
