# Work: 계정 설정 페이지

> 연관 문서: [review/account/01-account-settings-page.md](../../review/account/01-account-settings-page.md), [todo/account/01-account-settings-page.md](../../todo/account/01-account-settings-page.md)

## 작업 범위

- account 비밀번호 변경 API 추가
- `/settings` 페이지에 닉네임/비밀번호/OTP 설정 통합
- 헤더 계정정보 및 설정 링크를 `/settings` 로 연결

## 구현 순서

1. `AccountMapper`, DTO, `AccountService`, `AccountController`에 비밀번호 변경 API 반영
2. `useSettingsApi.ts` 와 `/settings/index.vue`에 닉네임/비밀번호/OTP UI 반영
3. `AppHeader.vue` 를 설정 페이지 진입 구조로 조정
4. `todo/auth/05-google-otp-totp.md` Step 12를 `/settings` 기준으로 업데이트
5. todo 체크 및 가능한 범위 테스트/타입 검증 수행

## 운영 반영 순서

1. Backend 배포
2. Frontend 배포
3. `/settings` 페이지에서 닉네임/비밀번호/OTP 동작 점검
4. 기존 `/security` 링크 대신 `/settings` 진입 확인
