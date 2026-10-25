# work 프런트 구조 정렬 계획 — 리뉴얼에 통합

- 정리일: 2026-10-25
- 상태: 구조 기준 승계 / 후속 작업은 리뉴얼 계획으로 통합.
- 검토: [work 구조 재사용 검토](../../review/work/review-work-feature-refactor.md)
- 실행 기준: [리뉴얼 계획](../common/todo-project-renewal.md)

## Goal

- 기존 work feature 구조를 유지하면서 모바일 할 일과 보고서 제거 요구를 반영한다.

## Scope

- 업무·TODO·Git feature 구조와 공용 API 진입점·feature 내부 타입 경계를 유지한다.
- 보고서 전용 컴포넌트·API·타입은 제거 대상으로 전환한다.
- 기존 “새 기능·백엔드·공통 UI 개편 제외” 조건은 이 과거 정렬 작업에만 해당하며 상위 리뉴얼을 제한하지 않는다.

## Current State

- 업무·TODO·Git UI는 `features/work`에 유지하고 보고서 전용 UI는 3단계에서 제거했다.
- `pages/work`는 feature UI 진입점이다. 공개·관리자 보고서 페이지와 대시보드의 보고서 연결은 제거했다.
- `work.types.ts`에는 업무·TODO·Git 타입을 유지하고 보고서 전용 타입을 제거했다.

## Action Items

- [x] 이전 구조 정렬 계획을 리뉴얼 계획과 연결한다.
- [x] 기존 API 진입점과 feature 내부 구현 경계를 유지하며 TODO 화면을 확장한다.
- [x] 상위 계획 3단계에서 보고서 전용 구현과 재노출 타입을 제거한다.
- [x] 공통 UI·로직·스타일 분리와 유지 대상 업무·Git 흐름을 검증한다.

## Done Criteria

- 페이지는 조합 중심이고, 공통화 근거와 feature 경계가 설명 가능하다.
- 보고서 유지 전제와 새 제거 요구가 충돌하지 않는다.
- 구현·검증 상태를 상위 리뉴얼 계획에서 관리한다.

- 검증은 상위 리뉴얼 계획의 2·3단계 기록을 따른다. 실제 Git 원격 호출과 운영 DB 적용은 수행하지 않았다.
