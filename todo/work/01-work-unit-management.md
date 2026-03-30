# Todo: 업무등록 기능 구현

> 연관 문서: [review/work/01-work-unit-management.md](../../review/work/01-work-unit-management.md)
> 작업 레벨: **HEAVY**
> 레이어: **Full-stack**

---

## 작업 범위

- 계정별 업무 마스터를 관리하는 `work` 도메인 신규 추가
- 업무 목록 조회, 등록, 수정, 사용안함 처리 API 구현
- 검색, 카테고리 필터, 상태 필터, 사용여부 필터를 포함한 `/work` 관리 화면 구현
- 이후 일일보고 화면에서 재사용할 수 있는 업무 선택용 옵션 API 구조 마련

### 범위 제외

- 일일보고 등록 기능 자체 구현
- 주간보고 취합 기능 구현
- AI 요약 기능 구현
- 계정 간 업무 공유, 협업, 권한 위임 기능

---

## 구현 체크리스트

### 1. 문서 및 범위 고정

- [x] review 문서 기준으로 API, DB, UI 범위 최종 확인
- [x] 작업 중 `work` 도메인 범위를 벗어나는 변경이 필요한지 수시 확인

### 2. DB 설계 및 마이그레이션

- [x] `gw-home-infra-db/src/main/resources/sql/ddl/work/tb_work_unit.sql` 작성
- [x] `tb_work_unit`에 `mbr_acct_idx`, `ttl`, `dscr`, `ctgr`, `sts`, `use_yn`, `use_cnt`, `last_used_at` 정의
- [x] `TIMESTAMPTZ`와 기본 감사 컬럼 규칙 준수 확인
- [x] 목록 조회용 인덱스와 최근 사용 정렬용 인덱스 정의
- [x] 동일 계정 내 업무명 중복 방지 정책 반영 여부 확인

### 3. Backend share 모델

- [x] `gw-home-share/src/main/java/com/gw/share/vo/work/WorkUnitVo.java` 생성
- [x] `gw-home-share/src/main/java/com/gw/share/vo/work/WorkUnitListSearchVo.java` 생성
- [x] `BaseVo` 상속 및 프로젝트 네이밍 규칙 준수 확인

### 4. Backend infra-db

- [x] `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/work/WorkUnitMapper.java` 생성
- [x] `gw-home-infra-db/src/main/resources/mapper/work/WorkUnitMapper.xml` 생성
- [x] 목록 조회 SQL에 `keyword`, `category`, `status`, `useYn`, `sort` 조건 반영
- [x] 모든 조회/수정 SQL에 현재 계정의 `mbr_acct_idx` 조건 강제
- [x] 비활성화는 `DELETE`가 아니라 `use_yn = 'N'` 업데이트로 처리
- [x] 옵션 조회 SQL은 일일보고 연계용 최소 필드만 반환하도록 구성

### 5. Backend API

- [x] `gw-home-api/src/main/java/com/gw/api/dto/work/` 하위 Request/Response DTO 생성
- [x] `gw-home-api/src/main/java/com/gw/api/service/work/WorkUnitService.java` 생성
- [x] `gw-home-api/src/main/java/com/gw/api/controller/work/WorkUnitController.java` 생성
- [x] `GET /api/v1/work-units` 구현
- [x] `POST /api/v1/work-units` 구현
- [x] `PUT /api/v1/work-units/{uuid}` 구현
- [x] `PUT /api/v1/work-units/{uuid}/use` 구현
- [x] `GET /api/v1/work-units/options` 구현
- [x] `uuid`만 외부 응답에 노출되고 `_idx`는 노출되지 않는지 확인
- [x] 업무 등록/수정 시 동일 계정 내 업무명 중복 검증 추가
- [x] 로그인 사용자 기준 본인 업무만 접근 가능한지 검증

### 6. Frontend 타입 및 API 연동

- [x] `gw-home-ui/types/work.ts` 생성
- [x] `gw-home-ui/composables/useWorkUnitApi.ts` 생성
- [x] `ApiResponse<T>` 언래핑 패턴을 기존 규칙대로 적용
- [x] 목록 조회 파라미터와 응답 타입을 검색/필터 구조에 맞게 정의
- [x] 등록, 수정, 사용안함 처리, 옵션 조회 메서드 구성

