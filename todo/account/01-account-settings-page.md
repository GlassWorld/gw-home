# Todo: 계정 설정 페이지

> review: `review/account/01-account-settings-page.md`
> 작업 분류: **NORMAL**
>
> ⚠️ OTP 섹션은 `todo/auth/05-google-otp-totp.md` Step 9, 10 완료 후 연결
> ⚠️ `todo/auth/05-google-otp-totp.md` Step 12 (`/security/index.vue`) 는 이 작업으로 대체 — 완료 처리 필요

---

## Step 1. Backend — Mapper

- [x] `AccountMapper.java` — `updatePassword(idx, pwd)` 추가
- [x] `AccountMapper.xml` — `updatePassword` SQL 추가
  ```sql
  UPDATE tb_mbr_acct
  SET pwd = #{pwd}, updated_by = #{updatedBy}, updated_at = now()
  WHERE mbr_acct_idx = #{idx} AND del_at IS NULL
  ```

---

## Step 2. Backend — DTO

- [x] `ChangePasswordRequest.java` 신규 생성
  - `currentPassword: String` — `@NotBlank`
  - `newPassword: String` — `@NotBlank`, `@Size(min=8, max=100)`

---

## Step 3. Backend — AccountService

- [x] `changePassword(loginId, currentPassword, newPassword): void`
  - 계정 조회
  - `passwordEncoder.matches(currentPassword, account.getPwd())` 검증 실패 시 `UNAUTHORIZED`
  - 새 비밀번호 = 현재 비밀번호 동일 시 `BusinessException(DUPLICATE, "현재 비밀번호와 동일합니다.")`
  - `updatePassword(idx, passwordEncoder.encode(newPassword))` 호출

---

## Step 4. Backend — AccountController

- [x] `PUT /api/v1/accounts/me/password` 추가
  ```java
  @PutMapping("/me/password")
  public ApiResponse<Void> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request)
  ```

---

## Step 5. Frontend — composable

- [x] `composables/useSettingsApi.ts` 신규 생성
  - `changeNickname(nickname: string): Promise<void>`
    - `PUT /api/v1/profiles/me` 호출, `{ nickname }` 만 전송
    - 성공 시 `authStore.currentUser.nickname` 갱신
  - `changePassword(currentPassword: string, newPassword: string): Promise<void>`
    - `PUT /api/v1/accounts/me/password` 호출

---

## Step 6. Frontend — 설정 페이지

- [x] `pages/settings/index.vue` 신규 생성
  - `middleware: 'auth'`
  - 3개 섹션 구조: 이름 변경 / 비밀번호 변경 / 보안 설정

  **섹션 1 — 이름 변경**
  - 현재 닉네임 표시 (authStore)
  - 새 닉네임 input
  - 저장 버튼 → `changeNickname()` → 성공 토스트

  **섹션 2 — 비밀번호 변경**
  - 현재 비밀번호 input (type="password")
  - 새 비밀번호 input (type="password")
  - 새 비밀번호 확인 input (type="password") — 프론트 일치 검증
  - 변경 버튼 → `changePassword()` → 성공 토스트

  **섹션 3 — 보안 설정 (OTP)**
  - `auth/05` Step 9, 10 미완료 시: "준비 중" 안내 표시
  - 완료 후: `useOtpApi` + `AuthOtpCodeInput` 연결
    - OTP 미설정: 설정 시작 버튼 → QR 코드 표시 → 6자리 코드 입력 → 활성화
    - OTP 설정 완료: "OTP 사용 중" 표시 + 해제 버튼 → OTP 코드 재확인 → 비활성화

---

## Step 7. Frontend — AppHeader 수정

- [x] `components/common/AppHeader.vue` 수정
  - `.app-header__profile` 전체를 `<NuxtLink to="/settings">` 로 감싸기
  - hover 시 밑줄 또는 강조 스타일 추가

---

## Step 8. todo/auth/05 연동 처리

- [x] `todo/auth/05-google-otp-totp.md` Step 12 수정
  - `/security/index.vue` 신규 생성 항목 → `/settings` 보안 섹션으로 통합 완료 표시
  - AppHeader 보안설정 링크 추가 항목 → `/settings` 링크로 대체
