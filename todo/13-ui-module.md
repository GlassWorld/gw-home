# 13. UI Module (Nuxt3)

**Layer**: Frontend
**Depends on**: 05-domain-auth (로그인 API), 07-domain-board (게시글 API)

## 목적

`{project}-ui` 디렉토리에 Nuxt3 기반 프론트엔드를 초기 세팅하고,
로그인 → 대시보드 기본 흐름을 구현한다.

---

## TODO

### 초기 세팅

- [x] `gw-home-ui/` 디렉토리 생성
- [x] `nuxt.config.ts` 작성 (API base URL, 모듈 설정)
- [x] `tsconfig.json` 작성
- [x] `package.json` 작성 (Nuxt3, Pinia, TypeScript)
- [x] `.env.example` 작성 (`NUXT_PUBLIC_API_BASE=http://localhost:8080`)

### 디렉토리 구조 생성

- [x] `pages/` 생성
- [x] `components/` 생성
- [x] `composables/` 생성
- [x] `stores/` 생성
- [x] `types/api/` 생성
- [x] `middleware/` 생성
- [x] `assets/` 생성

### TypeScript 타입 정의

- [x] `types/api/auth.ts` — 로그인 Request/Response 타입
- [x] `types/api/board.ts` — 게시글 목록/상세 타입
- [x] `types/api/user.ts` — 사용자 프로필 타입

### Pinia Store

- [x] `stores/auth.store.ts`
  - `currentUser: UserProfile | null`
  - `accessToken: string | null`
  - `isAuthenticated: computed`
  - actions: `setUser`, `setToken`, `clearAuth`

### Composables

- [x] `composables/use-auth.ts`
  - `login(email, password)` → POST `/api/v1/auth/login`
  - `logout()` → POST `/api/v1/auth/logout`
  - `refreshToken()` → POST `/api/v1/auth/refresh`
- [x] `composables/use-board.ts`
  - `fetchBoardList(params?)` → GET `/api/v1/boards`
  - `fetchBoard(boardUuid)` → GET `/api/v1/boards/{uuid}`

### Middleware

- [x] `middleware/auth.ts` — 비인증 시 `/login` 리다이렉트
- [x] `middleware/guest.ts` — 인증 시 `/dashboard` 리다이렉트

### Pages

- [x] `pages/login.vue`
  - `definePageMeta({ middleware: 'guest' })`
  - 이메일 + 비밀번호 입력 폼
  - 로그인 성공 → `/dashboard` 이동
  - 에러 메시지 표시

- [x] `pages/dashboard/index.vue`
  - `definePageMeta({ middleware: 'auth' })`
  - 로그인 사용자 정보 표시
  - 최근 게시글 목록 (5건)
  - 게시글 작성 링크

- [x] `pages/board/index.vue`
  - `definePageMeta({ middleware: 'auth' })`
  - 게시글 목록 (페이징)
  - 키워드 검색

- [x] `pages/board/[boardUuid].vue`
  - `definePageMeta({ middleware: 'auth' })`
  - 게시글 상세
  - 수정/삭제 버튼 (작성자 본인만)

### Components

- [x] `components/common/AppHeader.vue` — 상단 네비게이션 (로그인 사용자명, 로그아웃)
- [x] `components/board/BoardListItem.vue` — 게시글 목록 아이템
- [x] `components/auth/LoginForm.vue` — 로그인 폼

---

## 구현 순서

1. 초기 세팅 (nuxt.config.ts, package.json)
2. TypeScript 타입 정의
3. Pinia Store (auth)
4. Composables (use-auth, use-board)
5. Middleware (auth, guest)
6. Pages (login → dashboard → board)
7. Components

## 완료 기준

- [ ] `npm run dev` 정상 실행
- [ ] `/login` 접속 → 로그인 폼 표시
- [ ] 로그인 성공 → `/dashboard` 이동
- [ ] 비인증 상태 `/dashboard` 접속 → `/login` 리다이렉트
- [ ] `/board` 게시글 목록 정상 표시

## 진행 메모

- [x] Nuxt3 기반 `gw-home-ui/` 초기 모듈 구성 완료
- [x] 문서 요구 범위 외에 `/`, `/board/create`, 공통 스타일, 루트 `app.vue`도 함께 연결
- [x] 백엔드 실제 스펙에 맞춰 로그인 입력값은 `email` 대신 `loginId`로 반영
- [x] 백엔드 실제 스펙에 맞춰 `refreshToken`은 응답 바디 기준으로 쿠키에 저장하도록 구현
- [ ] `npm run typecheck` 시도 결과 `nuxt: Permission denied`로 실행 검증 미완료
