# Todo: 공통 변환기 및 예외 메시지 리팩토링

> 연관 문서: [review/work/03-overall-refactor-check.md](../../review/work/03-overall-refactor-check.md)
> 작업 레벨: **HEAVY**
> 레이어: **Backend**

---

## 작업 범위

- 서비스 내부에 흩어진 `toResponse`, `toDetailResponse`, `toSummaryResponse` 계열 메서드를 별도 `convert` 자바 클래스로 분리
- 도메인별 변환 책임을 서비스에서 제거하고 변환 클래스로 위임
- `BusinessException`의 외부 노출 메시지를 `ErrorCode` 공통 메시지 기준으로 정리
- 서비스 내부에 반복되는 `validate`, `normalize`, 기본 문자열/날짜 검증 함수를 `share` 공통화 후보로 정리
- 날짜 처리, 형변환, null 체크 같은 범용 로직을 `share.util` 공통 유틸로 정리
- 서비스 메서드의 한글 주석과 진입/완료/실패 로그를 프로젝트 규칙 기준으로 정리
- MyBatis `snake_case -> camelCase` 자동 매핑 기준으로 불필요한 Mapper alias를 정리
- 필요 시 상세 예외 문구는 로그용 상세 메시지로만 분리
- 예외 공통화에 맞춰 테스트 기대값 정리

### 현재 요청 기준 우선 범위

- `work` 도메인 변환기 분리
  - `DailyReportService`
  - `WeeklyReportService`
  - `WorkUnitService`
- 공통 예외 구조 정리
  - `ErrorCode`
  - `BusinessException`
  - `GlobalExceptionHandler`
- 공통 검증 함수 정리
  - 문자열 필수값 검증
  - 공백 제거/빈값 null 처리
  - 날짜 범위 기본 검증
  - enum/상태값 기본 검증
- `share.util` 공통 유틸 정리
  - 날짜 처리 유틸
  - 기본 형변환 유틸
  - null 체크 유틸
- 서비스 한글 주석 및 로그 정리
  - 메서드 상단 한글 목적 주석
  - 진입 로그
  - 정상 완료 로그
  - 예외 실패 로그
- Mapper alias 정리
  - `idx`, `uuid`는 유지
  - 자동 camelCase 매핑 가능한 alias는 제거 후보 검토
- 예외 메시지 공통화 영향 테스트 정리

### 현재 요청 기준 후속 범위

- `account`, `admin`, `board`, `comment`, `vault`, `profile`, `file`, `tag` 도메인까지 변환기 패턴 확장
- 도메인별 `convert` 패키지 규칙 고정
- 커스텀 예외 상세 메시지 로그 포맷 통일
- 공통 검증 유틸을 각 도메인 서비스 전반으로 확장 적용

### 현재 요청 기준 범위 제외

- API 스펙 변경
- 프론트 에러 UX 변경
- DB 스키마 변경
- 문서 전면 개편

---

## 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| Backend - share | `ErrorCode`, `BusinessException`, `GlobalExceptionHandler` |
| Backend - share | 공통 검증/정규화 유틸 또는 support 클래스 추가 가능 |
| Backend - share/util | 날짜 처리, 형변환, null 체크 유틸 추가 가능 |
| Backend - api/service/work | 서비스 내부 응답 변환 메서드 제거 |
| Backend - api/service/work | 서비스 한글 주석 및 로그 정리 |
| Backend - api/convert/work | `DailyReportConvert`, `WeeklyReportConvert`, `WorkUnitConvert` 신규 또는 유사 구조 추가 |
| Backend - infra-db/mapper/work | 불필요한 alias 제거 및 필요한 alias 기준 정리 |
| Test | 예외 메시지 기대값, 변환기 분리, 공통 검증 함수 적용 후 테스트 정리 |

---

## 설계 방향

### 1. 변환 책임은 서비스에서 분리한다

- 서비스는 조회, 검증, 저장, 권한 판단에 집중한다.
- DTO 변환은 도메인별 `convert` 클래스가 담당한다.
- 서비스 내부 private 변환 메서드는 제거하거나 최소화한다.

