# 일상 매일 체크 캘린더 검토

## Scope
- 2026-10-31 후속 요청: 추천 운동·카테고리 없이 텍스트로 직접 항목을 등록한다. 캘린더 하단 목록을 한 줄 중심으로 축소하고, 반복 종료 대신 즉시 삭제한다. 아래 후속 검토가 이전 운동 빠른 추가 구현보다 우선한다.
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

## 텍스트 등록·간결한 목록·즉시 삭제 후속 검토 (2026-10-31)
- 분류: Full-stack / HEAVY. 사용자 `ㄱㄱ` 승인 후 구현.
- 현재 문제: 추천 운동 버튼을 추가한 이전 변경은 사용자 의도와 다르다. 항목마다 체크 행과 상태·버튼 행이 따로 있어 모바일 세로 공간을 많이 사용한다. 서버는 오늘까지 남기는 반복 종료만 제공하며 삭제 API가 없다.
- 목표: 직접 텍스트 입력으로만 추가한다. 체크박스·항목명·삭제 버튼을 한 행에 배치하고 완료/미완료 별도 행을 제거한다. 긴 제목만 줄바꿈하고 터치 영역을 보장한다. 제목 수정은 작은 보조 버튼으로 유지한다.
- 삭제: 짧은 확인 후 `DELETE /api/v1/life-daily/{uuid}`를 호출한다. 성공 즉시 목록·월간 개수를 다시 조회한다. 삭제된 항목과 해당 체크 기록은 모든 날짜의 화면·집계에서 제외한다. 기존 `del_at`을 사용해 소프트 삭제하고 물리 데이터는 제거하지 않는다. 새 스키마는 필요 없다.
- 소유권 검증·계정 잠금·서비스 트랜잭션을 재사용한다. 기존 조회는 이미 항목의 `del_at IS NULL`을 검사하므로 항목 소프트 삭제로 체크 기록도 노출되지 않는다.
- 기존 종료 API는 호환성을 위해 유지하되 화면에서는 종료 기능을 제거한다. 이미 종료된 항목도 본인 소유이면 삭제할 수 있다.
- 실패하면 목록과 체크를 유지하고 오류를 표시한다. 저장 중 중복 삭제·모달 닫기를 차단한다.
- 수정 파일: `LifeDailyChecklist.vue`, `use-life-daily.ts`, `life-daily.api.ts`; 서버 `LifeDailyController.java`, `LifeDailyService.java`; `LifeDailyMapper.java`, `LifeDailyMapper.xml`; 해당 서비스·HTTP·SQL·UI 테스트; `docs/frontend/pages.md`, `docs/common/api-contract.md`; 이 검토와 연결된 todo.
- 검증: 등록 후 즉시 삭제·새로고침 후 미노출·과거/미래 집계 제외·종료 항목 삭제·타인 삭제 차단·실패 복구·360/390px 목록 높이와 긴 제목·타입 검사·빌드·관련 서버 회귀.
