# Review: 계정 설정 페이지

> 작업 분류: **NORMAL**
> 신규 비밀번호 변경 API + 신규 설정 페이지 + 헤더 네비게이션 연결

---

## 작업 개요

헤더 계정정보 클릭 시 `/settings` 페이지로 이동.
페이지 내 3개 섹션 제공:

1. **이름 변경** — 닉네임 수정 (기존 `PUT /api/v1/profiles/me` 재사용)
2. **비밀번호 변경** — 현재 비밀번호 확인 후 새 비밀번호 설정 (신규 API)
3. **보안 설정** — OTP 설정/해제 (`todo/auth/05-google-otp-totp.md` Step 12 통합)

---

## 변경 목적

설정 기능을 한 페이지에 통합해 접근성 개선.
헤더 계정정보 → 직접 설정 페이지 진입.

---

## 설계 결정 사항

### 1. 페이지 경로

`/settings/index.vue`

### 2. OTP 섹션 통합

`todo/auth/05-google-otp-totp.md` Step 12에서 `/security/index.vue` 신규 생성으로 계획된 OTP 설정 UI를 이 페이지의 보안 설정 섹션으로 통합.
→ `/security/index.vue` 생성 불필요, `/settings` 내 섹션으로 대체.

### 3. 헤더 계정정보 클릭

`.app-header__profile` 을 `<NuxtLink to="/settings">` 로 감싸 클릭 시 이동.

### 4. 이름 변경

닉네임만 변경 (introduction, profileImageUrl 는 별도 프로필 편집 기능으로 분리).
기존 `PUT /api/v1/profiles/me` 재사용, `nickname` 필드만 전송.

### 5. 비밀번호 변경 API

신규: `PUT /api/v1/accounts/me/password`
- 현재 비밀번호 검증 후 새 비밀번호로 교체
- `AccountMapper` 에 `updatePassword(idx, hashedPwd)` 추가
- DB 스키마 변경 없음

---

## 예상 영향 범위

### Backend (NORMAL)

| 레이어 | 파일 | 변경 내용 |
|--------|------|-----------|
| Mapper | `AccountMapper.java` | `updatePassword(idx, pwd)` 추가 |
| Mapper XML | `AccountMapper.xml` | `updatePassword` SQL 추가 |
| Service | `AccountService.java` | `changePassword(loginId, currentPwd, newPwd)` 추가 |
| Controller | `AccountController.java` | `PUT /api/v1/accounts/me/password` 추가 |
| DTO | `ChangePasswordRequest.java` (신규) | `currentPassword`, `newPassword` |

### Frontend (NORMAL)

| 파일 | 변경 내용 |
|------|-----------|
| `pages/settings/index.vue` (신규) | 이름 변경 + 비밀번호 변경 + 보안 섹션 |
| `composables/useSettingsApi.ts` (신규) | `changePassword()`, `changeNickname()` |
| `components/common/AppHeader.vue` | `.app-header__profile` → NuxtLink 감싸기 |

> OTP 섹션은 `useOtpApi.ts` (auth/05 Step 9), `OtpCodeInput.vue` (auth/05 Step 10) 완료 후 재사용.

---

## 리스크 분석

| 리스크 | 대응 |
|--------|------|
| 현재 비밀번호 틀림 | `passwordEncoder.matches()` 검증, `BusinessException(UNAUTHORIZED)` |
| 새 비밀번호 = 현재 비밀번호 | 서비스단에서 동일 여부 체크 후 거부 |
| `auth/05` OTP 작업 미완료 상태에서 settings 페이지 접근 | 보안 섹션: OTP 작업 완료 전까지 "준비 중" 표시 또는 작업 순서 조정 |

---

## 작업 순서 의존성

```
auth/05 Step 9 (useOtpApi.ts)    ──┐
auth/05 Step 10 (OtpCodeInput)   ──┤──> settings 페이지 보안 섹션
이번 작업 (settings page)        ──┘
```

비밀번호 변경 / 이름 변경 섹션은 독립적으로 먼저 구현 가능.
OTP 섹션은 `auth/05` Step 9, 10 완료 이후 연결.

---

## 전체 파일 목록

### 신규 (3개)
```
gw-home-api/.../dto/account/ChangePasswordRequest.java
gw-home-ui/pages/settings/index.vue
gw-home-ui/composables/useSettingsApi.ts
```

### 변경 (4개)
```
gw-home-infra-db/.../mapper/account/AccountMapper.java
gw-home-infra-db/.../mapper/account/AccountMapper.xml
gw-home-api/.../service/account/AccountService.java
gw-home-api/.../controller/account/AccountController.java
gw-home-ui/components/common/AppHeader.vue
```

### 연관 todo 수정
```
todo/auth/05-google-otp-totp.md  Step 12 — /security/index.vue → /settings 통합으로 변경
```

---

## 관련 문서

- `review/auth/04-google-otp-totp.md` — OTP 설계
- `todo/auth/05-google-otp-totp.md` — OTP 구현 계획 (Step 12 수정 필요)
- `gw-home-api/.../controller/account/AccountController.java`
- `gw-home-api/.../controller/profile/ProfileController.java`
