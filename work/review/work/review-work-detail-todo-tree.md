# 업무 상세 TODO 트리 재사용 검토

- 재검토일: 2026-10-25
- 상태: 기존 구현 재사용 / 리뉴얼 2단계 통합 내 할 일·완료 시각 구현·로컬 검증 완료 / 실제 DB 적용 잔여.
- 후속 기준: [리뉴얼 검토](../common/review-project-renewal.md), [리뉴얼 계획](../../todo/common/todo-project-renewal.md)

## Scope

- WorkUnit에 연결된 TODO 트리의 현재 구현과 리뉴얼에서 재사용할 경계를 확인한다.
- 이전 문서의 신규 테이블·CRUD 도입 계획을 현재 상태에 맞게 정정한다.

## Summary

- TODO 저장 구조와 API·화면이 이미 존재한다. 동일 기능을 다시 만들 필요가 없다.
- 통합 내 할 일, 완료 시각, 사용자 시간대, 메일·간트·통계 구현과 로컬 검증은 상위 리뉴얼 계획의 2·4·5단계에서 완료했다. 메일은 비활성 상태로 보존한다.

## Findings or Scores

- [tb_wrk_todo.sql](../../../gw-home-infra-db/src/main/resources/sql/ddl/work/tb_wrk_todo.sql)에 업무·소유자·부모·제목·설명·상태·진행률·시작일·마감일·정렬·소프트 삭제 필드가 있다.
- [WorkTodoController.java](../../../gw-home-api/src/main/java/com/gw/api/controller/work/WorkTodoController.java)에 업무별 트리 조회·생성·수정·완료·삭제 API가 있다.
- [WorkTodoService.java](../../../gw-home-api/src/main/java/com/gw/api/service/work/WorkTodoService.java)는 소유권, 날짜 순서, 순환 부모 관계를 검사하고 최하위 완료 기준으로 상위 상태·진행률을 계산한다.
- [통합 내 할 일](../../../gw-home-ui/pages/work/todos/index.vue)과 [업무별 상세](../../../gw-home-ui/pages/work/todos/[workUnitUuid].vue)가 같은 feature 폼·트리·상세·편집 composable을 재사용한다.
- [WorkTodoServiceTest.java](../../../gw-home-api/src/test/java/com/gw/api/work/WorkTodoServiceTest.java)를 확장해 실행했다. 통합 조회·시간대·MyBatis 계약 테스트 및 SQL 검증 결과는 상위 계획에 기록했다.
- 지연 집계는 최하위 기준, 날짜는 클라이언트 IANA 시간대를 기준으로 통일했다.
- 완료 시각은 `cmpl_at`에 기록하며 기존 완료는 NULL로 유지한다. `updated_at`으로 기간별 완료 실적을 계산하지 않는다.
- WorkUnit 자체의 수동 상태와 TODO 파생 상태는 별개다. 최하위 TODO의 수동 진행중 상태가 구현되어 있다고 가정하지 않는다.

## Recommended Next Actions

1. 기존 테이블·CRUD·트리·폼·검증을 재사용한다.
2. 통합 목록·완료 시각·알림 테이블의 실제 DB 적용과 간트·통계의 운영 회귀를 확인한다. 메일 제공자 연결·실제 발송은 보류한다.
3. 이 문서의 과거 간트차트·전체 목록 개편 제외 조건은 새 리뉴얼 범위에 적용하지 않는다.
