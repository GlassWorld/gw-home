# 업무·일상 할 일 분리 검토

- 상태: 구현·로컬 검증 완료 / 실제 DB 적용·배포·실기기 검증 대기.
- 문서 정리: 2026-10-25 사용자 요청에 따라 관련 구현·검증·문서를 일상 할 일 커밋으로 분리한다. 아래의 미커밋 언급은 최초 구현 당시 기록이다.

## Scope
- 2026-10-25 사용자 승인: 기존 내 할 일을 업무 할 일로 명명하고 일상 할 일을 독립 메뉴로 추가한다.
- 일상 전용 카테고리·등록/조회/수정/삭제·완료/취소·집계와 대시보드의 분리 요약을 구현한다.

## Summary
- Full-stack / HEAVY: 신규 DB 스키마와 API, 인증 사용자별 데이터, 프런트 페이지가 필요하다.
- 현재 업무 TODO는 WorkUnit 필수·트리·진행률·알림에 결합되어 있다. 일상 데이터를 `life` 도메인과 별도 두 테이블에 저장한다.
- 사용자의 “그렇게 작업” 승인을 현재 범위의 구현 승인으로 적용한다. 기존 작업 중 변경을 보존하며 커밋하지 않는다.

## Findings
- 업무 메뉴는 `/work/todos` 경로를 유지하고 표시명을 업무 할 일로 변경한다.
- 일상 메뉴는 `/life/todos`, API는 `/api/v1/life-todos`, 카테고리는 `/api/v1/life-todos/categories`로 둔다.
- 제목만 필수이며 카테고리·날짜·메모는 선택이다. 카테고리는 사용자별로 추가·이름 변경·삭제한다. 삭제하면 소속 할 일은 미분류로 보존한다.
- 상태는 미완료/완료다. 날짜 분류는 오늘·예정·지연·완료·일정 미지정이며 오늘은 사용자 시간대의 미완료 일정이다.
- 소유자는 인증 계정으로 결정하고 다른 사용자의 항목·카테고리는 404로 처리한다. 카테고리 연결과 삭제 경합은 사용자 행 잠금으로 직렬화한다.
- 일상 집계는 일상 테이블만 조회한다. 기존 업무 트리·간트·업무 통계·메일 조회는 변경하지 않는다.
- 반복 루틴·습관 연속 달성·일상 메일 알림은 이번 요청에 포함하지 않는다. 실제 DB 적용은 별도 배포 단계다.

## Files
- 신규: `gw-home-api/.../{controller,service,convert,dto}/life/`, `gw-home-share/.../{vo,jvo}/life/`, `gw-home-share/.../common/policy/LifeTodoPolicy.java`
- 신규: `gw-home-infra-db/.../mapper/life/`, `sql/ddl/life/tb_lf_todo.sql`, `life-todos-migrate.sql`
- 신규: `gw-home-ui/pages/life/todos/index.vue`, `features/life/{api,types,components,composables}/`, `composables/use-life-todo-api.ts`
- 수정: 업무 할 일 표시명 사용처, 메뉴·즐겨찾기 허용 경로, 대시보드 요약, `docs/{all-ddl.sql,backend/domain.md,backend/database.md,common/api-contract.md,frontend/pages.md}`
- 검증: 일상 서비스·HTTP·실제 Mapper SQL 테스트, 프런트 타입/빌드·로컬 화면 확인

## Recommended next actions
1. 구현·로컬 검증은 [실행 계획](../../todo/life/todo-life-todos.md)의 검증 기록대로 완료했다.
2. 실제 사용 전 `life-todos-migrate.sql`을 적용하고 API·UI를 배포한다. 실제 DB에는 아직 실행하지 않았다.
