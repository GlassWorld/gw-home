# Review: 로그인 잠금 / OTP 강제 등록 / 관리자 초기화 기능

> 작업 분류: **HEAVY**
> 인증 흐름 변경 / 관리자 계정관리 확장 / Frontend 로그인 UX 변경 / 임시 비밀번호 발급

---

## 작업 개요

다음 요구사항을 하나의 인증/관리 시나리오로 묶어서 정리한다.

- 비밀번호 5회 오류 시 계정 잠금
- 로그인 시 OTP 미등록 계정은 OTP 등록 모달로 유도
- OTP 인증도 5회 제한
- 관리자는 계정관리 화면에서 관련 상태 초기화 가능
- 비밀번호 초기화 시 임시 비밀번호를 관리자에게 보여주고 관리자가 직접 전달

---

## 현재 상태

### 이미 구현된 항목

1. 비밀번호 5회 오류 잠금
- `AuthService.login()` 에서 `MAX_LOGIN_FAIL_COUNT = 5`
- 실패 시 `lgn_fail_cnt` 증가
- 5회 이상이면 `lck_yn = true`, `lck_at = now()`

2. OTP 5회 제한
- `AuthService.otpVerify()` 에서 `MAX_OTP_FAIL_COUNT = 5`
- 실패 시 `otp_fail_cnt` 증가
- 5회 이상이면 30분 차단 (`otp_last_failed_at` 기준)

3. 관리자 계정관리 기본 기능
- 관리자 목록 조회 / 단건 조회 / 생성 / 권한 변경 / 상태 변경 / 삭제는 이미 구현됨

### 아직 없는 항목

1. 로그인 성공 후 OTP 미등록 강제 등록 분기
- 현재는 OTP 활성화된 계정만 `OTP_REQUIRED`
- OTP 미등록 계정을 별도 상태로 내려주는 구조 없음

2. 관리자 초기화 기능
- 로그인 잠금 해제
- OTP 실패 횟수 초기화
- OTP 비활성화/재등록 유도
- 비밀번호 임시 초기화

3. 프론트 로그인 후 OTP 등록 강제 모달
- 현재 로그인 화면은 `SUCCESS` / `OTP_REQUIRED` 만 처리하는 구조

---

## 설계 결정 사항

### 1. 비밀번호 5회 잠금

현재 구현 유지가 적절함.

- 추가 작업 불필요
- 관리자 초기화 기능만 보강

### 2. OTP 미등록 강제 등록

로그인 시 아래 3단계 상태로 나누는 것이 가장 자연스럽다.

- `SUCCESS`
- `OTP_REQUIRED`
- `OTP_SETUP_REQUIRED`

권장 흐름:

1. 아이디/비밀번호 검증 성공
2. 계정 잠금/비활성 상태 확인
3. `otp_enabled = true` 이면 `OTP_REQUIRED`
4. `otp_enabled = false` 이면 `OTP_SETUP_REQUIRED`
5. 프론트는 로그인 직후 OTP 등록 모달을 강제로 열고 닫기 제한

주의:
- 현재 `otp_enabled = false` 이고 `otp_secret = null` 인 계정이 대부분일 수 있음
- 강제 등록 정책 적용 시 기존 사용자 전체가 첫 로그인에 OTP 등록 플로우로 들어감

### 3. OTP 5회 제한

현재 구현 유지가 적절함.

다만 관리자가 아래 정보를 해제할 수 있어야 한다.

- `otp_fail_cnt = 0`
- `otp_last_failed_at = null`

필요 시 같이 고려할 수 있는 선택지:

- OTP 잠금만 해제
- OTP 자체 비활성화 (`otp_enabled = false`, `otp_secret = null`)

권장안:
- "OTP 잠금 초기화"와 "OTP 재등록 초기화"를 분리

### 4. 관리자 초기화 기능

계정관리 액션을 다음처럼 분리하는 것이 안전하다.

1. 로그인 잠금 초기화
- `lgn_fail_cnt = 0`
- `lck_yn = false`
- `lck_at = null`

2. OTP 실패 초기화
- `otp_fail_cnt = 0`
- `otp_last_failed_at = null`

3. OTP 재등록 초기화
- `otp_enabled = false`
- `otp_secret = null`
- `otp_fail_cnt = 0`
- `otp_last_failed_at = null`

4. 비밀번호 임시 초기화
- 서버가 임시 비밀번호 생성
- BCrypt 저장
- 응답 본문에 **1회 표시용 평문 임시 비밀번호** 반환
- 관리자가 복사해 사용자에게 전달

### 5. 임시 비밀번호 처리 방식

별도 테이블 없이 즉시 교체 방식이 범위 대비 가장 단순하다.

권장 규칙:

- 길이 10~12자
- 영문 대소문자 + 숫자 + 특수문자 일부 포함
- API 응답으로 평문을 **한 번만** 전달
- 서버 로그에는 평문 절대 남기지 않음

추가로 권장되는 후속 정책:

