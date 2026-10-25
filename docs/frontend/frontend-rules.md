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
| 컴포넌트 파일명 | 기존 디렉터리 관례 유지 | `AppHeader.vue`, `WorkTodoFormModal.vue` |
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

### 화면 제목과 목록 표현

- 페이지 제목은 `CommonBasePageHeader`의 `title`을 사용한다. 선택적인 `eyebrow`·`description`, 상단 버튼의 `actions` 슬롯, 결과 건수 등의 `summary` 슬롯, 추가 안내의 기본 슬롯을 제공한다.
- 이 컴포넌트는 `h1`을 출력하므로 페이지의 주 제목에만 사용한다. 화면 내부 `h2`·`h3` 영역은 기존 `page-section-header`를 사용한다. 패널을 포함하지 않으므로 사용처에서 `content-panel page-panel-padding-md` 또는 `page-panel-padding-lg`로 감싼다.
- `actions` 슬롯에는 버튼·링크를 직접 배치한다. 모바일은 2열, 버튼 하나만 있으면 전체 너비로 표시하며 동작·비활성 상태는 사용처가 관리한다. 별도 페이지별 제목·상단 버튼 CSS를 반복하지 않는다.
- 페이지 간격은 `page-container page-stack`, 패널 간격은 `page-panel-padding-*`의 모바일 규칙을 공유한다. 화면별 고정 패딩으로 공통 간격을 덮어쓰지 않는다.
- 조회 로딩·오류·빈 결과는 `BaseState`, 이전/다음 이동은 `BasePagination`을 재사용한다. 오류 재조회는 `action-label`과 `@action`, 조회 중 페이지 이동 차단은 `:disabled`를 연결한다. 입력 검증 메시지는 해당 필드 가까이에 유지한다.

## CSS 공통화 원칙

- 반복되는 hero, section header, actions, panel grid, form grid, OTP panel 같은 패턴은 전역 공통 스타일로 올린다
- 전역 스타일은 `assets/styles`에서 관리하고, scoped CSS는 해당 화면의 고유한 색상, 배치, 예외 스타일만 남긴다
- 같은 `padding`, `gap`, 반응형 1열 전환 규칙이 여러 페이지에 반복되면 공통 클래스 또는 토큰으로 승격한다
- 공통화는 무조건 추상화하지 말고, 두 개 이상 화면에서 반복되는 패턴부터 적용한다

## 디자인 기준과 반응형 공통 UI

