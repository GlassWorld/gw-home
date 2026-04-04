# 프론트엔드 개발 규칙

## 이 문서의 목적

이 문서는 프론트엔드 구현 시 지켜야 할 네이밍, 구조, API 연동 기준을 설명한다.

## 핵심 원칙

- 축약어를 사용하지 않는다
- `any` 타입을 사용하지 않는다
- API 응답 식별자는 `uuid` 기준으로만 다룬다
- 공통 UI는 가능한 한 공통 컴포넌트로 관리한다
- 페이지는 조합 중심으로 유지하고, 반복 로직은 util, composable, feature component로 올린다

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

## 페이지 리팩토링 기준

- 페이지 파일이 300줄 이상이 되면 바로 분리 대상 여부를 검토한다
- 줄 수만 보지 말고 아래 책임이 한 파일에 섞였는지 먼저 확인한다
  - API 호출과 비동기 상태
  - 폼 상태와 검증
  - 모달 열기/닫기와 액션 처리
  - 마크다운, 날짜 포맷, query sync 같은 재사용 가능한 유틸
  - 화면 내부 전용 타입, 옵션 상수, 드롭다운 상태
- 페이지는 가능한 한 화면 조합과 라우트 예외 처리만 남기고, 세부 동작은 feature 단위로 분리한다

## 리팩토링 우선 순서

- 먼저 공통 util을 분리한다
  - 예: 날짜 포맷, 마크다운 렌더링, query helper
- 다음으로 공통 composable을 분리한다
  - 예: OTP 설정 흐름, 목록 검색/페이지 이동, 관리자 액션 처리
- 그 다음 공통 또는 feature component를 분리한다
  - 예: 상세 모달, OTP 패널, 필터 폼, 관리 모달, 테이블
- 마지막으로 페이지는 분리된 모듈을 조합하는 구조로 정리한다

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
- 페이지 안에서만 임시로 두었던 타입도 재사용 가능성이 생기면 페이지 밖으로 이동한다

## 공통 UI 원칙

- 버튼, 모달, 토스트처럼 반복 사용되는 UI는 공통 컴포넌트로 관리한다
- 페이지 컴포넌트는 공통 컴포넌트를 조합해 화면을 만든다
- 새 컴포넌트를 만들기 전에 기존 공통 컴포넌트 확장 가능성을 먼저 검토한다
- 인라인 SVG는 일회성 장식이 아니라면 `assets` 또는 아이콘 컴포넌트로 분리한다

## CSS 공통화 원칙

- 반복되는 hero, section header, actions, panel grid, form grid, OTP panel 같은 패턴은 전역 공통 스타일로 올린다
- 전역 스타일은 `assets/styles`에서 관리하고, scoped CSS는 해당 화면의 고유한 색상, 배치, 예외 스타일만 남긴다
- 같은 `padding`, `gap`, 반응형 1열 전환 규칙이 여러 페이지에 반복되면 공통 클래스 또는 토큰으로 승격한다
- 공통화는 무조건 추상화하지 말고, 두 개 이상 화면에서 반복되는 패턴부터 적용한다
