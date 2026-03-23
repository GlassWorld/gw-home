# Pages

## 라우트 구조

| 경로 | 파일 | 설명 | 인증 필요 |
|------|------|------|-----------|
| `/login` | `pages/login.vue` | 로그인 화면 (초기 진입) | X |
| `/dashboard` | `pages/dashboard/index.vue` | 대시보드 (로그인 후 진입) | O |
| `/board` | `pages/board/index.vue` | 게시글 목록 | O |
| `/board/[boardUuid]` | `pages/board/[boardUuid].vue` | 게시글 상세 | O |
| `/board/create` | `pages/board/create.vue` | 게시글 작성 | O |
| `/profile` | `pages/profile/index.vue` | 내 프로필 조회/수정 | O |
| `/admin` | `pages/admin/index.vue` | 관리자 대시보드 | O (관리자) |

## 초기 진입 흐름

```
브라우저 접속
  ↓
미들웨어: auth.ts (인증 확인)
  ├─ 비인증 → /login 리다이렉트
  └─ 인증됨 → 요청 경로 유지
        ↓ (/ 루트 접속 시)
    /dashboard 리다이렉트
```

## 페이지별 책임

### `login.vue`
- 로그인 폼 (이메일 + 비밀번호)
- 로그인 성공 → `/dashboard` 이동
- 이미 로그인 상태 → `/dashboard` 자동 이동

### `dashboard/index.vue`
- 로그인 사용자 요약 정보
- 최근 게시글 목록
- 빠른 링크 (게시글 작성, 프로필)

### `board/index.vue`
- 게시글 목록 (페이징, 키워드 검색)
- 정렬: 최신순 기본

### `board/[boardUuid].vue`
- 게시글 상세 본문
- 댓글 목록
- 작성자 본인: 수정/삭제 버튼

### `profile/index.vue`
- 내 프로필 조회
- 닉네임, 소개 수정
- 프로필 이미지 업로드 (file 도메인 경유)

## 미들웨어

```
middleware/
├── auth.ts         # 인증 필요 페이지 접근 제어
└── guest.ts        # 비인증 전용 페이지 (login 등)
```

### `auth.ts`

```typescript
// middleware/auth.ts
export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore()
  if (!authStore.isAuthenticated) {
    return navigateTo('/login')
  }
})
```

### `guest.ts`

```typescript
// middleware/guest.ts
export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore()
  if (authStore.isAuthenticated) {
    return navigateTo('/dashboard')
  }
})
```
