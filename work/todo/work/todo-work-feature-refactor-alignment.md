# 개편된 문서 기준 기반 work 프런트 리팩토링 정렬 계획

## Goal

- 목적: `work` 프런트 리팩토링 결과를 현재 문서 기준에 맞게 정렬해, 페이지 책임 분리 이점을 유지하면서 구조 규칙과 실제 코드 배치를 일치시킨다.

## Scope

- 포함:
  - `gw-home-ui/pages/work/*`
  - `gw-home-ui/features/work/*`
  - `gw-home-ui/composables/useDailyReportApi.ts`
  - `gw-home-ui/composables/useWeeklyReportApi.ts`
  - `gw-home-ui/composables/useWorkGitApi.ts`
  - `gw-home-ui/composables/useWorkUnitApi.ts`
  - `gw-home-ui/types/work.ts`
  - `docs/frontend/frontend-rules.md`
  - 관련 review 문서
- 제외:
  - work 도메인 백엔드 API 변경
  - 새로운 기능 추가
  - `work` 외 다른 프런트 도메인 구조 개편
  - 공통 UI 전면 재설계

## Current State

- 현재 상태:
  - `pages/work/*`는 얇은 엔트리 페이지 구조로 잘 정리되어 있다.
  - 공용 API 진입점은 `composables/`에 두고, feature 내부 구현은 `features/work/api/*`에 두는 방향으로 문서 기준을 맞췄다.
  - 컴포넌트 파일명은 저장소의 기존 PascalCase 관례를 유지하는 방향으로 문서 기준을 맞췄다.
  - `DailyReportEditorPage` 중간 래퍼는 제거했고, create/edit 페이지는 `DailyReportWorkspace`를 직접 조합한다.
  - `DailyReportFormModal`, `DailyReportDetailModal`, `WeeklyReportFormModal`, `WorkUnitForm`, `DailyReportWorkspace`와 workspace 하위 패널까지 `features/work/components`로 이동했다.
  - 현재 work 관련 UI는 사실상 `features/work/components` 아래로 모였고, `components/work`는 비어 있다.
- 제약:
  - 기존 라우트 동작과 사용자 화면 동작을 깨면 안 된다.
  - 문서와 코드 중 하나를 기준으로 정하면 나머지도 같은 방향으로 맞춰야 한다.
  - 작업 범위는 “정렬”에 집중하고, 별도 기능 확장은 포함하지 않는다.

## Action Items

1. 문서와 구조의 기준점을 지금처럼 유지한다.
   - 공용 API 진입점은 `composables/`, feature-local 구현과 타입은 `features/work`에 둔다.
   - 컴포넌트 파일명은 기존 디렉터리 관례를 유지하고, route 파일은 kebab-case 규칙을 유지한다.
2. `DailyReportEditorPage`처럼 얇은 중간 래퍼는 계속 점검해 유지 이유가 약한 경우 계층을 줄인다.
3. 정렬 작업 후 review 문서를 현재 상태 기준으로 갱신하고, 남은 예외가 있으면 명시한다.
4. `##커밋` 시점에 빈 `components/work` 디렉터리 정리 여부를 함께 판단한다.

## Done Criteria

- 문서와 실제 API 배치가 서로 같은 규칙을 가리킨다.
- 파일 네이밍 규칙이 실제 저장소 관례와 충돌하지 않는다.
- `pages/work/*`는 얇은 composition 역할을 유지한다.
- `features/work`와 `components/work`의 책임 경계가 설명 가능할 정도로 명확해진다.
- 후속 작업자가 `docs/frontend/frontend-rules.md`와 실제 코드 구조를 함께 봤을 때 탐색 경로가 충돌하지 않는다.
