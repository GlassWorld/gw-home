# 바이브코딩 문서 세팅 리뷰

## 범위

- 대상: `.ai/AGENTS.md`, `.ai/CORE_RULES.md`, `.ai/TASK_ROUTER.md`, `.ai/SKILL_INDEX.md`, `.ai/project/summary.md`
- 관점: 바이브코딩 활용도, 토큰 효율, 문서 구조, 중복도, 실행 가능성

## 총평

- 전체 평점: `8.1 / 10`
- 한줄 평가: 작은 코어 문서로 작업 흐름을 빠르게 고정하는 구조는 좋지만, 일부 규칙이 여러 문서에 반복되어 토큰 효율과 유지보수 효율이 조금 깎인다.

## 분야별 점수

| 항목 | 점수 | 메모 |
|------|------|------|
| 진입성 | `9.0 / 10` | 시작 문서와 우선순위가 분명하다 |
| 토큰 효율 | `7.5 / 10` | 코어 문서 총량은 작지만 반복 규칙이 있다 |
| 문서 구조 | `8.8 / 10` | 역할 분리가 선명하다 |
| 중복 관리 | `6.8 / 10` | 요약 문서와 규칙 문서 사이 반복이 보인다 |
| 실행 가능성 | `8.7 / 10` | 트리거와 라우팅 규칙이 구체적이다 |
| 확장성 | `7.9 / 10` | 현재 규모엔 적합하지만 도메인 증가 시 index 관리가 필요하다 |
| 모델 친화성 | `8.3 / 10` | 읽기 순서, 범위 제한, 금지 규칙이 모델 행동 제어에 유리하다 |

## 강점

1. `AGENTS -> summary -> 필요한 문서만 로드` 흐름이 짧고 명확하다.
2. 절대 규칙을 `.ai/CORE_RULES.md`로 모아 충돌 시 우선순위를 판단하기 쉽다.
3. `##검토`, `##계획`, `##작업`, `##커밋` 트리거가 명시되어 에이전트 행동을 안정적으로 유도한다.
4. `TASK_ROUTER`가 SIMPLE / NORMAL / HEAVY 기준을 분리해 과도한 구현을 막는다.

## 주요 이슈

### 1. 중복 규칙이 여러 문서에 퍼져 있어 수정 비용이 커질 수 있음

- `work/review`, `work/todo` 경로와 관련 규칙이 [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L58), [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L68), [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L78)에 반복된다.
- 스킬이 자동 실행이 아니라 참조용이라는 설명도 [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L76), [`.ai/SKILL_INDEX.md`](/home/glassworld/workspace/gw-home/.ai/SKILL_INDEX.md#L8), [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L67)에 분산돼 있다.
- 영향: 규칙 수정 시 2~3개 문서를 함께 수정해야 해서 drift 위험이 생긴다.

### 2. summary 문서가 요약과 규칙을 함께 가져가 토큰 재소모 가능성이 있음

- [`.ai/project/summary.md`](/home/glassworld/workspace/gw-home/.ai/project/summary.md#L45)에는 설계 원칙이 있고, 이 중 일부는 [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L7), [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L34), [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L39), [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L47)와 의미상 겹친다.
- 영향: summary를 읽은 뒤 rules를 다시 읽으면 모델 입장에서 같은 신호를 두 번 받는다.

### 3. AGENTS와 TASK_ROUTER 사이 경계가 살짝 겹침

- [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L54)는 트리거와 워크 문서 규칙을 담고 있고, [`.ai/TASK_ROUTER.md`](/home/glassworld/workspace/gw-home/.ai/TASK_ROUTER.md#L76)도 문서 lifecycle과 review/todo 경로를 다시 설명한다.
- 영향: 엔트리 문서가 길어질수록 router의 독립성이 약해진다.

## 토큰 효율 평가

- 코어 문서 5개 합계는 약 `1,854 words`, `367 lines`라서 절대량은 가볍다.
- 현재 구조는 대형 단일 문서보다 훨씬 효율적이다.
- 다만 다음 항목은 압축 여지가 있다.
  - work 문서 경로 규칙
  - skill 문서 해석 규칙
  - 설계 원칙과 절대 규칙의 중복 표현

## 개선 우선순위

1. `summary`는 진짜 요약만 남기고 규칙성 문장은 `CORE_RULES` 링크로 치환
2. work 문서 경로와 lifecycle은 `TASK_ROUTER` 한 곳만 source of truth로 두고 `AGENTS`는 링크만 유지
3. skill 해석 규칙은 `SKILL_INDEX`로 모으고 다른 문서에서는 한 줄 참조만 남기기

## 권장 목표 점수

- 위 3가지만 정리하면 예상 점수는 `8.8 ~ 9.1 / 10`
- 특히 토큰 효율은 `7.5 -> 8.6` 수준까지 개선 가능
