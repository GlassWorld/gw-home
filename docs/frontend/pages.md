# Pages

## 라우트 구조

| 경로 | 파일 | 설명 | 인증 필요 |
|------|------|------|-----------|
| `/` | `pages/index.vue` | 루트 진입 시 `/dashboard` 이동 | X |
| `/login` | `pages/login.vue` | 로그인 화면 | X |
| `/dashboard` | `pages/dashboard/index.vue` | 대시보드 | O |
| `/board` | `pages/board/index.vue` | 게시글 목록 | O |
| `/board/[boardUuid]` | `pages/board/[boardUuid].vue` | 게시글 상세 | O |
| `/board/create` | `pages/board/create.vue` | 게시글 작성 | O |
| `/work` | `pages/work/index.vue` | 업무 등록/조회 | O |
| `/work/daily-reports` | `pages/work/daily-reports/index.vue` | 일일보고 작성/조회 | O |
| `/work/weekly-reports` | `pages/work/weekly-reports/index.vue` | 주간보고 작성/조회 | O |
| `/vault` | `pages/vault/index.vue` | 자격증명 보관함 | O |
| `/settings` | `pages/settings/index.vue` | 계정 설정 | O |
| `/security` | `pages/security/index.vue` | OTP 및 보안 설정 | O |
| `/admin/accounts` | `pages/admin/accounts/index.vue` | 관리자 계정 관리 | O (관리자) |
| `/admin/daily-reports` | `pages/admin/daily-reports/index.vue` | 관리자 일일보고 관리 | O (관리자) |
| `/admin/vault-categories` | `pages/admin/vault-categories/index.vue` | 관리자 금고 카테고리 관리 | O (관리자) |

## 초기 진입 흐름

```
브라우저 접속 (`/`)
  ↓
`pages/index.vue` 에서 `/dashboard` 이동
  ↓
미들웨어: auth.ts (보호 페이지 인증 확인)
  ├─ 비인증 → /login 리다이렉트
  └─ 인증됨 → 요청 경로 유지
```

## 페이지별 책임

### `login.vue`
- 로그인 폼 (로그인 ID + 비밀번호)
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

### `settings/index.vue`
- 이메일, 닉네임, 소개 수정
- 프로필 이미지 변경 진입

### `security/index.vue`
- OTP 설정/활성화/비활성화
- 보안 설정 관리

### `work/*`
- 업무 등록/수정/사용 여부 관리
- 일일보고/주간보고 작성 및 조회

### `admin/*`
- 계정 관리
- 일일보고 관리
- 금고 카테고리 관리

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
