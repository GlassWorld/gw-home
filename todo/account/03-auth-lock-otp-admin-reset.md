# Todo: 로그인 잠금 / OTP 강제 등록 / 관리자 초기화 기능

> 작업 분류: **HEAVY**
> 관련 리뷰: `review/account/03-auth-lock-otp-admin-reset.md`

---

## Step 1 — Backend: 로그인 응답 상태 확장

- [ ] `LoginResponse.java` 에 `OTP_SETUP_REQUIRED` 상태를 표현할 수 있도록 응답 구조 검토 및 반영
- [ ] 필요 시 로그인 응답 관련 프론트 타입과 일치하도록 필드명 점검

---

## Step 2 — Backend: AuthService 로그인 분기 수정

- [ ] `AuthService.java`
  - [ ] 비밀번호 검증 성공 후 `otp_enabled = true` 이면 `OTP_REQUIRED`
  - [ ] `otp_enabled = false` 이면 `OTP_SETUP_REQUIRED`
  - [ ] 기존 `INACTIVE` 차단 / 비밀번호 5회 잠금 / OTP 5회 제한 로직과 충돌 없는지 확인

---

## Step 3 — Backend: AccountMapper 관리자 초기화 메서드 추가

- [ ] `AccountMapper.java` 메서드 추가
  - [ ] `unlockAccountByUuid(String uuid, String updatedBy)`
  - [ ] `resetOtpFailureByUuid(String uuid, String updatedBy)`
  - [ ] `resetOtpByUuid(String uuid, String updatedBy)`
  - [ ] `updatePasswordByUuid(String uuid, String password, String updatedBy)`
- [ ] `AccountMapper.xml` SQL 추가
  - [ ] 로그인 잠금 초기화 SQL
  - [ ] OTP 실패 카운트 초기화 SQL
  - [ ] OTP 재등록 초기화 SQL
  - [ ] UUID 기준 비밀번호 변경 SQL

---

## Step 4 — Backend: 관리자 계정 DTO 추가

- [ ] `AdminPasswordResetResponse.java` 생성
  - [ ] `temporaryPassword`

---

## Step 5 — Backend: AdminAccountService 확장

- [ ] `AdminAccountService.java`
  - [ ] `unlockAccount(uuid, adminLoginId)`
  - [ ] `resetOtpFailure(uuid, adminLoginId)`
  - [ ] `resetOtp(uuid, adminLoginId)`
  - [ ] `resetPassword(uuid, adminLoginId)` 구현
- [ ] 자기 자신 상태/삭제 금지 정책과 별도로 초기화 액션의 자기 자신 허용 여부 결정 반영
- [ ] 임시 비밀번호 생성 로직 추가
  - [ ] 영문/숫자/특수문자 조합
  - [ ] 서버 로그에 평문 비밀번호 노출 금지

---

## Step 6 — Backend: AdminAccountController 액션 추가

- [ ] `AdminAccountController.java`
  - [ ] `PUT /api/v1/admin/accounts/{uuid}/unlock`
  - [ ] `PUT /api/v1/admin/accounts/{uuid}/otp-failure/reset`
  - [ ] `PUT /api/v1/admin/accounts/{uuid}/otp/reset`
  - [ ] `PUT /api/v1/admin/accounts/{uuid}/password/reset`

---

## Step 7 — Frontend: 로그인 응답 타입/플로우 반영

- [ ] `types/api/auth.ts` 또는 관련 타입 파일
  - [ ] `OTP_SETUP_REQUIRED` 상태 반영
- [ ] `use-auth.ts`
  - [ ] `OTP_SETUP_REQUIRED` 분기 추가
  - [ ] OTP 설정 필요 상태를 UI에서 사용할 수 있도록 반환 형태 확장

---

## Step 8 — Frontend: 로그인 후 OTP 등록 강제 모달

- [ ] `pages/login.vue` 또는 로그인 폼 컴포넌트 수정
  - [ ] OTP 미등록 로그인 성공 시 설정 모달 오픈
  - [ ] QR 코드 / 수동 등록 URL / OTP 코드 입력 UI 연결
  - [ ] 등록 완료 전 로그인 완료 처리하지 않도록 흐름 정리

---

## Step 9 — Frontend: 관리자 계정관리 API 확장

- [ ] `types/admin.ts`
  - [ ] 비밀번호 초기화 응답 타입 추가
- [ ] `useAdminAccountApi.ts`
  - [ ] `unlockAccount(uuid)`
  - [ ] `resetOtpFailure(uuid)`
  - [ ] `resetOtp(uuid)`
  - [ ] `resetPassword(uuid)`

---

## Step 10 — Frontend: 관리자 계정관리 UI 확장

- [ ] `pages/admin/accounts/index.vue`
  - [ ] 행별 초기화 액션 UI 추가
  - [ ] 로그인 잠금 초기화 버튼
  - [ ] OTP 실패 초기화 버튼
  - [ ] OTP 재등록 초기화 버튼
  - [ ] 비밀번호 초기화 버튼
  - [ ] 임시 비밀번호 표시 모달 추가
  - [ ] 임시 비밀번호 복사 UX 추가

---

## Step 11 — 검증

- [ ] Backend compile
  - [ ] `:gw-home-api:compileJava`
  - [ ] `:gw-home-infra-db:compileJava`
  - [ ] `:gw-home-share:compileJava`
- [ ] Frontend typecheck
  - [ ] `npm run typecheck`
- [ ] 수동 흐름 점검
  - [ ] 비밀번호 5회 오류 잠금
  - [ ] OTP 5회 제한
  - [ ] OTP 미등록 로그인 시 등록 유도
  - [ ] 관리자 초기화 액션 동작

---

## 정책 확인 메모

- [ ] 관리자 자기 자신 계정 잠금 초기화 허용 여부
- [ ] 관리자 자기 자신 OTP 실패 초기화 허용 여부
- [ ] 관리자 자기 자신 OTP 재등록 초기화 허용 여부
- [ ] 관리자 자기 자신 비밀번호 임시 초기화 허용 여부
- [ ] 임시 비밀번호 로그인 후 강제 변경 정책은 이번 범위에 포함할지 여부

