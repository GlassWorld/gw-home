# 바이브코딩 문서 세팅 리뷰

## Scope

- 대상: `.ai/AGENTS.md`, `.ai/CORE_RULES.md`, `.ai/TASK_ROUTER.md`, `.ai/SKILL_INDEX.md`, `.ai/project/summary.md`
- 관점: 바이브코딩 활용도, 토큰 효율, 문서 구조, 중복 관리, 실행 가능성, 참조 정확도, 인덱스 세분화 효과, 스킬 전환 후보

## Summary

- 한줄 요약: 전체 구조는 꽤 잘 잡혀 있지만, 실제 참조 경로 정확도와 work 문서 규칙의 중복 경계 때문에 체감 신뢰도가 깎인다.
- 전체 판단: `8.3 / 10`

## Findings or Scores

| 항목 | 점수 | 평가 |
|------|------|------|
| 바이브코딩 활용도 | `8.8 / 10` | 진입 순서와 트리거는 빠르지만 메타 문서 리뷰 흐름은 약하다 |
| 토큰 효율 | `8.1 / 10` | 코어 문서 총량은 나쁘지 않지만 항상 읽는 정보가 조금 넓다 |
| 문서 구조 | `8.7 / 10` | 역할 분리는 선명하나 work 문서 책임 경계가 약간 겹친다 |
| 중복 관리 | `7.6 / 10` | template, work 문서 규칙, 참조 성격 문구가 두 문서에 나뉘어 반복된다 |
| 실행 가능성 | `8.6 / 10` | 트리거와 라우팅은 실무적으로 충분히 작동한다 |
| 참조 정확도 | `6.9 / 10` | skill 경로 표기가 실제 저장소 구조와 맞지 않는다 |
| 유지보수성 | `8.4 / 10` | source of truth 선언은 좋지만 잘못된 링크 하나가 전체 신뢰를 해칠 수 있다 |

### Finding 1

- 항목: 참조 정확도
- 점수: `6.9 / 10`
- 근거:
  - [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L5)~[`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L8)은 skill reference path를 `.claude/skill/{name}/SKILL.md`, `.ai/skill/{name}/SKILL.md`로 안내한다.
  - 실제 저장소에는 `/.claude/skills/...` 구조가 있고 `.ai/skill` 경로는 보이지 않는다.
  - 문서가 source of truth를 선언한 상태에서 첫 경로 안내가 틀리면, 새 세션 에이전트가 skill을 잘못 찾거나 없는 경로를 먼저 탐색하게 된다.
- 영향:
  - 바이브코딩에서 가장 중요한 "빠르게 맞는 문맥으로 진입" 흐름이 첫 단계에서 흔들린다.

### Finding 2

- 항목: 중복 관리
- 점수: `7.6 / 10`
- 근거:
  - [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L91)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L100)이 work document rule과 template reference를 직접 가진다.
  - 동시에 [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L38)~[`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L52)은 review/todo template를 common skill처럼 한 번, work document reference로 한 번 더 적는다.
  - 결과적으로 "work 문서 규칙의 단일 출처는 TASK_ROUTER"라는 선언과 실제 유지 포인트가 완전히 일치하지 않는다.
- 영향:
  - 규칙 변경 시 `TASK_ROUTER`와 `SKILL_INDEX`가 같이 수정돼야 할 가능성이 커지고 drift 위험이 생긴다.

### Finding 3

- 항목: 토큰 효율
- 점수: `8.1 / 10`
- 근거:
  - 코어 문서 5개는 현재 `1920 words` 수준으로 아주 무겁지는 않다.
  - 다만 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L7)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L9)은 모든 작업에서 `summary`를 먼저 읽도록 유도한다.
  - 작은 수정이나 단일 도메인 작업에서도 stack, module overview를 매번 먼저 읽게 되면 entry cost가 누적된다.
- 영향:
  - 한 번 보면 작은 비용이지만, 짧은 세션이 반복되는 바이브코딩에서는 불필요한 고정 비용이 된다.

### Finding 4

- 항목: 문서 구조
- 점수: `8.7 / 10`
- 근거:
  - [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L11)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L19), [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L3), [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L3) 덕분에 큰 역할 분리는 분명하다.
  - 반면 메타 작업인 문서 세팅 리뷰 자체는 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L45)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L65)에서 별도 context loading 힌트가 없다.
  - 즉 제품 개발 흐름은 잘 안내하지만, 문서 체계 자체를 손보는 유지보수 작업 경로는 약하다.
- 영향:
  - 실사용에서는 괜찮지만, 문서 운영 개선 작업에서는 어디까지 읽어야 하는지 다시 판단해야 한다.

### Finding 5