### 2. 변환기는 도메인 단위로 관리한다

- 공통 범용 변환기 하나로 합치지 않는다.
- `work` 도메인 안에서는 일일보고, 주간보고, 업무등록 변환 책임을 명확히 분리한다.
- 클래스명은 `*Convert` 또는 팀 규칙에 맞는 동일 접미사로 통일한다.

### 3. 외부 에러 메시지는 `ErrorCode`를 기준으로 통일한다

- API 응답 메시지는 `ErrorCode.getMessage()`를 기준으로 고정한다.
- 개별 상황 설명이 필요하면 별도 상세 메시지를 보관하되, 로그에서만 확인 가능하도록 정리한다.
- 테스트도 상세 문구가 아니라 공통 메시지 기준으로 바꾼다.

### 4. 상세 문구가 꼭 필요한 경우는 기준을 분리한다

- 사용자에게 직접 노출할 공통 메시지
- 로그 및 디버깅용 상세 메시지

위 두 메시지의 책임을 섞지 않는다.

### 5. 기본 검증 함수는 재사용 가능한 것만 `share`로 올린다

- 도메인 의미가 없는 기본 검증만 공통화한다.
- 예:
  - 필수 문자열 검증
  - 공백 제거 후 null 변환
  - 시작일/종료일 순서 검증
  - 공통 enum 후보값 검증
- 반대로 도메인 의미가 강한 검증은 각 서비스에 남긴다.
- 예:
  - 업무명 중복 검증
  - 본인 글 수정 가능 여부 검증
  - 주간보고 기간 7일 제한 같은 정책성 검증

즉 `share`에는 "기본 도구"만 올리고, "업무 규칙"은 도메인 서비스에 남기는 방향으로 정리한다.

### 6. 날짜 처리, 형변환, null 체크는 `share.util`로 모은다

- 여러 서비스에서 반복되는 범용 유틸은 `share.util`에 둔다.
- 예:
  - 날짜 비교/범위 확인
  - 문자열/숫자/날짜 기본 형변환 보조
  - null 안전 처리
  - 빈 문자열 정리
- 서비스는 유틸을 조합해 쓰고, 정책 판단은 직접 유지한다.

즉 `share.util`은 "기본 도구 상자" 역할만 하고, 비즈니스 규칙은 넣지 않는다.

### 7. 서비스 주석과 로그는 구조 정리와 함께 맞춘다

- 리팩토링으로 메서드 책임이 바뀌면 주석과 로그도 같이 갱신한다.
- 메서드 상단에는 목적을 설명하는 한글 주석을 둔다.
- 서비스 메서드에는 진입, 정상 완료, 예외 실패 로그를 팀 규칙에 맞게 정리한다.
- 공통화된 검증 함수 호출로 실패할 수 있는 지점도 로그 문맥에서 추적 가능해야 한다.

### 8. Mapper alias는 자동 매핑 설정을 기준으로 최소화한다

- MyBatis 설정에 `mapUnderscoreToCamelCase(true)`가 있으므로 snake_case와 camelCase가 일대일 대응되는 alias는 제거 후보다.
- 예:
  - `use_yn AS useYn`
  - `use_cnt AS useCnt`
  - `last_used_at AS lastUsedAt`
  - `created_by AS createdBy`
  - `updated_by AS updatedBy`
  - `created_at AS createdAt`
  - `updated_at AS updatedAt`
- 반대로 `BaseVo`의 `idx`, `uuid`처럼 컬럼명과 필드명이 직접 대응되지 않는 경우는 alias를 유지한다.
- 예:
  - `work_unit_idx AS idx`
  - `work_unit_uuid AS uuid`

즉 "자동으로 되는 건 맡기고, 구조적으로 필요한 alias만 남긴다"는 기준으로 정리한다.

---

## 구현 체크리스트

### 1. 예외 공통화 기준 확정

- [x] `ErrorCode` 공통 메시지를 API 응답 기준 메시지로 확정
- [x] `BusinessException` 생성자 정책 확정
- [x] 상세 메시지 유지 방식 결정
- [x] `GlobalExceptionHandler` 응답/로그 정책 정리

