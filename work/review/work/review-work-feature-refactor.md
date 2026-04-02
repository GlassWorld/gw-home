# 개편된 문서 기준 기반 work 프런트 리팩토링 검수

## Scope

- 대상: `gw-home-ui/pages/work/*`, `gw-home-ui/features/work/*`, `gw-home-ui/composables/useDailyReportApi.ts`, `gw-home-ui/composables/useWeeklyReportApi.ts`, `gw-home-ui/composables/useWorkGitApi.ts`, `gw-home-ui/composables/useWorkUnitApi.ts`, `gw-home-ui/types/work.ts`
- 관점: 개편된 프런트 문서 기준 부합 여부, 페이지 책임 축소 완성도, 구조 일관성, 후속 유지보수 안전성

## Summary

- 한줄 요약: 문서와 구조의 충돌은 정리됐고 얇은 editor 래퍼도 제거됐지만, work 전용 UI의 위치 경계는 여전히 조금 더 다듬을 여지가 있다.
- 전체 판단: `8.6 / 10`

## Findings or Scores

| 항목 | 점수 | 평가 |
|------|------|------|
| 페이지 책임 분리 | `9.0 / 10` | `pages/work/*`는 얇은 엔트리로 유지되고 불필요한 editor 중간 래퍼도 제거됐다 |
| API 배치 일관성 | `8.7 / 10` | feature-local API 허용 기준이 문서와 코어 규칙에 반영됐다 |
| 네이밍 규칙 부합성 | `8.3 / 10` | 기존 디렉터리 관례 유지 기준이 문서에 반영되어 실제 저장소 패턴과 충돌이 줄었다 |
| 구조 응집도 | `9.2 / 10` | `components/work` 의존이 사라지고 work UI가 `features/work/components` 아래로 정리됐다 |

### Finding 1

- 항목: 문서와 실제 API/타입 배치 충돌은 해소됨
- 점수: `8.7 / 10`
- 근거:
  - [`frontend-rules.md`](/home/glassworld/workspace/gw-home/docs/frontend/frontend-rules.md#L50)~[`frontend-rules.md`](/home/glassworld/workspace/gw-home/docs/frontend/frontend-rules.md#L56)는 공용 API 진입점은 `composables/`에 두되, feature-local API와 타입은 `features/{domain}` 아래에 둘 수 있다고 명시한다.
  - [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L47)~[`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L59)도 같은 방향으로 정리되어, [`useDailyReportApi.ts`](/home/glassworld/workspace/gw-home/gw-home-ui/composables/useDailyReportApi.ts#L1) 같은 공개 진입점과 `features/work/api/*`의 내부 구현 위치가 같은 규칙 아래 설명된다.
- 영향:
  - 후속 작업자가 문서를 읽고 구조를 따라가도 공개 진입점과 feature 내부 구현 위치를 혼동할 가능성이 크게 줄었다.

### Finding 2

- 항목: 파일 네이밍 규칙이 실제 저장소 관례에 맞게 정리됨
- 점수: `8.3 / 10`
- 근거:
  - [`frontend-rules.md`](/home/glassworld/workspace/gw-home/docs/frontend/frontend-rules.md#L22)~[`frontend-rules.md`](/home/glassworld/workspace/gw-home/docs/frontend/frontend-rules.md#L34)는 라우트/일반 파일은 kebab-case, 컴포넌트 파일은 기존 디렉터리 관례를 유지하도록 수정됐다.
  - [`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L45)~[`.ai/CORE_RULES.md`](/home/glassworld/workspace/gw-home/.ai/CORE_RULES.md#L58)도 기존 컴포넌트 디렉터리 관례를 보존하도록 정리되어, 현재 저장소의 PascalCase Vue component 패턴과 충돌하지 않는다.
- 영향:
  - 앞으로는 route 파일과 component 파일을 서로 다른 규칙으로 설명할 수 있어, 예외처럼 보이던 부분이 규칙 안으로 들어왔다.

### Finding 3

- 항목: work 전용 UI가 feature 내부로 거의 완전히 수렴됨
- 점수: `9.2 / 10`
- 근거:
  - [`create.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/work/daily-reports/create.vue#L1)와 [`edit.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/work/daily-reports/[dailyReportUuid]/edit.vue#L1)는 이제 [`DailyReportWorkspace.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/components/DailyReportWorkspace.vue#L1)를 직접 조합한다.
  - [`DailyReportWorkspace.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/components/DailyReportWorkspace.vue#L1), [`DailyReportHistorySidebar.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/components/DailyReportHistorySidebar.vue#L1), [`ReportEditorPanel.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/components/ReportEditorPanel.vue#L1), [`TaskListPanel.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/components/TaskListPanel.vue#L1), [`ReportHistoryPanel.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/components/ReportHistoryPanel.vue#L1)까지 `features/work/components` 아래로 이동했다.
  - 현재 work 관련 UI 컴포넌트는 사실상 모두 `features/work/components`에 모였고, `components/work`는 비워진 상태다.
- 영향:
  - 이제 `work` 화면을 수정할 때 진입 경로가 `pages/work/* -> features/work/components/*`로 거의 고정돼 탐색 비용이 크게 줄었다.

## Recommended Next Actions

1. 다음 work 화면 리팩토링 때는 새 wrapper를 추가하기보다, 실제 page-specific composition인지 먼저 확인한다.
2. `features/work/components` 안에서도 page entry, modal/form, workspace panel을 더 분리할 필요가 있는지 필요 시 재검토한다.
3. `##커밋` 시점에는 빈 `components/work` 정리 여부와 이 리뷰/todo 문서 최신화 여부를 함께 점검한다.
