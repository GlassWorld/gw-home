# .ai 문서 세팅 2차 압축 계획

## Goal

- 목적: 현재 문서 세팅의 구조적 장점은 유지하면서, 남아 있는 토큰 효율 병목을 줄여 총평을 `8.8 ~ 8.9 / 10` 수준으로 끌어올린다.

## Scope

- 포함:
  - `.ai/AGENTS.md`
  - `.ai/TASK_ROUTER.md`
  - `.ai/SKILL_INDEX.md`
  - `work/review/common/review-vibe-doc-setting.md`
- 제외:
  - `.ai/CORE_RULES.md`
  - 애플리케이션 코드 변경
  - 도메인 개발 문서 본문 개편
  - 새로운 스킬/문서 추가

## Current State

- 현재 상태:
  - 최신 리뷰 기준 현재 총평은 `8.6 / 10`이다.
  - 강점은 `바이브코딩 활용도`, `문서 구조`, `실행 가능성`이다.
  - 현재 가장 큰 병목은 `토큰 효율 7.6 / 10`이다.
  - 의미 중복은 일부 줄었지만 코어 문서 총량은 `1911 words -> 1973 words`로 소폭 증가했다.
  - 특히 `AGENTS`의 trigger 설명과 `TASK_ROUTER`의 work document rules가 함께 읽히며 누적 토큰을 만든다.
- 제약:
  - 단순 삭제로 실행 가능성을 훼손하면 안 된다.
  - source of truth 구조는 유지하고, 설명만 더 짧게 압축해야 한다.

## Action Items

1. `AGENTS`의 trigger 표 아래 보조 설명을 더 줄이고, 세부 work 문서 규칙은 `TASK_ROUTER` 참조 한 줄로 축약한다.
2. `TASK_ROUTER`의 `Work Document Minimum Sections`와 `Document ownership`를 하나의 더 짧은 블록으로 재구성한다.
3. `SKILL_INDEX`의 work document template 참조를 유지하되, 중복 신호가 크면 더 짧은 표현으로 압축한다.
4. 압축 후 코어 문서 총량과 중복 설명 구간을 다시 점검해 리뷰 문서 점수를 업데이트한다.

## Done Criteria

- `AGENTS`는 entry/triggers 중심 문서로 더 짧게 읽힌다.
- `TASK_ROUTER`는 work document 규칙의 단일 출처를 유지하면서도 현재보다 더 짧아진다.
- `SKILL_INDEX`는 단일 출처 역할을 유지하되 불필요한 반복 참조가 늘지 않는다.
- 다음 재검토에서 `토큰 효율` 점수가 현재보다 개선된다.
- 다음 재평가 총점이 `8.8 / 10` 이상이 될 수 있는 상태다.