### 2. 공통 검증 함수 후보 정리

- [x] `work` 서비스 내부의 `normalizeText`, `require*`, `validate*` 메서드 목록 정리
- [x] 도메인 중립 함수와 도메인 종속 함수를 분리
- [x] `share`로 올릴 공통 검증/support 클래스 위치 확정
- [x] 함수명/예외 처리 방식 통일
- [x] 공통화 후 서비스 가독성 개선 기준 확인

### 3. `share.util` 공통 유틸 후보 정리

- [x] 날짜 처리 유틸 후보 목록 정리
- [x] 기본 형변환 유틸 후보 목록 정리
- [x] null 체크 유틸 후보 목록 정리
- [x] `share.util` 패키지 구조 확정
- [x] 도메인 정책이 섞이지 않도록 경계 기준 정리

### 4. work 도메인 변환기 분리

- [x] `DailyReportService`의 `toResponse`, `toAdminResponse`, `toWorkUnitResponses` 분리
- [x] `WeeklyReportService`의 `toWeeklyResponse`, `toDailySourceResponse`, `toWorkUnitResponses` 분리
- [x] `WorkUnitService`의 `toResponse`, `toOptionResponse` 분리
- [x] 서비스는 convert 호출만 하도록 정리

### 5. 서비스 주석 및 로그 정리

- [x] `work` 도메인 서비스 메서드 상단 한글 주석 정리
- [x] 진입 로그 형식 통일
- [x] 정상 완료 로그 형식 통일
- [x] 예외 실패 로그 형식 통일
- [x] 공통화된 검증/변환 흐름이 로그에서 드러나도록 정리

### 6. Mapper alias 정리

- [x] `work` 도메인 mapper XML에서 자동 매핑 가능한 alias 목록 정리
- [x] `idx`, `uuid`처럼 유지해야 하는 alias 기준 확정
- [x] 불필요한 alias 제거 후 가독성 확인
- [x] mapper 규칙을 문서화할지 검토

### 7. 패키지 구조 정리

- [x] `com.gw.api.convert.work` 패키지 추가 여부 확정
- [x] 공통 검증 클래스의 `share` 패키지 위치 확정
- [x] 공통 유틸 클래스의 `share.util` 패키지 위치 확정
- [x] 파일명/클래스명 규칙 통일
- [x] 주입 방식(`static util` vs `@Component`) 결정

### 8. 테스트 정리

- [x] 예외 메시지 공통화에 맞춰 테스트 기대값 수정
- [x] 공통 검증 함수 적용 후 테스트 기대값 영향 확인
- [x] `share.util` 공통 유틸 적용 후 테스트 영향 확인
- [x] mapper alias 정리 후 조회 결과 회귀 확인
- [x] 변환기 분리 후 서비스 테스트 회귀 확인
- [x] 가능하면 share/api 테스트 우선 수행

### 9. 후속 확장 메모

- [x] 다른 도메인 확장 대상 목록 정리
- [x] `toResponse` 잔존 서비스 목록 정리
- [x] `validate`/`normalize` 잔존 서비스 목록 정리
- [x] `share.util`로 올릴 유틸 잔존 목록 정리
- [x] 주석/로그 정비가 필요한 서비스 목록 정리
- [x] alias 정리가 필요한 mapper 목록 정리
- [x] 공통 리팩토링 패턴 문서화 여부 검토

---

## 진행 결과

- `work` 도메인 서비스의 응답 변환 책임을 `com.gw.api.convert.work` 패키지로 분리했다.
- `BusinessException`은 외부 응답 메시지를 `ErrorCode` 기준으로 통일하고, 상세 문구는 로그용으로 분리했다.
- `share.util`에 문자열 정리, 기본 검증, 날짜 검증, 형변환 유틸을 추가했다.
- `DailyReportService`, `WeeklyReportService`, `WorkUnitService`의 한글 주석과 진입/완료/실패 로그를 정리했다.
- `work` mapper XML에서 snake_case -> camelCase 자동 매핑 가능한 alias를 제거하고 `idx`, `uuid` alias만 유지했다.
- 관련 테스트 기대값을 현재 정책에 맞게 정리했다.
- Mapper alias 기준과 `convert`/`share.util` 리팩토링 패턴을 공통 문서에 반영했다.

