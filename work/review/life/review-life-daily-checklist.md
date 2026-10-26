# 일상 매일 체크 캘린더 검토

## Scope
- 요청: 매일 할 일을 했는지 캘린더와 체크리스트로 간단하게 체크한다.
- 적용 대상은 기존 `/life/todos`의 일상 할 일로 제안한다.
- 매일 반복 항목을 등록하고 날짜별 완료 체크·취소, 월별 기록 조회를 제공한다.

## Summary
- Full-stack / HEAVY. 사용자 `ㄱㄱ` 승인 후 구현했다. 실제 DB 적용·배포는 별도다.
- 기존 일상 할 일은 `completed`, `completedAt`, `dueDate`만 가지므로 날짜마다 독립적인 완료 여부를 저장할 수 없다.
- 기존 완료 상태를 매일 초기화하지 않고 반복 항목과 날짜별 기록을 별도로 관리한다.

## Findings
- 화면: 월 이동·오늘 이동, 날짜별 완료 수/전체 수, 날짜 선택 시 체크리스트. 초기 선택은 오늘이다.
- 입력: 제목만으로 매일 항목 등록. 체크 즉시 저장하며 재클릭으로 취소한다.
- 시작일 이후 항목을 표시하고 종료한 항목의 과거 기록은 유지한다. 미래 날짜는 조회만 가능하다.
- 미체크는 미완료로 표시한다. 과거 날짜도 수정할 수 있고 날짜 간 상태는 서로 영향을 주지 않는다.
- 기록 키는 사용자 소유 항목과 현지 날짜 조합으로 유일하게 관리한다. 날짜를 TIMESTAMP로 저장하지 않으며 날짜 표현은 기존 일상 날짜 정책을 확인해 정한다. 감사 시각은 TIMESTAMPTZ를 사용한다.
- 인증 계정 소유권, 중복 저장, 연속 클릭, 월 변경 중 늦은 응답, 저장 실패 시 상태 복구를 검증한다.
- 기존 단발성 할 일과 업무 통계는 유지한다. 매일 항목을 기존 단발성 집계에 중복 포함하지 않는다.

## Files
- 수정: `gw-home-ui/features/life/components/LifeTodoBoardPage.vue`
- 신규: 같은 components의 `LifeDailyChecklist.vue`, `LifeDailyCalendar.vue`
- 신규: `gw-home-ui/features/life/{api/life-daily.api.ts,types/life-daily.types.ts,composables/use-life-daily.ts}`
- 신규: `gw-home-api/src/main/java/com/gw/api/{controller,service,convert,dto}/life/`의 매일 체크 전용 클래스
- 신규: `gw-home-share/src/main/java/com/gw/share/{vo,jvo}/life/`의 반복 항목·기록 모델
- 신규: `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/life/LifeDailyMapper.java`, `src/main/resources/mapper/life/LifeDailyMapper.xml`
- 신규: `gw-home-infra-db/src/main/resources/sql/ddl/life/`의 반복 항목·날짜 기록 DDL 및 마이그레이션
- 수정: `docs/all-ddl.sql`, `docs/backend/database.md`, `docs/backend/domain.md`, `docs/common/api-contract.md`, `docs/frontend/pages.md`
- 신규: 일상 매일 체크 서비스·HTTP·SQL·UI 검증 파일

## Recommended next actions
- 구현과 로컬 검증 결과는 [구현 계획](../../todo/life/todo-life-daily-checklist.md)에 기록한다.
- 현재 다른 작업의 업무 간트·통계 미커밋 변경은 보존한다.
- 운영 DB 적용·배포는 별도다. 2026-10-27 사용자 요청에 따라 구현·검증·관련 문서를 함께 커밋한다.
