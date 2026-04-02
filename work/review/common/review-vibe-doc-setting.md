# 바이브코딩 문서 세팅 리뷰

## Scope

- 대상: `.ai/AGENTS.md`, `.ai/CORE_RULES.md`, `.ai/TASK_ROUTER.md`, `.ai/SKILL_INDEX.md`, `.ai/project/summary.md`
- 관점: 바이브코딩 활용도, 토큰 효율, 문서 구조, 중복 관리, 실행 가능성

## Summary

- 한줄 요약: 현재 세팅은 entry, routing, skill reference 역할이 꽤 선명해졌고, 2차 압축으로 토큰 효율도 다시 개선돼 전체 균형이 한 단계 좋아졌다.
- 전체 판단: `8.8 / 10`

## Findings or Scores

| 항목 | 점수 | 평가 |
|------|------|------|
| 바이브코딩 활용도 | `9.2 / 10` | 빠른 진입과 작업 모드 고정이 더 매끄러워졌다 |
| 토큰 효율 | `8.4 / 10` | 총량이 다시 줄고 중복 설명도 더 압축됐다 |
| 문서 구조 | `9.1 / 10` | 역할 경계와 source of truth가 더 선명해졌다 |
| 중복 관리 | `8.2 / 10` | 핵심 반복 구간이 꽤 정리됐다 |
| 실행 가능성 | `9.0 / 10` | 트리거, 라우팅, 산출물 규칙이 계속 강하다 |
| 확장성 | `8.0 / 10` | 현재 규모엔 충분히 좋고 이후 인덱싱만 보강하면 된다 |
| 유지보수성 | `8.4 / 10` | 단일 출처 선언과 실제 수정 포인트가 더 가까워졌다 |

### 항목: 바이브코딩 활용도

- 점수: `9.2 / 10`
- 근거:
  - 진입 순서가 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L5)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L9)에 짧고 안정적으로 고정돼 있다.
  - `TASK_ROUTER`와 `SKILL_INDEX`를 각각 단일 출처로 명시한 부분이 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L31)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L33), [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L3), [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L3)에 반영됐다.
  - 요청 트리거는 여전히 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L56)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L65)에서 빠르게 해석되고, 보조 설명은 더 짧아졌다.
- 영향:
  - 새 세션에서 에이전트가 읽기 순서, 작업 기준, 참조 기준을 더 빠르게 고정할 수 있다.

### 항목: 토큰 효율

- 점수: `8.4 / 10`
- 근거:
  - 코어 문서 5개 총량이 `1973 words, 394 lines`에서 `1920 words, 375 lines`로 다시 줄었다.
  - [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L56)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L65)의 trigger 보조 설명이 축약됐다.
  - [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L91)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L96)에서 work 문서 규칙이 한 블록으로 압축됐다.
- 영향:
  - 구조 이해를 유지하면서 첫 로딩 부담도 실제로 줄었다.

### 항목: 문서 구조

- 점수: `9.1 / 10`
- 근거:
  - `TASK_ROUTER`가 task routing과 work document rules의 단일 출처라고 직접 선언해 [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L3) 문서 역할이 더 분명해졌다.
  - `SKILL_INDEX`도 skill interpretation의 단일 출처라고 선언해 [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L3) 경계가 분명해졌다.
  - `summary`의 quick reference가 [`.ai/project/summary.md`](/home/glassworld/workspace/gw-home/.ai/project/summary.md#L45)~[`.ai/project/summary.md`](/home/glassworld/workspace/gw-home/.ai/project/summary.md#L47)에서 한 줄로 압축돼 summary 역할에 더 가까워졌다.
- 영향:
  - 각 문서를 왜 읽는지 설명하기 쉬워졌고, 탐색 흐름도 더 자연스러워졌다.

### 항목: 중복 관리

- 점수: `8.2 / 10`
- 근거:
  - `summary` 쪽 반복은 많이 줄었다.
  - `##검토`, `##계획`, `##작업`의 세부 규칙은 더 짧아졌고, work 문서 규칙도 [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L91)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L96) 한 블록에 모였다.
  - `SKILL_INDEX`의 template purpose 문구도 [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L47)~[`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L52)에서 더 짧게 정리됐다.
- 영향:
  - 이전보다 중복 신호가 눈에 띄게 줄었고, drift 위험도 더 낮아졌다.

### 항목: 실행 가능성

- 점수: `9.0 / 10`
- 근거:
  - 요청 트리거별 행동은 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L56)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L67)에 그대로 명확하다.
  - 난이도 분류와 후속 액션은 [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L15)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L65)에서 충분히 구체적이다.
  - work 문서 최소 섹션과 템플릿 연결도 [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L91)~[`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L117), [review template](/home/glassworld/workspace/gw-home/work/review/_template.md), [todo template](/home/glassworld/workspace/gw-home/work/todo/_template.md)에 맞물려 있다.
- 영향:
  - 실사용 관점에서는 여전히 매우 강한 구조다.

### 항목: 확장성

- 점수: `8.0 / 10`
- 근거:
  - 프로젝트 개요와 도메인 정보는 [`.ai/project/summary.md`](/home/glassworld/workspace/gw-home/.ai/project/summary.md#L20)~[`.ai/project/summary.md`](/home/glassworld/workspace/gw-home/.ai/project/summary.md#L43) 정도면 빠르게 파악 가능하다.
  - `SKILL_INDEX`가 단일 출처가 되면서 skill 추가 시 어디에 정리할지 기준은 더 명확해졌다.
  - 다만 skill 수가 많이 늘면 현재 표 기반 index만으로는 검색성, 우선순위, 태그 분류가 부족할 수 있다.
- 영향:
  - 지금은 충분히 유연하지만, 성장 단계에서는 index 보강이 필요할 가능성이 있다.

### 항목: 유지보수성

- 점수: `8.4 / 10`
- 근거:
  - 이전에는 source of truth 선언과 실제 구조 사이 거리가 있었지만, 지금은 [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L3), [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L3)에서 직접 맞물리기 시작했다.
  - 2차 압축으로 `AGENTS`는 더 entry 중심이 되고, `TASK_ROUTER`는 더 규칙 중심이 되어 수정 포인트가 더 읽기 쉬워졌다.
- 영향:
  - 규칙 개정 시 수정 경로가 전보다 더 예측 가능해졌다.

## Recommended Next Actions

1. 현재 구조는 실사용 기준으로 충분히 안정적이므로, 다음 단계는 무리한 축약보다 실제 운영에서 헷갈리는 지점이 있는지 관찰하는 쪽이 낫다.
2. skill 수가 늘기 시작하면 `SKILL_INDEX`에 태그나 우선순위 열을 추가해 검색성을 보강한다.
3. 이후 다시 점수가 떨어진다면 문서 길이보다 "한 요청에서 실제 몇 파일을 읽게 되는지" 기준으로 재검토한다.
