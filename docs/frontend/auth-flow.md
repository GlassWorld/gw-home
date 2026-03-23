# Auth Flow

## 인증 흐름

```
1. 사용자 → /login 접속
2. 이메일 + 비밀번호 입력
3. POST /api/v1/auth/login
4. 서버 → AccessToken + RefreshToken 응답
5. AccessToken: 메모리(store) 저장
   RefreshToken: HttpOnly 쿠키 저장
6. /dashboard 이동
```

## 토큰 갱신 흐름

```
API 요청 → 401 응답
  ↓
POST /api/v1/auth/refresh (RefreshToken 쿠키 자동 전송)
  ├─ 성공 → 새 AccessToken 발급 → 원래 요청 재시도
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

## Store 구조

```typescript
// stores/auth.store.ts
interface AuthState {
  currentUser: UserProfile | null
  accessToken: string | null
}
```

## Composable 구조

```typescript
// composables/use-auth.ts
export function useAuth() {
  const login = async (email: string, password: string): Promise<void>
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