- 임시 비밀번호 로그인 후 즉시 비밀번호 변경 강제

하지만 이건 현재 범위를 넘길 수 있으므로 이번 작업에서는 제외 가능

---

## 영향 범위

### Backend

변경 예상 파일:

- `gw-home-api/.../service/auth/AuthService.java`
- `gw-home-api/.../dto/auth/LoginResponse.java`
- `gw-home-api/.../controller/auth/AuthController.java` 영향 가능
- `gw-home-api/.../service/account/AdminAccountService.java`
- `gw-home-api/.../controller/admin/AdminAccountController.java`
- 관리자 계정 DTO 추가 필요
- `gw-home-infra-db/.../mapper/account/AccountMapper.java`
- `gw-home-infra-db/.../mapper/account/AccountMapper.xml`

추가 메서드 예상:

- `unlockAccount(...)`
- `resetOtpFailure(...)`
- `resetOtpSetup(...)`
- `resetPasswordTemp(...)`

### Frontend

변경 예상 파일:

- `gw-home-ui/composables/use-auth.ts`
- `gw-home-ui/types/api/auth.ts`
- `gw-home-ui/pages/login.vue` 또는 로그인 폼 컴포넌트
- `gw-home-ui/pages/admin/accounts/index.vue`
- `gw-home-ui/composables/useAdminAccountApi.ts`
- `gw-home-ui/types/admin.ts`

필요 UI:

- 로그인 후 OTP 등록 모달
- 관리자 계정관리 액션 버튼들
- 임시 비밀번호 표시용 확인 모달

### DB

신규 컬럼 추가는 꼭 필요하지 않음.

현재 컬럼으로 처리 가능한 항목:

- 비밀번호 잠금: `lgn_fail_cnt`, `lck_yn`, `lck_at`
- OTP 실패 제한: `otp_fail_cnt`, `otp_last_failed_at`
- OTP 등록 상태: `otp_enabled`, `otp_secret`

즉, 이번 요구사항은 **DDL 없이도 진행 가능한 가능성이 높음**

---

## 리스크 분석

### 1. OTP 강제 등록 정책의 운영 충격

- 기존 모든 사용자가 다음 로그인부터 OTP 설정을 강제당할 수 있음
- 사용자 공지 없이 적용하면 문의가 급증할 수 있음

### 2. 임시 비밀번호 평문 노출

- 관리자 화면에 보여주는 순간이 가장 민감한 구간
- 토스트보다 모달/명시적 확인이 적절
- 브라우저 콘솔/서버 로그/에러 응답에 평문이 남지 않게 주의 필요

### 3. 자기 자신 초기화

- 관리자가 자기 자신의 OTP/비밀번호를 초기화하는 것은 허용 가능 여부를 정해야 함
- 권한/상태/삭제와 달리 비밀번호 초기화는 자기 자신 허용 여부가 업무 정책에 따라 달라짐

권장:
- 자기 자신 비밀번호 초기화는 허용 가능
- 자기 자신 계정 잠금 초기화도 허용 가능
- 다만 자기 자신 상태 비활성화/삭제는 계속 금지

### 4. 로그인 응답 계약 변경

- `LoginResponse` 에 새 상태값이 추가되면 프론트 로그인 처리도 반드시 같이 수정되어야 함

---

## 권장 구현 순서

1. `LoginResponse` / `AuthService.login()` 에 `OTP_SETUP_REQUIRED` 추가
2. 프론트 로그인 플로우에 OTP 등록 강제 모달 연결
3. 관리자 계정관리 API에 초기화 액션 추가
4. 관리자 UI에 초기화 버튼 및 결과 모달 추가
5. 보안 점검: 임시 비밀번호 로그 미노출 확인

---

## 권장 API 방향

### 로그인 응답

기존:

- `SUCCESS`
- `OTP_REQUIRED`

변경:

- `SUCCESS`
- `OTP_REQUIRED`
- `OTP_SETUP_REQUIRED`

### 관리자 계정 액션

권장 엔드포인트:

- `PUT /api/v1/admin/accounts/{uuid}/unlock`
- `PUT /api/v1/admin/accounts/{uuid}/otp-failure/reset`
- `PUT /api/v1/admin/accounts/{uuid}/otp/reset`
- `PUT /api/v1/admin/accounts/{uuid}/password/reset`

비밀번호 초기화 응답 예:

- `temporaryPassword`

---

## 결론

이 작업은 HEAVY이며, 핵심 포인트는 두 가지다.

1. 비밀번호 5회 잠금과 OTP 5회 제한은 이미 구현되어 있음
2. 실제 신규 작업은 "OTP 미등록 강제 등록 플로우"와 "관리자 초기화 기능" 쪽임

권장안은 아래와 같다.

- 로그인 응답에 `OTP_SETUP_REQUIRED` 추가
- 로그인 직후 OTP 등록 강제 모달 연결
- 관리자 계정관리에서 잠금 초기화 / OTP 실패 초기화 / OTP 재등록 초기화 / 임시 비밀번호 발급 추가