### 7. Frontend 화면 구현

- [x] `gw-home-ui/pages/work/index.vue` 생성
- [x] 필요 시 `gw-home-ui/components/work/WorkUnitForm.vue` 생성
- [x] 검색 입력, 카테고리 필터, 상태 필터, 비활성 포함 토글 UI 구현
- [x] 등록/수정 공용 폼 구현
- [x] 수정 시 기존 값 폼 프리필 동작 구현
- [x] 비활성 업무 기본 숨김 및 표시 전환 구현
- [x] 상태, 사용여부, 카테고리, 설명을 목록에서 확인 가능하게 구성
- [x] 추후 최근 사용/자주 사용 정렬을 붙일 수 있도록 정렬 선택 구조 확보

### 8. 검증

- [x] 등록 후 목록에서 즉시 반영되는지 확인
- [x] 수정 후 목록과 폼 상태가 정상 갱신되는지 확인
- [x] 사용안함 처리 후 기본 목록에서 숨김 또는 별도 표시되는지 확인
- [x] 동일 계정 중복 업무명 방지 동작 확인
- [x] 다른 계정 업무 접근 차단이 백엔드에서 보장되는지 확인
- [x] 가능한 범위의 backend 테스트 또는 compile 검증 수행
- [x] 가능한 범위의 frontend typecheck 또는 build 검증 수행

---

## 마이그레이션 체크리스트

- [x] DDL 변경 SQL 작성
- [x] 기존 데이터 영향 검토
- [x] NULL / DEFAULT 정책 확인
- [x] 인덱스 영향 확인
- [x] 롤백 SQL 작성 (운영 정책상 미작성)
- [x] 운영 반영 순서 정의
- [x] 데이터 마이그레이션 필요 여부 확인

---

## 작업 순서

1. DDL부터 확정한다.
2. VO, Mapper, Service, Controller 순서로 backend를 구현한다.
3. 타입, composable, `/work` 페이지 순서로 frontend를 구현한다.
4. 등록, 수정, 비활성화, 검색/필터, 계정 소유권 검증을 마지막에 확인한다.

---

## 완료 기준

- 계정별로 본인 업무 목록을 조회하고 관리할 수 있다.
- 업무 등록, 수정, 사용안함 처리가 모두 동작한다.
- 기본 목록에서 활성 업무 중심으로 탐색할 수 있다.
- 이후 일일보고 화면이 업무 선택 API를 재사용할 수 있는 구조가 준비된다.

---

## 추가 계획 (2026-03-30)

### 작업 범위

- `사용안함` 처리 실행 시 발생하는 에러 원인 분석 및 수정
- 프론트 payload, 백엔드 검증, mapper update 흐름 정합성 재점검

### 구현 체크리스트

#### 9. 사용안함 처리 오류 수정

- [x] `/work` 화면에서 `useYn` 전송값과 요청 시점 상태를 확인
- [x] `useWorkUnitApi.ts` 의 `updateWorkUnitUse` payload 구조 점검
- [x] `UpdateWorkUnitUseRequest` 검증 조건과 허용값 점검
- [x] `WorkUnitService` 가 비활성화 요청을 정상 처리하는지 확인
- [x] `WorkUnitMapper.xml` 의 사용여부 업데이트 SQL 및 where 조건 점검
- [x] 비활성화 후 목록 갱신과 성공/실패 토스트 흐름 점검

#### 10. 검증

- [x] 활성 업무를 사용안함 처리할 때 에러 없이 완료되는지 확인
- [x] 사용안함 처리 후 기본 목록에서 숨김 또는 비활성 표시가 일관된지 확인
- [x] 이미 비활성인 업무 재요청 시 예외 처리 방식 확인
- [x] 가능한 범위의 backend compile 또는 관련 검증 수행

### 추가 완료 기준

- `사용안함` 처리 API와 화면 액션이 동일 계약으로 동작한다.
- 사용안함 처리 시 서버 오류나 화면 예외가 발생하지 않는다.
- 처리 후 목록 상태와 사용자 피드백이 일관된다.