- `assets/styles/tokens.css`는 색상·글꼴·간격·모서리·그림자·컨트롤 높이의 기준이다. 사용자 선호에 따라 기본 다크모드(`color-scheme: dark`)를 사용한다. 배경은 `main.css`의 검정에 가까운 우주 그라데이션과 기본색 `#050608`을 사용하고, 성운의 채도·밝기를 낮춰 희미한 별빛을 남긴다. 패널 `#181e26`, 본문 `#edf2f8`, 주요 동작 `#8bb8ff`와 의미별 성공·주의·오류 토큰을 유지한다.
- `assets/styles/layout.css`는 앱 골격, 페이지 여백, 패널·폼·액션·표 스크롤을 담당한다. `main.css`는 공통 스타일 파일을 불러오고 기본 요소·입력·버튼·상태 스타일을 제공한다.
- `assets/styles/motion.css`는 페이지·모달·모바일 메뉴·선택 목록·토스트와 버튼/펼침 영역의 모션을 담당한다. `tokens.css`의 140/220/280ms와 공통 감속 곡선을 사용하고, `prefers-reduced-motion`에서는 이동·지연을 제거하고 지속 시간을 최소화한다. 닫히는 요소는 `utils/motion.ts`의 `disableLeavingElement`로 inert 처리해 퇴장 중 클릭·키보드 접근을 막는다.
- 본문은 기본 16px/1.6, 주요 입력은 16px 이상을 유지한다. 사용자의 간결한 버튼 선호에 따라 일반 버튼은 `--button-height` 36px, 작은 버튼은 `--button-height-small` 30px를 사용한다. 터치 환경(`pointer: coarse`)에서는 각각 40px·36px이며 동작 간 8px 이상 간격을 둔다. 입력·메뉴는 `--control-height` 40px, 터치에서는 44px를 사용한다. 상태는 색뿐 아니라 문구로 함께 전달한다.
- 콘텐츠 최대 폭은 1280px, 좌우 여백은 PC·태블릿 20px/모바일 8px이며 상하 여백은 20px/12px이다. `--section-gap`은 16px/12px, 패널은 `--panel-padding` 16px·큰 패널은 20px, 내부 카드는 `--card-padding` 12px를 사용한다. 모바일 주요 패널은 `--mobile-panel-padding`(상하 12px·좌우 10px)을 공유한다. 새 페이지는 `page-container`, `page-stack`, `content-panel`을 조합하고 중첩 카드에 불필요한 여백을 추가하지 않는다.
- 카드·패널·모달은 `--radius-medium`과 `--radius-large`의 0px 모서리를 사용한다. 버튼·입력은 `--radius-small` 4px를 사용하며 상태 배지·진행 막대 등의 모양은 유지한다. 화면별 카드에 고정 반경·여백을 중복 정의하지 않고 공통 토큰을 사용한다.
- 내비게이션은 1100px 이상에서 고정 폭 사이드바, 그 미만에서 헤더 버튼으로 여는 메뉴를 사용한다. 콘텐츠·폼의 기본 모바일 전환 기준은 768px 미만이다. 표를 카드로 바꿔야 하는 화면은 feature 안에서 정보·동작 순서를 정의한다.
- 공통 UI가 화면 상태만 표현하도록 한다. `BaseState`의 `loading / empty / error / success`, `BaseButton`의 `loading`, `ToastViewport`의 저장 완료 메시지를 재사용하고 데이터 조회·재시도는 사용처가 처리한다.
- `BaseModal`과 모바일 메뉴는 `useOverlayLayer`로 배경 스크롤·포커스 순환·Escape·닫은 뒤 포커스 복귀를 공유한다. 필수 OTP처럼 닫을 수 없는 모달은 `dismissible=false`를 명시한다.
- 모달 제목·닫기와 하단 액션을 유지하고 본문만 스크롤한다. 작은 화면 높이와 safe area를 고려하며 긴 제목·본문은 줄바꿈한다.
- `SearchableSelect`는 검색·방향키·Enter·Escape·Tab, 선택 상태의 접근성 속성과 화면 가장자리 배치를 공통 처리한다. 사용처는 명확한 `label` 또는 `placeholder`를 제공한다.
- 인증·즐겨찾기·메모의 API 경계와 도메인 정책은 디자인 변경 과정에서 수정하지 않는다.

### 화면 검증 기준

| 크기 | 확인 기준 |
|------|-----------|
| 360×800, 390×844 | 페이지 전체 가로 넘침 없음, 터치 버튼 40px(작은 버튼 36px)·입력/메뉴 44px·동작 간 간격, 긴 제목 줄바꿈, 메뉴 스크롤·닫기·포커스 복귀 |
| 768×1024 | 메뉴 전환, 목록·폼 열 배치, 패널 너비와 내부 표 스크롤 |
| 1280×800, 1440×900 | 사이드바·헤더·본문 정렬, 최대 콘텐츠 폭, 즐겨찾기와 상세 진입 |
| 높이 430px 보조 검증 | 긴 모달 본문 스크롤, 제목·닫기·저장 버튼 접근, 드롭다운 배치 |

- 로딩·빈 목록·오류·저장 중·저장 완료와 키보드 이동도 확인한다. 실제 모바일 키보드·터치는 기기에서 별도로 확인하고 데스크톱 viewport 검증과 구분해 기록한다.
