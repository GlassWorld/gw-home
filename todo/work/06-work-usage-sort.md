# Todo: 업무 사용 이력 정렬 기준 정비

> 연관 문서: [review/work/03-overall-refactor-check.md](../../review/work/03-overall-refactor-check.md)
> 작업 레벨: **HEAVY**
> 레이어: **Backend + Frontend**

---

## 작업 범위

- `work_unit`의 `recent`, `frequent` 정렬 기준을 실제 동작과 맞춘다
- `use_cnt`, `last_used_at` 컬럼을 살릴지, 정렬 옵션을 숨길지 방향을 확정한다
- 저장 로직, 조회 로직, 화면 옵션의 정합성을 맞춘다

### 현재 요청 기준 우선 범위

- `tb_work_unit` 사용 이력 컬럼 활용 여부 결정
- 일일보고 저장 시 사용 이력 갱신 여부 정리
- 업무 목록 정렬 옵션과 실제 동작 정합성 맞춤

### 현재 요청 기준 범위 제외

- 업무 도메인 전체 구조 재설계
- 정렬 UI 전면 개편
- 별도 통계 기능 추가

---

## 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| Backend - work | 저장 시 사용 이력 갱신 또는 정렬 옵션 제거 |
| Mapper | 정렬 조건과 쿼리 기준 재정리 |
| Frontend - work | 노출 정렬 옵션과 실제 정책 정합성 정리 |
| Docs | 정렬 기준 설명 보완 가능 |

---

## 설계 방향

### 1. 먼저 정책을 확정한다

- `use_cnt`, `last_used_at`를 실제 운영 지표로 사용할지
- 아니면 아직 미사용 컬럼으로 보고 정렬 옵션을 감출지

정책이 확정되지 않으면 저장 로직과 조회 로직이 계속 어긋난다.

### 2. 컬럼을 살릴 경우 갱신 지점을 명확히 한다

- 일일보고 등록 시 증가/갱신
- 일일보고 수정 시 재계산 여부
- 삭제 시 역보정 여부

운영 복잡도를 고려해 최소 정책부터 정한다.

### 3. 컬럼을 쓰지 않을 경우 UI를 먼저 보정한다

- `recent`, `frequent` 옵션 비노출 또는 비활성화
- 문서와 API 허용값 정리

---

## 구현 체크리스트

### 1. 정책 결정

- [x] `use_cnt`, `last_used_at` 컬럼 유지 목적 재확인
- [x] 정렬 옵션 유지 vs 비노출 방향 결정

### 2. 백엔드 정리

- [x] 정렬 분기 로직 재확인
- [x] 일일보고 저장/수정 시 사용 이력 갱신 필요 여부 검토
- [x] 필요한 경우 Mapper/Service 갱신 로직 추가

### 3. 프론트 정리

- [x] 업무 목록 정렬 옵션 실제 노출 상태 확인
- [x] 정책에 맞게 옵션 목록 정리
- [x] 설명 문구 또는 기본값 영향 확인

### 4. 테스트 및 검증

- [x] 정렬 결과 회귀 확인
- [x] 업무 등록/사용 후 정렬 변화 확인
- [x] 옵션 제거 시 API/프론트 허용값 회귀 확인

---

## 진행 결과

- `recent`, `frequent` 정렬 옵션은 유지하기로 확정했다.
- 일일보고-업무 연결 저장 후 영향받은 업무의 `use_cnt`, `last_used_at`를 재계산하도록 정리했다.
- 프론트 정렬 옵션은 그대로 사용하되, 이제 실제 데이터 기준으로 정렬이 동작하도록 백엔드 정합성을 맞췄다.

---

## 후보 대상 파일 메모

- `gw-home-api/src/main/java/com/gw/api/service/work/WorkUnitService.java`
- `gw-home-api/src/main/java/com/gw/api/service/work/DailyReportService.java`
- `gw-home-infra-db/src/main/resources/mapper/work/WorkUnitMapper.xml`
- `gw-home-infra-db/src/main/resources/sql/ddl/work/tb_work_unit.sql`
- `gw-home-ui/pages/work/index.vue`
- `gw-home-ui/types/work.ts`
