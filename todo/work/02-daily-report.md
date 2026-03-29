# Todo: 일일보고 및 주간보고 기반 기능 구현

> 연관 문서: [review/work/02-daily-report.md](../../review/work/02-daily-report.md)
> 작업 레벨: **HEAVY**
> 레이어: **Full-stack**

---

## 작업 범위

- 개인 전용 일일보고 `work` 도메인 기능 추가
- 본인 작성/수정/목록 조회와 관리자 전체 조회, 누락 조회 API 구현
- 일일보고 작성 및 목록 화면 구현
- 공개형 주간보고를 위한 별도 모델과 작성 화면 기반 구조 구현
- 주간보고 작성 시 같은 주의 일일보고를 옆 패널에서 참고할 수 있는 화면 구성 마련
- 주간보고 작성 방식으로 수동 작성과 AI 보조 초안 생성을 모두 수용할 수 있는 구조 설계

### 범위 제외

- 타인의 일일보고를 일반 사용자가 조회하는 기능
- 관리자에 의한 타인 일일보고 수정/삭제 기능
- 휴가, 공휴일, 탄력근무까지 포함한 정교한 누락 계산
- AI가 생성한 주간보고를 자동 게시하는 기능
- OpenAI 연동 고도화 모델 튜닝, 프롬프트 운영도구, 사용량 대시보드

---

## 구현 체크리스트

### 1. 문서 및 범위 고정

- [ ] `review/work/02-daily-report.md` 기준으로 일일보고/주간보고 경계 재확인
- [ ] 일일보고는 개인용, 주간보고는 공개용이라는 권한 분리를 작업 중 계속 유지
- [ ] OpenAI 사용은 선택 기능이며 수동 작성만으로도 주간보고 작성이 가능해야 한다는 점 반영

### 2. DB 설계 및 마이그레이션 - 일일보고

- [ ] `gw-home-infra-db/src/main/resources/sql/ddl/work/tb_wrk_dly_rpt.sql` 작성
- [ ] `tb_wrk_dly_rpt`에 `mbr_acct_idx`, `rpt_dt`, `cntn`, `sts`, `note` 정의
- [ ] `mbr_acct_idx + rpt_dt` 유니크 제약 추가
- [ ] `TIMESTAMPTZ` 감사 컬럼 규칙과 축약형 컬럼 네이밍 준수 확인
- [ ] 본인 목록, 관리자 날짜 정렬, 누락 조회를 고려한 인덱스 정의

### 3. DB 설계 및 마이그레이션 - 주간보고

- [ ] `gw-home-infra-db/src/main/resources/sql/ddl/work/tb_wrk_wkl_rpt.sql` 초안 작성
- [ ] 공개 보고에 필요한 `wk_start_dt`, `wk_end_dt`, `ttl`, `cntn`, `opn_yn`, `pbls_at`, `gen_type` 구조 정의
- [ ] 일일보고 테이블과 직접 혼합하지 않고 별도 공개 엔터티로 유지
- [ ] 임시저장과 게시 상태를 구분할 수 있는 최소 컬럼 구조 확정

### 4. Backend share 모델

- [ ] `gw-home-share/src/main/java/com/gw/share/vo/work/DailyReportVo.java` 생성
- [ ] `gw-home-share/src/main/java/com/gw/share/vo/work/DailyReportListSearchVo.java` 생성
- [ ] 필요 시 관리자 조회용 `DailyReportJvo.java` 생성
- [ ] 주간보고 구현 범위가 포함되면 `WeeklyReportVo.java` 생성
- [ ] `BaseVo` 상속 및 프로젝트 네이밍 규칙 준수 확인

### 5. Backend infra-db - 일일보고

- [ ] `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/work/DailyReportMapper.java` 생성
- [ ] `gw-home-infra-db/src/main/resources/mapper/work/DailyReportMapper.xml` 생성
- [ ] 본인 목록 조회 SQL에 `dateFrom`, `dateTo`, `status`, `keyword` 조건 반영
- [ ] 관리자 조회 SQL에 회원 필터와 작성 여부/누락 계산 연계 구조 반영
- [ ] 모든 본인 조회/수정 SQL에 `mbr_acct_idx` 조건 강제
- [ ] `_idx` 외부 노출 없이 `uuid`만 응답에 사용되는지 확인

### 6. Backend infra-db - 주간보고

- [ ] `WeeklyReportMapper.java` 및 XML 초안 생성 여부 결정
- [ ] 같은 주 일일보고 조회용 보조 쿼리 설계
- [ ] 주간보고 작성 화면에서 사용할 "주간 범위 일일보고 묶음 조회" SQL 설계
- [ ] 공개 목록과 본인 초안 목록을 분리할 수 있는 조회 구조 검토

### 7. Backend API - 일일보고

- [ ] `gw-home-api/src/main/java/com/gw/api/dto/work/` 하위 일일보고 Request/Response DTO 생성
- [ ] `DailyReportService.java` 생성
- [ ] `DailyReportController.java` 생성
- [ ] `GET /api/v1/daily-reports` 구현
- [ ] `GET /api/v1/daily-reports/{uuid}` 구현
- [ ] `POST /api/v1/daily-reports` 구현
- [ ] `PUT /api/v1/daily-reports/{uuid}` 구현
- [ ] `GET /api/v1/daily-reports/missing` 구현
- [ ] `GET /api/v1/admin/daily-reports` 구현
- [ ] `GET /api/v1/admin/daily-reports/missing` 구현
- [ ] 동일 날짜 중복 작성 시 생성 차단 또는 수정 유도 정책 반영

### 8. Backend API - 주간보고

