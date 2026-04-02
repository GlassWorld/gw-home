# 프론트엔드 개발 규칙

## 이 문서의 목적

이 문서는 프론트엔드 구현 시 지켜야 할 네이밍, 구조, API 연동 기준을 설명한다.

## 핵심 원칙

- 축약어를 사용하지 않는다
- `any` 타입을 사용하지 않는다
- API 응답 식별자는 `uuid` 기준으로만 다룬다
- 공통 UI는 가능한 한 공통 컴포넌트로 관리한다

## 기술 스택

- Nuxt3
- Vue3 Composition API
- TypeScript
- Pinia
- `$fetch`, `useFetch`

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 라우트/일반 파일명 | kebab-case | `user-profile.vue`, `use-board-list.ts` |
| 컴포넌트 파일명 | 기존 디렉터리 관례 유지 | `AppHeader.vue`, `DailyReportFormModal.vue` |
| 컴포넌트명 | PascalCase | `UserProfile` |
| 함수명 | camelCase | `fetchUserProfile` |
| 변수명 | full name camelCase | `userList`, `boardContent` |
| 타입/인터페이스 | PascalCase | `BoardResponse` |
| Composable | `use` 접두사 | `useAuth`, `useBoard` |
| Store | `use{Name}Store` | `useAuthStore` |
| CSS 클래스 | kebab-case | `board-list-item` |

## 디렉터리 구조

```text
gw-home-ui/
├── pages/
├── features/
├── components/
├── composables/
├── stores/
├── types/
├── assets/
├── middleware/
└── ...
```

## API 호출 규칙

- 공용으로 재사용되는 API 진입점은 `composables/`에 둔다
- 특정 feature 안에서만 쓰는 API 구현과 feature-local 타입은 `features/{domain}/api`, `features/{domain}/types`에 둘 수 있다
- feature 내부에서는 가능하면 루트 `composables/`의 공개 진입점을 통해 접근하고, 직접 경로 참조는 feature 내부에서만 제한적으로 사용한다
- 응답 타입은 TypeScript로 명시한다
- Base URL은 환경 변수로 관리한다
- `ApiResponse<T>` 구조를 기준으로 성공 여부와 메시지를 처리한다

## 상태 관리 규칙

- Store는 도메인 단위로 분리한다
- Store 파일명은 `{domain}.store.ts` 형태를 따른다
- 상태 변경은 action을 통해 수행하는 구조를 권장한다

## 타입 규칙

- API 응답 타입은 `types/api/` 아래에 둔다
- feature 전용 화면, 폼, 도메인 타입은 `features/{domain}/types` 또는 루트 `types/`에 둘 수 있다
- props 타입은 명시적으로 선언한다
- `uuid`는 `string`으로 다룬다

## 공통 UI 원칙

- 버튼, 모달, 토스트처럼 반복 사용되는 UI는 공통 컴포넌트로 관리한다
- 페이지 컴포넌트는 공통 컴포넌트를 조합해 화면을 만든다
- 새 컴포넌트를 만들기 전에 기존 공통 컴포넌트 확장 가능성을 먼저 검토한다
