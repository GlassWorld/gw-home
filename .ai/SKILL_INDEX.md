# SKILL INDEX

참조 문서 위치:
- `.claude/skill/{name}/SKILL.md`
- `.claude/skills/{name}/SKILL.md`

해석 기준:
- Claude Code: 위 경로의 문서를 slash command 스타일 스킬 문서로 사용할 수 있다.
- Codex / 기타: 위 경로의 문서를 자동 스킬이 아니라 로컬 참조 문서로 본다.
- 즉, Codex에는 "Claude Code의 slash skill"과 동일한 자동 호출 개념이 기본 내장된 것은 아니며, 필요 시 사람이 읽는 작업 가이드처럼 참조한다.

---

## Backend

| Slash Command | 위치 | 설명 |
|---------------|------|------|
| `/create-domain-structure` | `.claude/skill/create-domain-structure/SKILL.md` | 신규 도메인 패키지 구조 전체 생성 |
| `/create-service` | `.claude/skill/create-service/SKILL.md` | Service 클래스 생성 |

## Database

| Slash Command | 위치 | 설명 |
|---------------|------|------|
| `/generate-ddl` | `.claude/skill/generate-ddl/SKILL.md` | DDL 스크립트 생성 |
| `/create-mapper` | `.claude/skill/create-mapper/SKILL.md` | Mapper 인터페이스 + XML 생성 |

## Frontend

| Slash Command | 위치 | 설명 |
|---------------|------|------|
| `/create-page` | `.claude/skill/create-page/SKILL.md` | Nuxt3 페이지 + composable 생성 |
| `/create-component` | `.claude/skill/create-component/SKILL.md` | Vue3 컴포넌트 생성 |

## Common

| Slash Command | 위치 | 설명 |
|---------------|------|------|
| `/create-api-endpoint` | `.claude/skill/create-api-endpoint/SKILL.md` | 백엔드 API 엔드포인트 전체 생성 |
| `/api-connect` | `.claude/skill/api-connect/SKILL.md` | 프론트 API 연동 타입 + composable 생성 |

---

## 호출 방법

### Claude Code
```
/create-api-endpoint DOMAIN: board ENDPOINT: GET /api/v1/boards PURPOSE: 게시글 목록 조회
/api-connect DOMAIN: board ENDPOINT: GET /api/v1/boards PAGING: true
/create-page DOMAIN: board PAGE: index AUTH: required
/generate-ddl TABLE: tb_brd_pst DOMAIN: board COLUMNS: 제목, 본문
```

### Codex / 기타 도구
```
SKILL: create-api-endpoint
DOMAIN: board
ENDPOINT: GET /api/v1/boards
PURPOSE: 게시글 목록 조회
```

위 형식은 "자동 스킬 호출"이 아니라, 참조할 작업 가이드를 명시하는 입력 형식으로 본다.