- [ ] `WeeklyReportService.java` / `WeeklyReportController.java` 범위 확정
- [ ] 주간보고 초안 저장 API 설계
- [ ] 주간보고 게시 API 설계
- [ ] 같은 주의 일일보고 참고 데이터 조회 API 설계
- [ ] AI 보조 초안 생성 API는 수동 작성과 독립적으로 호출 가능하게 설계
- [ ] OpenAI 미사용 환경에서도 수동 작성 API만으로 동작 가능하도록 분리

### 9. AI 연계 설계

- [ ] OpenAI 호출 책임을 controller가 아니라 service 또는 별도 adapter 계층으로 분리
- [ ] 입력 데이터는 해당 주의 일일보고 목록과 기간 정보로 제한
- [ ] AI 결과는 게시 완료본이 아니라 편집 가능한 초안 문자열로만 반환
- [ ] OpenAI 실패 시에도 사용자가 수동 작성 흐름을 계속 사용할 수 있게 예외 흐름 정리
- [ ] 프롬프트/모델 선택값을 하드코딩하지 않고 설정 가능한 구조 검토

### 10. Frontend 타입 및 API 연동

- [ ] `gw-home-ui/types/work.ts` 또는 분리 타입 파일에 `DailyReport`, `WeeklyReport` 타입 추가
- [ ] `gw-home-ui/composables/useDailyReportApi.ts` 생성
- [ ] 필요 시 `gw-home-ui/composables/useWeeklyReportApi.ts` 생성
- [ ] 목록 조회, 생성, 수정, 누락 조회, 주간 초안/게시, AI 초안 요청 메서드 정의
- [ ] `ApiResponse<T>` 언래핑 규칙 준수

### 11. Frontend 화면 구현 - 일일보고

- [ ] `gw-home-ui/pages/work/daily-reports/index.vue` 또는 동등 경로 생성
- [ ] 작성 폼에 `작성일자`, `업무내용`, `진행상태`, `특이사항` 입력 UI 구현
- [ ] 날짜 기준 최신순 목록 UI 구현
- [ ] 기간 필터와 내용 검색 UI 구현
- [ ] 동일 날짜 기존 보고가 있을 때 수정 흐름으로 유도하는 UX 반영

### 12. Frontend 화면 구현 - 관리자

- [ ] 관리자 전용 일일보고 조회 화면 또는 탭 구성
- [ ] 회원별/기간별/상태별 조회 UI 구현
- [ ] 누락 날짜 확인 UI 구현
- [ ] 일반 사용자와 관리자 노출 경로가 섞이지 않도록 권한 분기 적용

### 13. Frontend 화면 구현 - 주간보고 작성

- [ ] 주간보고 작성 화면 생성
- [ ] 화면을 `주간보고 편집 패널`과 `해당 주 일일보고 참고 패널`의 2패널 구조로 구성
- [ ] 참고 패널에서 날짜, 상태, 업무내용, 특이사항을 날짜순으로 탐색 가능하게 구성
- [ ] 접기/펼치기 또는 선택 강조로 특정 일자의 내용을 빠르게 확인할 수 있게 구성
- [ ] 편집 패널에서 제목, 본문, 공개 여부, 임시저장, 게시 액션 제공
- [ ] AI 초안 생성 버튼은 선택 기능으로 제공하고, 생성 결과는 편집기에 삽입만 하도록 구현

### 14. 검증

- [ ] 본인만 일일보고 생성/수정 가능한지 확인
- [ ] 타인 UUID 직접 접근이 백엔드에서 차단되는지 확인
- [ ] 관리자 전체 조회 및 누락 조회가 정상 동작하는지 확인
- [ ] 동일 날짜 중복 작성 방지 동작 확인
- [ ] 주간보고 화면에서 같은 주 일일보고 참고 패널이 정상 동작하는지 확인
- [ ] AI 초안 생성 실패 시 수동 작성 흐름이 깨지지 않는지 확인
- [ ] 가능한 범위의 backend compile/test 검증 수행
- [ ] 가능한 범위의 frontend typecheck/build 검증 수행

---

## 마이그레이션 체크리스트

- [ ] 일일보고 DDL 작성
- [ ] 주간보고 DDL 작성 여부 및 시점 확정
- [ ] 기존 데이터 영향 검토
- [ ] NULL / DEFAULT 정책 확인
- [ ] 인덱스 영향 확인
- [ ] 데이터 마이그레이션 필요 여부 확인
- [ ] 운영 반영 순서 정의

---

## 작업 순서

1. 일일보고 DDL과 권한 정책을 먼저 확정한다.
2. 일일보고 backend API와 목록/작성 화면을 우선 구현한다.
3. 관리자 전체 조회와 누락 조회를 추가한다.
4. 주간보고 엔터티와 작성 화면의 기본 구조를 분리 구현한다.
5. 같은 주 일일보고 참고 패널을 붙인다.
6. 마지막 단계에서 OpenAI 보조 초안 기능을 선택적으로 연결한다.

---

## 완료 기준

- 사용자가 본인 일일보고를 날짜 기준으로 작성, 수정, 조회할 수 있다.
- 관리자가 전체 일일보고와 누락 현황을 조회할 수 있다.
- 일일보고는 개인용으로 유지되고 일반 사용자 간 공유되지 않는다.
- 주간보고는 별도 공개 보고로 분리되며 수동 작성이 가능하다.
- 주간보고 작성 화면에서 해당 주의 일일보고를 옆에서 참고해 정리할 수 있다.
- OpenAI를 사용하지 않아도 주간보고 작성 흐름이 완결되고, 사용할 경우에는 초안 보조로만 동작한다.
