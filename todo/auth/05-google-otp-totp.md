# Todo: Google OTP (TOTP) 2차 인증

> review: `review/auth/04-google-otp-totp.md`
> 작업 분류: **HEAVY** — DB → 백엔드 → 프론트 순서로 진행

---

## Step 1. DB 스키마 변경

- [x] `tb_mbr_acct_add_otp.sql` 생성 — ALTER + 롤백 SQL
  ```sql
  ALTER TABLE tb_mbr_acct
    ADD COLUMN otp_enabled        BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN otp_secret         TEXT,
    ADD COLUMN otp_fail_cnt       INT         NOT NULL DEFAULT 0,
    ADD COLUMN otp_last_failed_at TIMESTAMPTZ;
  ```
- [x] DB에 직접 실행 확인

---

## Step 2. Backend — share 모듈

- [x] `AcctVo.java` — OTP 필드 4개 추가
  - `otpEnabled: boolean`
  - `otpSecret: String`
  - `otpFailCnt: int`
  - `otpLastFailedAt: OffsetDateTime`

---

## Step 3. Backend — infra-db 모듈

- [x] `AccountMapper.java` — OTP 메서드 추가
  - `updateOtpSecret(idx, encryptedSecret)`
  - `enableOtp(idx)`
  - `disableOtp(idx)`
  - `incrementOtpFailCount(idx)`
  - `resetOtpFailCount(idx)`
- [x] `AccountMapper.xml` — 해당 SQL 작성
  - SELECT 쿼리에 OTP 컬럼 포함
  - 각 update SQL 작성

---

## Step 4. Backend — api 모듈 (util)

- [x] `OtpSecretEncryptor.java` 생성
  - AES-128/GCM, `@Value("${otp.encryption-key}")` 주입
  - `encrypt(plainSecret): String`
  - `decrypt(encryptedSecret): String`
- [x] `OtpTotpUtil.java` 생성
  - `com.eatthepath:java-otp:0.4.0` 의존성 추가 (`build.gradle`)
  - `generateSecret(): String` — Base32 랜덤 secret 생성
  - `buildOtpAuthUrl(loginId, secret): String` — `otpauth://totp/...` URL 생성
  - `verify(secret, code): boolean` — ±1 window 검증
- [x] `application.yml` — `otp.encryption-key` 항목 추가

---

## Step 5. Backend — JWT

- [x] `JwtProvider.java`
  - `generateOtpTempToken(loginId): String` — 5분 만료, `type: "otp_temp"`
  - `isOtpTempToken(token): boolean`
- [x] `JwtAuthenticationFilter.java`
  - `type == "otp_temp"` 토큰은 일반 인증 Principal 등록 제외 처리

---

## Step 6. Backend — DTO 신규 생성

- [x] `LoginResponse.java` — `loginStatus`, `tokenResponse`, `otpTempToken`
- [x] `OtpSetupResponse.java` — `otpAuthUrl`
- [x] `OtpVerifyRequest.java` — `otpCode`, `otpTempToken`
- [x] `OtpActivateRequest.java` — `otpCode`
- [x] `OtpDisableRequest.java` — `otpCode`
- [x] `OtpStatusResponse.java` — `otpEnabled`

---

## Step 7. Backend — AuthService

- [x] `login()` 반환 타입 `TokenResponse` → `LoginResponse` 변경
  - `otpEnabled=false` → `loginStatus: "SUCCESS"`, tokenResponse 채움
  - `otpEnabled=true` → `loginStatus: "OTP_REQUIRED"`, otpTempToken 채움
- [x] `otpSetup(loginId): OtpSetupResponse`
  - secret 생성 → 암호화 → DB 저장 (`updateOtpSecret`)
  - `otpauth://` URL 반환
- [x] `otpActivate(loginId, otpCode): void`
  - DB에서 암호화된 secret 조회 → 복호화 → TOTP 검증
  - 성공 시 `enableOtp(idx)`
- [x] `otpVerify(otpTempToken, otpCode): TokenResponse`
  - temp token 검증 (signature + type + 만료)
  - `otp_last_failed_at` 기반 30분 차단 여부 확인
  - TOTP 검증 → 성공 시 실 JWT 발급, `resetOtpFailCount`
  - 실패 시 `incrementOtpFailCount`, 5회 초과 시 `otp_last_failed_at` 갱신
- [x] `otpDisable(loginId, otpCode): void`
  - 현재 OTP 코드 검증 후 `disableOtp(idx)`, secret null 처리
- [x] `otpStatus(loginId): OtpStatusResponse`

---

## Step 8. Backend — AuthController + SecurityConfig

- [x] `AuthController.java` — OTP 엔드포인트 5개 추가
  - `POST /api/v1/auth/otp/setup`
  - `POST /api/v1/auth/otp/activate`
  - `POST /api/v1/auth/otp/verify`
  - `POST /api/v1/auth/otp/disable`
  - `GET /api/v1/auth/otp/status`
- [x] `SecurityConfig.java`
  - `POST /api/v1/auth/otp/verify` → `permitAll()` 추가

---

## Step 9. Frontend — 타입 + composable

- [x] `types/api/auth.ts` — `LoginApiResponse` 타입 추가
  - `login_status: 'SUCCESS' | 'OTP_REQUIRED'`
  - `token_response: TokenApiResponse | null`
  - `otp_temp_token: string | null`
- [x] `composables/use-auth.ts` — `login()` 수정
  - 응답 `login_status` 분기
  - `OTP_REQUIRED` 시 `{ status: 'OTP_REQUIRED', otpTempToken }` 반환
  - `SUCCESS` 시 기존처럼 토큰 저장 + 유저 정보 로드
- [x] `composables/useOtpApi.ts` 신규 생성
  - `fetchOtpStatus(): Promise<{ otpEnabled: boolean }>`
  - `setupOtp(): Promise<{ otpAuthUrl: string }>`
  - `activateOtp(otpCode: string): Promise<void>`
  - `verifyOtp(otpTempToken: string, otpCode: string): Promise<void>`
    - 성공 시 토큰 저장 + 유저 정보 로드
  - `disableOtp(otpCode: string): Promise<void>`

---

## Step 10. Frontend — OtpCodeInput 컴포넌트

- [x] `components/auth/OtpCodeInput.vue` 신규 생성
  - Props: `modelValue: string`, `disabled?: boolean`
  - 6자리 숫자 전용 input (single input, maxlength=6)
  - 숫자 외 입력 차단
  - 붙여넣기 지원
  - 6자리 완성 시 `complete` emit
  - 기존 `.input-field` 스타일 기반 + 가운데 정렬, letter-spacing

---

## Step 11. Frontend — 로그인 페이지 OTP 분기

- [x] `pages/login.vue` 수정
  - `loginStep: 'credentials' | 'otp'` ref 추가
  - `otpTempToken: string` ref 추가
  - `handleLogin()` — `OTP_REQUIRED` 응답 시 `loginStep = 'otp'` 전환
  - OTP 입력 단계 UI: `AuthOtpCodeInput` + 인증 버튼 + 에러 메시지
  - OTP 인증 성공 시 대시보드 이동
  - 뒤로가기(취소) 버튼 → `loginStep = 'credentials'` 복귀

---

## Step 12. Frontend — 보안 설정 페이지

- [x] `/settings` 보안 설정 섹션으로 통합 완료
  - `pages/settings/index.vue` 에서 OTP 상태 조회/설정/해제 처리
  - `AuthOtpCodeInput` + `useOtpApi` 재사용
- [x] `components/common/AppHeader.vue` — 보안 설정 네비게이션 링크를 `/settings` 기준으로 대체