## 검증 결과

- [x] `:gw-home-api:compileJava`
- [x] `:gw-home-infra-db:compileJava`
- [x] `:gw-home-share:compileJava`
- [x] `:gw-home-share:test --tests com.gw.share.common.handler.GlobalExceptionHandlerTest`
- [x] `:gw-home-api:test --tests com.gw.api.auth.AuthServiceTest --tests com.gw.api.vault.VaultServiceTest`

---

## 후보 대상 서비스 메모

### 1차 우선 대상

- `gw-home-api/src/main/java/com/gw/api/service/work/DailyReportService.java`
- `gw-home-api/src/main/java/com/gw/api/service/work/WeeklyReportService.java`
- `gw-home-api/src/main/java/com/gw/api/service/work/WorkUnitService.java`

### 2차 확장 후보

- `gw-home-api/src/main/java/com/gw/api/service/account/AccountService.java`
- `gw-home-api/src/main/java/com/gw/api/service/account/AdminAccountService.java`
- `gw-home-api/src/main/java/com/gw/api/service/board/BoardService.java`
- `gw-home-api/src/main/java/com/gw/api/service/comment/CommentService.java`
- `gw-home-api/src/main/java/com/gw/api/service/vault/VaultService.java`
- `gw-home-api/src/main/java/com/gw/api/service/vault/VaultCategoryService.java`
- `gw-home-api/src/main/java/com/gw/api/service/profile/ProfileService.java`
- `gw-home-api/src/main/java/com/gw/api/service/file/FileService.java`
- `gw-home-api/src/main/java/com/gw/api/service/tag/TagService.java`
- `gw-home-api/src/main/java/com/gw/api/service/admin/AdminService.java`

---

## 작업 순서

1. `BusinessException`과 `GlobalExceptionHandler`의 메시지 정책을 먼저 확정한다.
2. `work` 도메인 서비스의 `validate`, `normalize`, `require` 메서드를 분류해 공통화 대상을 고른다.
3. 날짜 처리, 형변환, null 체크 중 `share.util`로 올릴 범용 유틸을 확정한다.
4. `work` 도메인 변환 메서드를 분석해 `convert` 클래스 구조를 정한다.
5. 일일보고 → 주간보고 → 업무등록 순으로 변환기를 분리한다.
6. 공통화 가능한 기본 검증 함수와 범용 유틸을 `share`/`share.util`로 올리고 서비스 호출로 치환한다.
7. 리팩토링된 서비스 메서드 기준으로 한글 주석과 로그를 정리한다.
8. `work` 도메인 mapper XML에서 자동 매핑 가능한 alias를 걷어내고 필요한 alias만 남긴다.
9. 테스트 기대값과 예외 메시지를 공통 기준으로 정리한다.
10. 이후 다른 도메인 확장 여부를 별도 작업으로 판단한다.

---

## 완료 기준

- `work` 도메인 서비스 내부의 응답 변환 메서드가 별도 `convert` 클래스로 분리되어 있다.
- `work` 도메인 서비스 내부의 기본 `validate`/`normalize` 함수 중 공통화 가능한 항목이 `share`로 정리되어 있다.
- 날짜 처리, 형변환, null 체크 같은 범용 로직이 `share.util`로 정리되어 있다.
- 서비스는 변환 책임 없이 비즈니스 로직 중심으로 정리되어 있다.
- `work` 도메인 서비스 메서드의 한글 주석과 진입/완료/실패 로그가 규칙에 맞게 정리되어 있다.
- `work` 도메인 mapper XML에서 자동 매핑 가능한 alias는 제거되고, `idx`/`uuid`처럼 필요한 alias만 유지되어 있다.
- `BusinessException` 외부 메시지는 `ErrorCode` 공통 메시지 기준으로 동작한다.
- 테스트가 공통 메시지 기준과 분리된 변환 구조를 반영한다.
- 나머지 도메인 확장 대상이 후속 범위로 정리되어 있다.
