# AGENTS (내부 참조용)

> 루트 `AGENTS.md` 가 메인 진입점이다. 이 파일은 `.ai/` 내부 문서 간 참조용이다.

## 문서 구조

```
.ai/
├── CORE_RULES.md       절대 규칙 (상세)
├── TASK_ROUTER.md      작업 분류 및 실행 절차
├── SKILL_INDEX.md      스킬 목록
├── project/
│   └── summary.md      프로젝트 스택 요약
└── skills/
    ├── backend/        create-domain-structure, create-service
    ├── frontend/       create-page, create-component
    ├── database/       generate-ddl, create-mapper
    └── common/         create-api-endpoint, api-connect
```

## External Reference Docs

- `.claude/skill/{name}/SKILL.md` 또는 `.claude/skills/{name}/SKILL.md` 는 저장소 내부 참조 문서 위치로 본다.
- 이 경로는 Claude Code 스타일의 스킬 문서 보관 위치이며, Codex의 자동 스킬 로딩 경로를 의미하지는 않는다.
- Codex로 작업할 때도 관련 작업이면 위 문서를 필요 시 우선 참고할 수 있다.
- 충돌 시 우선순위는 시스템/개발자 지침 → `.ai/CORE_RULES.md` → 작업 관련 문서 → `.claude/skill*` 참조 문서 순서로 해석한다.

## Context Loading

| Task | 읽을 문서 |
|------|-----------|
| Backend API | `docs/backend/backend-rules.md`, `docs/common/architecture.md` |
| DB / DDL | `docs/backend/database.md`, `docs/backend/domain.md` |
| Frontend page | `docs/frontend/frontend-rules.md`, `docs/frontend/pages.md` |
| Frontend API 연동 | `docs/common/api-contract.md`, `docs/frontend/frontend-rules.md` |
| Auth (frontend) | `docs/frontend/auth-flow.md`, `docs/common/api-contract.md` |
| 전체 구조 | `docs/common/architecture.md`, `docs/common/project-structure.md` |
