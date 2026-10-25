# work 프런트 구조 재사용 검토

- 재검토일: 2026-10-25
- 상태: 기존 구조 원칙을 리뉴얼에 승계. 보고서 유지 전제는 종료한다.
- 후속 기준: [리뉴얼 검토](../common/review-project-renewal.md), [리뉴얼 계획](../../todo/common/todo-project-renewal.md)

## Scope

- `gw-home-ui/pages/work`, `features/work`, 공용 API 진입점과 타입의 구조를 재사용한다.
- 업무·TODO·Git 유지 영역과 일일·주간보고 제거 영역의 경계를 정리한다.

## Summary

- 페이지에서 feature UI를 조합하고 feature 내부 API·타입을 관리하는 구조를 유지한다.
- 구조 정렬을 위해 보고서 컴포넌트를 계속 보존해야 한다는 과거 전제는 새 제거 요구에 따라 종료한다.

## Findings or Scores

- [TODO 진입 페이지](../../../gw-home-ui/pages/work/todos/index.vue)는 feature 컴포넌트를 조합하는 얇은 진입점이다.
- [features/work](../../../gw-home-ui/features/work)에는 업무·TODO·Git UI, API, composable, 타입을 유지한다. 보고서 전용 구현은 3단계에서 제거했다.
- [프런트 규칙](../../../docs/frontend/frontend-rules.md)은 공용 API 진입점과 feature 내부 API·타입 배치를 허용한다.
- [work.types.ts](../../../gw-home-ui/features/work/types/work.types.ts)에 업무·TODO·Git 타입을 유지하고 보고서 타입만 제거했다.
- 보고서 전용 UI는 재사용 대상을 별도로 확인한다. 이름이 일반적인 패널도 실제 import 관계를 근거로 제거 여부를 판단한다.
- 과거 점수와 특정 래퍼 제거 평가를 최신 전체 화면 품질 보증으로 사용하지 않는다. 이번 확인은 정적 구조 확인이다.

## Recommended Next Actions

1. `pages → features/work` 조합 원칙과 기존 공통 UI 우선 확장을 유지한다.
2. 모바일 할 일은 기존 TODO 컴포넌트·composable을 먼저 확장한다.
3. 보고서 전용 UI·API·타입과 대시보드·관리자 연결 제거 결과는 상위 계획 3단계 기록을 따른다.
4. 불필요한 중간 래퍼와 화면별 반복 CSS를 추가하지 않는다.
