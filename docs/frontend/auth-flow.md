# 인증 흐름

## 이 문서의 목적

이 문서는 로그인, 토큰 갱신, 로그아웃, OTP 처리 흐름을 사용자 관점에서 설명한다.

## 로그인 흐름

```text
1. 사용자가 /login 에 접속한다
2. 로그인 ID와 비밀번호를 입력한다
3. POST /api/v1/auth/login 요청을 보낸다
4. 서버가 로그인 상태와 토큰 정보를 응답한다
5. 상태에 따라 다음 단계로 진행한다
   - SUCCESS
   - OTP_SETUP_REQUIRED
   - OTP_REQUIRED
6. 인증이 완료되면 /dashboard 로 이동한다
```

## 토큰 저장 방식

- Access Token은 프론트 상태와 쿠키에 저장한다
- Refresh Token도 현재는 프론트가 응답 바디에서 읽어 쿠키에 저장한다
- 따라서 현재 인증 모델은 프론트가 토큰 저장과 갱신을 주도하는 구조다

## 토큰 갱신 흐름

```text
API 요청
  -> 401 응답
  -> POST /api/v1/auth/refresh
  -> 성공 시 새 토큰 저장
  -> 원래 요청 재시도
  -> 실패 시 로그아웃 후 /login 이동
```

## 실제 인증 복구 순서

현재 프론트 구현은 아래 순서로 인증 상태를 복구한다.

```text
1. store 에 인증 정보가 있으면 그대로 사용
2. 없으면 access token 쿠키로 사용자 정보 복구 시도
3. access token 복구가 실패하면 refresh token 으로 갱신 시도
4. refresh 도 실패하면 인증 상태를 비우고 /login 으로 이동
```

## 로그아웃 흐름

```text
사용자 로그아웃 요청
  -> POST /api/v1/auth/logout
  -> 인증 상태 초기화
  -> /login 이동
```

## OTP 관련 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/v1/auth/otp/setup` | OTP 설정용 시크릿 발급 |
| `POST` | `/api/v1/auth/otp/activate` | OTP 활성화 |
| `POST` | `/api/v1/auth/otp/verify` | OTP 검증 후 토큰 발급 |
| `POST` | `/api/v1/auth/otp/disable` | OTP 비활성화 |
| `GET` | `/api/v1/auth/otp/status` | OTP 활성화 여부 조회 |

## 인증 필요 페이지 처리

- 인증이 필요한 페이지는 `auth` 미들웨어를 사용한다
- 비인증 전용 페이지는 `guest` 미들웨어를 사용한다
- 관리자 전용 페이지는 `admin` 미들웨어를 사용한다

예시:

```vue
<script setup lang="ts">
definePageMeta({ middleware: 'auth' })
</script>
```

```vue
<script setup lang="ts">
definePageMeta({ middleware: 'guest' })
</script>
```

```vue
<script setup lang="ts">
definePageMeta({ middleware: 'admin' })
</script>
```
