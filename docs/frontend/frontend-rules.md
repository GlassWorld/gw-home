# Frontend Rules

## 절대 규칙 (위반 금지)

- [ ] 축약어 사용 금지 (`button` not `btn`, `user` not `usr`, `index` not `idx`)
- [ ] `any` 타입 사용 금지 — 반드시 명시적 타입 정의
- [ ] API 응답 `_idx` 필드 사용 금지 — `uuid` 기반으로만 식별

## 기술 스택

- Nuxt3 (Vue3 Composition API)
- TypeScript
- Pinia (상태 관리)
- `$fetch` / `useFetch` (API 호출)

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 파일명 | kebab-case | `user-profile.vue`, `auth-form.ts` |
| 컴포넌트명 | PascalCase | `UserProfile`, `LoginForm` |
| 함수명 | camelCase (동사 시작) | `fetchUserProfile`, `handleLogin` |
| 변수명 | camelCase (full name) | `userList`, `boardContent`, `isLoading` |
| TypeScript 타입/인터페이스 | PascalCase | `BoardResponse`, `UserProfile` |
| Composable | `use` 접두사 | `useAuth`, `useBoard` |
| Store | `use{Name}Store` | `useAuthStore`, `useBoardStore` |
| CSS 클래스 | kebab-case | `user-avatar`, `board-list-item` |

## 디렉토리 구조 (`gw-home-ui/`)

```
gw-home-ui/
├── pages/              # 라우트 페이지
├── components/         # 재사용 컴포넌트
├── composables/        # API 호출 및 공통 로직
├── stores/             # Pinia 상태 관리
├── types/              # TypeScript 타입 정의
├── assets/             # 이미지, 스타일
├── middleware/         # 라우트 미들웨어 (인증 등)
├── nuxt.config.ts
├── tsconfig.json
└── package.json
```

## API 호출 규칙

- API 호출은 `composables/` 에 모아서 관리
- `useFetch` 또는 `$fetch` 사용
- Base URL은 환경 변수로 관리 (`NUXT_PUBLIC_API_BASE`)
- 응답 타입은 `types/` 에서 명시적으로 선언

```typescript
// composables/use-board.ts
export function useBoard() {
  const fetchBoardList = async (): Promise<BoardListResponse> => {
    return await $fetch('/api/v1/boards')
  }
  return { fetchBoardList }
}
```

## 상태 관리 규칙 (Pinia)

- Store는 도메인 단위로 분리
- Store 파일명: `{domain}.store.ts`
- 직접 mutation 금지 — action 경유

```typescript
// stores/auth.store.ts
export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<UserProfile | null>(null)
  const isAuthenticated = computed(() => currentUser.value !== null)

  function setUser(user: UserProfile) {
    currentUser.value = user
  }

  return { currentUser, isAuthenticated, setUser }
})
```

## TypeScript 타입 규칙

- API 응답 타입은 `types/api/` 에 정의
- 컴포넌트 props 타입은 `defineProps<{...}>()` 사용
- `uuid` 식별자는 `string` 타입으로 명시

```typescript
// types/api/board.ts
export interface BoardResponse {
  uuid: string
  title: string
  content: string
  createdBy: string
  createdAt: string
}
```
