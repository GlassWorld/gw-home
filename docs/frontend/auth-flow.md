# Auth Flow

## 인증 흐름

```
1. 사용자 → /login 접속
2. 로그인 ID + 비밀번호 입력
3. POST /api/v1/auth/login
4. 서버 → `login_status` + 토큰 응답
5. `SUCCESS` 또는 `OTP_SETUP_REQUIRED` 인 경우
   AccessToken: store + `gw-home-access-token` 쿠키 저장
   RefreshToken: `gw-home-refresh-token` 쿠키 저장
6. `OTP_REQUIRED` 인 경우 `otp_temp_token` 으로 추가 인증
7. 인증 완료 후 `/dashboard` 이동
```

## 토큰 갱신 흐름

```
API 요청 → 401 응답
  ↓
POST /api/v1/auth/refresh (refresh token을 요청 바디로 전송)
  ├─ 성공 → 새 AccessToken + RefreshToken 갱신 → 쿠키/스토어 갱신 후 원래 요청 재시도
  └─ 실패 → /login 이동 (로그아웃 처리)
```

## 로그아웃 흐름

```
사용자 → 로그아웃 버튼
  ↓
POST /api/v1/auth/logout
  ↓
store 초기화 (currentUser = null)
  ↓
/login 이동
```

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/v1/auth/login` | 로그인 (토큰 발급) |
| `POST` | `/api/v1/auth/logout` | 로그아웃 (토큰 무효화) |
| `POST` | `/api/v1/auth/refresh` | AccessToken 갱신 |
| `POST` | `/api/v1/auth/otp/setup` | OTP 설정용 시크릿 발급 |
| `POST` | `/api/v1/auth/otp/activate` | OTP 활성화 |
| `POST` | `/api/v1/auth/otp/verify` | OTP 검증 후 토큰 발급 |
| `POST` | `/api/v1/auth/otp/disable` | OTP 비활성화 |
| `GET` | `/api/v1/auth/otp/status` | OTP 활성화 여부 조회 |

## Store 구조

```typescript
// stores/auth.store.ts
interface AuthState {
  currentUser: UserProfile | null
  accessToken: string | null
}
```

현재 구현 메모:

- refresh token은 서버가 `HttpOnly` 쿠키로 내려주는 구조가 아니다.
- 프론트가 응답 바디의 `refresh_token` 값을 읽어 `gw-home-refresh-token` 쿠키에 저장한다.
- 따라서 현재 인증 모델은 "프론트 주도 토큰 저장/갱신" 구조로 이해해야 한다.

## Composable 구조

```typescript
// composables/use-auth.ts
export function useAuth() {
  const login = async (
    loginId: string,
    password: string
  ): Promise<
    | { status: 'SUCCESS' }
    | { status: 'OTP_REQUIRED'; otpTempToken: string }
    | { status: 'OTP_SETUP_REQUIRED' }
  >
  const logout = async (): Promise<void>
  const refreshToken = async (): Promise<string>

  return { login, logout, refreshToken }
}
```

## 인증 필요 페이지 처리

- `definePageMeta({ middleware: 'auth' })` 선언
- 미인증 시 `/login` 리다이렉트 자동 처리

```vue
<!-- pages/dashboard/index.vue -->
<script setup lang="ts">
definePageMeta({ middleware: 'auth' })
</script>
```

## 로그인 전용 페이지 처리

```vue
<!-- pages/login.vue -->
<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
</script>
```
