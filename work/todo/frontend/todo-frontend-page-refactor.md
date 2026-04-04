# 프론트 페이지 리팩토링 계획

- 이 계획은 300줄 이상 페이지를 우선 대상으로 하여 스크립트, 타입, SVG, CSS 공통화와 페이지 책임 분리를 단계적으로 진행하기 위한 문서다.

## Goal

- 목적:
  - 300줄 이상 프론트 페이지를 우선 정리해 페이지 책임을 줄이고, 반복되는 스크립트 함수, 인터페이스, SVG, CSS를 공통화한다.

## Scope

- 포함:
  - `gw-home-ui/pages/admin/accounts/index.vue`
  - `gw-home-ui/pages/settings/index.vue`
  - `gw-home-ui/pages/security/index.vue`
  - `gw-home-ui/pages/login.vue`
  - `gw-home-ui/pages/dashboard/index.vue`
  - `gw-home-ui/pages/notices/index.vue`
  - `gw-home-ui/components/common/AppHeader.vue`
  - `gw-home-ui/components/common`, `gw-home-ui/features/*`, `gw-home-ui/types/*`, `gw-home-ui/utils/*`, `gw-home-ui/assets/*`
- 제외:
  - 백엔드 API 변경
  - 라우트 구조 변경
  - 디자인 전면 개편
  - 우선순위 밖 300줄 이하 페이지의 대규모 재작성

## Current State

- 현재 상태:
  - `admin/accounts`는 목록, 생성, 상세 관리, 액션 처리까지 한 파일에 몰려 있다.
  - OTP 설정 흐름은 `settings`, `security`, `login`에 중복 구현되어 있다.
  - 공지 상세 조회와 마크다운 렌더링은 `dashboard`, `notices`, `admin/notices`에 분산되어 있다.
  - 인라인 SVG는 `AppHeader`에 남아 있고, 일부 화면 타입은 페이지 내부에 선언되어 있다.
  - scoped CSS 안에 `hero`, `section-header`, `actions`, `otp-grid`, `22px/28px 패널` 같은 패턴이 반복된다.
- 제약:
  - 기존 라우트와 사용자 동작은 유지해야 한다.
  - `any` 없이 타입을 유지해야 한다.
  - 공통화는 과도한 추상화보다 현재 중복이 분명한 범위에 한정해야 한다.
  - 기존 공통 컴포넌트 확장 가능성을 먼저 검토해야 한다.

## Action Items

1. 공통 로직 기반부터 정리한다.
   - `gw-home-ui/utils/date.ts` 또는 기존 유틸 위치에 날짜 포맷터와 주간 계산 로직을 이동한다.
   - `gw-home-ui/utils/markdown.ts` 또는 feature-local 유틸에 마크다운 렌더링과 sanitize 로직을 이동한다.
   - `gw-home-ui/features/security/composables/use-otp-setup-flow.ts` 같은 공통 OTP 흐름 composable 후보를 만든다.

2. 공통 UI와 자산을 분리한다.
   - `gw-home-ui/components/icons/*` 또는 `gw-home-ui/assets/icons/*`로 `AppHeader` 인라인 SVG를 분리한다.
   - OTP 등록/해제 UI를 `OtpSetupPanel`, `OtpDisableModal` 같은 공통 컴포넌트 후보로 정리한다.
   - 공지 상세 모달을 `features/notice/components/NoticeDetailModal.vue` 같은 재사용 가능한 단위로 분리한다.

3. 공통 CSS 계층을 정리한다.
   - `gw-home-ui/assets/css` 또는 현재 전역 스타일 진입점에 공통 레이아웃 클래스 후보를 만든다.
   - 후보 예시: `page-hero`, `page-section-header`, `page-actions`, `panel-grid`, `otp-setup-grid`, `panel-padding-md`, `panel-padding-lg`
   - 각 페이지의 scoped CSS는 화면 고유 스타일만 남기고, 반복 레이아웃은 공통 클래스로 이동한다.

4. 페이지별 1차 리팩토링을 진행한다.
   - `admin/accounts`는 `useAdminAccountManagement`, `AdminAccountFilterForm`, `AdminAccountTable`, `AdminAccountManageModal`, `TemporaryPasswordModal` 수준으로 분리한다.
   - `settings`, `security`, `login`은 OTP 흐름과 관련 스타일을 공통 로직과 공통 UI 기반으로 재구성한다.
   - `dashboard`, `notices`는 공지 상세, 마크다운, 날짜 유틸을 사용하도록 얇게 정리한다.

5. 타입과 파일 구조를 정리한다.
   - 페이지 내부 타입과 화면 전용 인터페이스를 `features/{domain}/types` 또는 루트 `types`로 이동한다.
   - 공통 타입과 feature-local 타입의 경계를 정리해 재사용 경로를 명확히 한다.

6. 검증과 후속 범위를 정리한다.
   - 리팩토링 후 주요 라우트별 동작과 모달 흐름을 확인한다.
   - 1차 대상 완료 후 `admin/notices`, `vault`, `admin/vault-categories`, `admin/daily-reports`를 2차 대상으로 검토한다.

## Done Criteria

- 1차 대상 페이지에서 반복 로직이 공통 util, composable, component로 이동되어 페이지 파일 책임이 감소한다.
- OTP 흐름은 최소 2개 이상 페이지에서 같은 공통 로직 또는 공통 UI를 사용한다.
- `AppHeader` 인라인 SVG가 파일 또는 전용 아이콘 컴포넌트로 분리된다.
- 반복 CSS 패턴이 공통 클래스나 공통 스타일 계층으로 이동하고, 페이지 scoped CSS는 화면 고유 영역 위주로 축소된다.
- 주요 동작인 로그인, OTP 설정/해제, 공지 상세 모달, 관리자 계정 관리 흐름이 기존과 동일하게 동작한다.
