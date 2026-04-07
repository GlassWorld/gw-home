# 업무 상세 연관 TODO 트리 기능 검토

## Scope

- 대상:
  - `gw-home-infra-db` 의 `work` 도메인 DDL, Mapper, XML
  - `gw-home-share` 의 `work` 도메인 `VO` 와 정책 상수
  - `gw-home-api` 의 `work` 도메인 Controller, Service, DTO, Convert
  - `gw-home-ui` 의 `work` 도메인 페이지, feature component, API, 타입
  - 내비게이션 메뉴에 추가될 신규 메뉴 `업무 세부 작업`
- 관점:
  - 기존 `업무관리(WorkUnit)` 구조 위에 트리형 TODO 상세 기능을 안전하게 확장할 수 있는지
  - 진행률, 상태, 일정, 정렬, 접기/펼치기 요구사항을 일관된 데이터 기준으로 처리할 수 있는지
  - 업무 자체 진행률을 TODO 기반으로 계산 가능하도록 API 와 UI 구조를 열어둘 수 있는지

## Summary

- 한줄 요약: 이 요청은 `work` 도메인에 신규 하위 개념인 트리형 TODO 를 추가하는 전체 기능 확장으로, DB 스키마와 API, 화면, 메뉴가 함께 바뀌는 `HEAVY` 작업이다.
- 전체 판단: 즉시 구현보다 영향 범위와 데이터 기준을 먼저 고정한 뒤 단계적으로 진행하는 편이 안전하다.

## Findings or Scores

### Finding 1

- 항목: 현재 `work` 도메인은 업무 기본정보와 Git 연동 중심이며, 연관 TODO 트리 저장 구조는 아직 없다.
- 점수: `9.3 / 10`
- 근거:
  - [WorkUnitController.java](/home/glassworld/workspace/gw-home/gw-home-api/src/main/java/com/gw/api/controller/work/WorkUnitController.java#L28)는 `/api/v1/work-units` 하위에서 업무 목록, 상세, 사용여부, Git 커밋만 제공한다.
  - [WorkUnitService.java](/home/glassworld/workspace/gw-home/gw-home-api/src/main/java/com/gw/api/service/work/WorkUnitService.java#L31)는 `WorkUnit` 자체의 CRUD 와 Git 프로젝트 매핑 보강에 집중한다.
  - [tb_work_unit.sql](/home/glassworld/workspace/gw-home/gw-home-infra-db/src/main/resources/sql/ddl/work/tb_work_unit.sql) 과 [WorkUnitMapper.xml](/home/glassworld/workspace/gw-home/gw-home-infra-db/src/main/resources/mapper/work/WorkUnitMapper.xml#L41)는 업무 단위 저장만 다루고, 계층형 TODO 나 진행률 집계 컬럼은 없다.
- 영향:
  - 요구사항을 만족하려면 `tb_work_unit` 확장만으로는 부족하고, 별도 TODO 테이블과 조회/저장 계약이 필요하다.

### Finding 2

- 항목: TODO 진행률과 상태는 단순 수동 입력보다 하위 항목 기준의 파생 규칙으로 고정해야 한다.
- 점수: `9.0 / 10`
- 근거:
  - 요구사항상 최하위 항목만 직접 완료 체크가 가능하고, 상위 항목은 하위 완료율로 상태와 진행률이 자동 계산된다.
  - 상위 항목의 `status`, `progressRate` 를 수동 수정 가능하게 두면 트리 집계 결과와 쉽게 충돌한다.
  - 현재 프로젝트 규칙은 서비스에서 검증과 흐름을 관리하고, 응답은 DTO/convert 에서 일관되게 내보내도록 권장한다.
- 영향:
  - 저장 기준은 `leaf 직접 완료 + 저장 시 재계산` 또는 `조회 시 계산` 중 하나로 고정해야 한다.
  - 구현 안정성과 목록/상세 재사용을 고려하면 `저장 시 상태/진행률 재계산 + 조회 시 overdue 파생` 방식이 가장 현실적이다.

### Finding 3

- 항목: 신규 메뉴 `업무 세부 작업`은 기존 `/work` 관리 화면과 분리된 상세 화면 라우트가 필요하다.
- 점수: `8.8 / 10`
- 근거:
  - [navigation-menu.ts](/home/glassworld/workspace/gw-home/gw-home-ui/constants/navigation-menu.ts#L11)는 현재 `업무관리`, `깃 계정관리`, `내주간보고`, `일일보고관리` 메뉴만 가진다.
  - [pages/work/index.vue](/home/glassworld/workspace/gw-home/gw-home-ui/pages/work/index.vue#L1)는 목록/관리 엔트리이고, [WorkUnitManagementPage.vue](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/components/WorkUnitManagementPage.vue#L1)는 상세 트리 편집에 적합한 구조가 아니다.
  - [pages.md](/home/glassworld/workspace/gw-home/docs/frontend/pages.md#L14)는 현재 `/work` 라우트를 업무 등록과 조회로 정의한다.
- 영향:
  - `업무 세부 작업`은 적어도 `업무 선택/목록` 과 `업무별 TODO 상세` 를 분리해야 한다.
  - 가장 자연스러운 후보는 신규 메뉴 엔트리 페이지와 `workUnitUuid` 기반 상세 라우트 추가다.

### Finding 4

- 항목: 업무 자체 진행률 연계까지 고려하면 TODO 응답 모델은 트리와 집계 요약을 함께 제공하는 편이 좋다.
- 점수: `8.7 / 10`
- 근거:
  - 현재 [WorkUnitResponse.java](/home/glassworld/workspace/gw-home/gw-home-api/src/main/java/com/gw/api/dto/work/WorkUnitResponse.java) 와 [work.types.ts](/home/glassworld/workspace/gw-home/gw-home-ui/features/work/types/work.types.ts#L38)는 업무 기본 메타와 Git 프로젝트만 가진다.
  - 요구사항은 TODO 행 단위 정보뿐 아니라 업무 진행률도 TODO 완료율 기준으로 계산 가능해야 한다고 명시한다.
- 영향:
  - 업무 상세 응답에 TODO 트리와 함께 `todoProgressRate`, `todoStatusSummary`, `todoTotalCount`, `todoCompletedCount` 같은 요약값을 같이 내리면 향후 목록 반영도 쉬워진다.

## Recommended Next Actions

1. TODO 저장 기준을 `별도 테이블 신설 + parentId 기반 트리 + leaf 직접 완료 + 서비스 재계산`으로 먼저 확정한다.
2. 백엔드와 프런트에서 공통으로 사용할 상태 체계를 `TODO`, `IN_PROGRESS`, `DONE`, `DELAYED` 중심으로 정리하고, overdue 는 일정과 완료 여부 기준으로 파생 규칙을 명확히 한다.
3. 신규 메뉴 `업무 세부 작업` 의 라우트 구조를 `목록/선택` 과 `업무별 상세 트리` 로 나눌지 먼저 결정한 뒤 구현에 들어간다.