- 항목: 실행 가능성
- 점수: `8.6 / 10`
- 근거:
  - [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L56)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L65)의 trigger 설계는 매우 직관적이다.
  - [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L15)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L65)의 SIMPLE, NORMAL, HEAVY 분류도 실무적으로 바로 쓸 수 있다.
  - [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L70)~[`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L74)의 repository exploration rule까지 붙어 있어 과탐색 방지 효과도 있다.
- 영향:
  - 실제 작업 개시 속도는 충분히 빠르고, 작업 폭주를 막는 안전장치도 있는 편이다.

## Recommended Structure

### 세분화 인덱스 개편 판단

- 평가: 효과 있음
- 근거:
  - 현재 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L45)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L65)은 context loading과 request trigger를 함께 들고 있다.
  - [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L67)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L100)은 task routing, skill selection, work document rule을 한 문서에서 같이 관리한다.
  - [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L38)~[`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L52)은 skill 목록과 work template reference를 함께 들고 있다.
- 판단:
  - 지금 구조는 작동은 잘 하지만, 책임이 약하게 겹쳐 있어 문서가 늘수록 토큰 효율보다 유지보수 비용이 먼저 나빠질 가능성이 크다.
  - 따라서 "얇은 엔트리 + 세분화 인덱스 + 필요 시 하위 문서 로드" 구조가 더 적합하다.

### 권장 파일 구조

```text
.ai/
  AGENTS.md
  CORE_RULES.md
  project/
    summary.md
  index/
    routing.md
    references.md
    skills.md
    work-docs.md
    doc-ops.md
```

### 파일별 역할

- `AGENTS.md`
  - 엔트리 전용
  - 작업 종류 판단, 다음에 읽을 인덱스 결정만 담당
- `CORE_RULES.md`
  - 절대 규칙 전용
- `index/routing.md`
  - `##검토`, `##계획`, `##작업`, `##커밋`
  - `SIMPLE`, `NORMAL`, `HEAVY`
  - 승인 흐름
- `index/references.md`
  - 현재 context loading 표를 이동
  - 작업 유형별 참조 문서 매핑
- `index/skills.md`
  - skill 목록, 실제 경로, 사용 조건만 유지
- `index/work-docs.md`
  - review, todo 문서 네이밍, 최소 섹션, template 위치 관리
- `index/doc-ops.md`
  - 문서 세팅 리뷰, 문서 개편, 메타 유지보수 작업 전용

### 이관 우선순위

1. [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L45)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L54) 의 context loading을 `index/references.md`로 이관
2. [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L91)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L100) 의 work 문서 규칙을 `index/work-docs.md`로 이관
3. [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L47)~[`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L52) 의 work template reference를 제거하거나 `index/work-docs.md`로 통합
4. [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L5)~[`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L14) 의 reference path를 실제 저장소 기준으로 정정

## Skillization Review

### 스킬로 전환 시 효율이 높은 후보

| 후보 | 우선순위 | 이유 |
|------|------|------|
| `work-review-writer` | 높음 | `##검토` 산출물을 반복적으로 구조화하는 작업이라 문서보다 스킬 효율이 높다 |
| `work-todo-writer` | 높음 | `##계획` 문서를 범위, 액션, 완료 기준으로 빠르게 정리하기 좋다 |
| `doc-setting-review` | 중간 | 문서 체계 자체를 검토하는 메타 작업은 별도 스킬로 분리하는 편이 자연스럽다 |
| `task-classifier` | 중간 | layer, complexity, skill 선택은 읽는 규칙보다 판단 절차 성격이 강하다 |

### 스킬로 옮기지 않는 편이 좋은 항목

- [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md)
  - 절대 규칙이라 스킬보다 상위 규범 문서로 남는 편이 안전하다.
- [`.ai/project/summary.md`](/home/glassworld/workspace/gw-home/.ai/project/summary.md)
  - 프로젝트 사실 요약 문서라 참조 문서 역할이 더 적합하다.
- [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md)
  - 스킬 자체가 아니라 스킬 메타 인덱스이므로 문서로 유지하는 편이 맞다.

### 스킬 전환 기준

- 반복적으로 같은 형식의 산출물을 생성하는가
- 조건을 읽고 판단 절차를 수행하는가
- 절대 규칙보다 실행 흐름에 가까운가
- 문서 본문보다 호출 단위 재사용성이 중요한가

## Recommended Next Actions

1. `SKILL_INDEX`의 reference path를 실제 경로와 맞추고, 존재하지 않는 `.ai/skill` 경로는 제거하거나 조건부 설명으로 바꾼다.
2. `TASK_ROUTER`와 `SKILL_INDEX`에 나뉜 work 문서 책임을 `index/work-docs.md` 단일 문서로 모은다.
3. `AGENTS`는 엔트리 전용으로 줄이고, context loading은 `index/references.md`로 분리한다.
4. `work-review-writer`, `work-todo-writer`를 1차 스킬화 후보로 잡고, 현재 템플릿과 출력 규칙을 스킬 입력/출력 포맷으로 옮긴다.
5. 다음 재검토 때는 문서 총 word 수보다 "짧은 요청 1건 처리 시 실제로 읽게 되는 파일 수"와 "분기 후 추가로 읽는 인덱스 수"를 핵심 지표로 본다.
