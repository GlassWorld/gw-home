# 프론트 페이지 리팩토링 검토

## Scope

- 대상: `gw-home-ui/pages/**`, `gw-home-ui/components/common/AppHeader.vue`
- 관점: 300줄 이상 페이지 우선 정리, 스크립트 함수와 인터페이스 공통화, SVG 파일 공통화, 페이지/컴포넌트 책임 세분화

## Summary

- 한줄 요약: 리팩토링 우선순위는 명확하며, 현재는 "줄 수"보다 "페이지 안에 섞인 책임"을 기준으로 쪼개는 것이 효과가 가장 크다.
- 전체 판단: `7.9 / 10`

## Findings or Scores

### Finding 1

- 항목: `admin/accounts` 페이지에 조회, 생성, 상세 관리, 상태 변경, 임시 비밀번호 발급, 드롭다운 제어가 한 파일에 과밀하게 섞여 있다
- 점수: `6.8 / 10`
- 근거:
  - [`index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/admin/accounts/index.vue#L15)~[`index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/admin/accounts/index.vue#L50)에 페이지 상태가 집중되어 있다.
  - [`index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/admin/accounts/index.vue#L102)~[`index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/admin/accounts/index.vue#L445)에서 목록 조회, 생성, 상세 조회, 권한/상태 변경, 삭제, 잠금 해제, OTP 재설정, 비밀번호 재설정까지 모두 처리한다.
  - 템플릿도 [`index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/admin/accounts/index.vue#L448) 이후에 필터, 테이블, 관리 모달, 임시 비밀번호 모달이 함께 존재한다.
- 영향:
  - 이 파일은 1174줄로 가장 크고, 한 기능 수정이 다른 액션 흐름에 영향을 주기 쉬워 회귀 위험이 크다.
  - 우선순위 1로 `useAdminAccountManagement`, `AdminAccountFilterForm`, `AdminAccountTable`, `AdminAccountManageModal`, `TemporaryPasswordModal` 수준으로 분리하는 편이 좋다.

### Finding 2

- 항목: OTP 설정 로직이 `settings`, `security`, `login`에 반복되어 공통 composable 또는 feature component로 올라갈 필요가 있다
- 점수: `7.1 / 10`
- 근거:
  - [`settings/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/settings/index.vue#L96)~[`settings/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/settings/index.vue#L196)와 [`security/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/security/index.vue#L25)~[`security/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/security/index.vue#L125)는 OTP 상태 조회, QR 생성, 활성화, 해제 흐름이 거의 같다.
  - [`login.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/login.vue#L86)~[`login.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/login.vue#L130)도 OTP 등록 강제 플로우를 별도로 다시 구현하고 있다.
  - QR 코드 생성도 [`settings/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/settings/index.vue#L123)~[`settings/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/settings/index.vue#L126), [`security/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/security/index.vue#L52)~[`security/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/security/index.vue#L55), [`login.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/login.vue#L97)~[`login.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/login.vue#L100)에서 중복된다.
- 영향:
  - 문구만 다르고 로직이 같은 코드가 늘어나면 버그 수정이 세 군데로 퍼진다.
  - `features/auth` 또는 `features/security` 아래에 `useOtpSetupFlow`와 `OtpSetupPanel`, `OtpDisableModal` 형태로 공통화하면 효과가 크다.

### Finding 3

- 항목: 공지 상세/마크다운 렌더링/쿼리 동기화 로직이 페이지별로 흩어져 있어 공통 유틸과 화면 조합 컴포넌트가 필요하다
- 점수: `7.6 / 10`
- 근거:
  - [`dashboard/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/dashboard/index.vue#L200)~[`dashboard/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/dashboard/index.vue#L260)와 [`notices/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/notices/index.vue#L69)~[`notices/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/notices/index.vue#L167)에 `renderMarkdown`, 상세 조회, 모달 열기/닫기 흐름이 반복된다.
  - `dashboard`에는 날짜 포맷터와 주간 계산 유틸도 함께 들어 있어 [`dashboard/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/dashboard/index.vue#L39)~[`dashboard/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/dashboard/index.vue#L118) 구간이 페이지 책임을 무겁게 만든다.
- 영향:
  - 공지 상세 방식이나 sanitize 정책이 바뀌면 같은 수정이 여러 페이지에 필요하다.
  - `utils/date.ts`, `utils/markdown.ts`, `features/notice/components/NoticeDetailModal.vue` 수준으로 올리면 `dashboard`와 `notices`가 얇아진다.

### Finding 4

- 항목: 인라인 SVG와 페이지 내부 타입 선언이 아직 남아 있어 공통 자산/타입 진입점 정리가 덜 끝난 상태다
- 점수: `8.0 / 10`
- 근거:
  - 인라인 SVG는 [`AppHeader.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/components/common/AppHeader.vue#L92)~[`AppHeader.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/components/common/AppHeader.vue#L124)에 남아 있다.
  - 페이지 내부 타입 선언은 [`login.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/login.vue#L8)처럼 아직 파일 내부에 존재한다.
  - 현재 300줄 이상 페이지는 `admin/accounts` 1174줄, `dashboard` 664줄, `settings` 574줄, `admin/notices` 492줄, `login` 474줄, `notices` 430줄, `vault` 393줄, `security` 360줄, `admin/vault-categories` 360줄, `admin/daily-reports` 311줄이다.
- 영향:
  - SVG 변경 시 컴포넌트 수정이 필요하고, 타입/상수의 재사용 경로도 약해진다.
  - `assets/icons/*.svg` 또는 `components/icons/*`로 옮기고, 화면 전용 타입은 `features/{domain}/types` 또는 루트 `types`로 정리하는 편이 유지보수에 유리하다.

### Finding 5

- 항목: 페이지 scoped CSS 안에 반복되는 레이아웃 패턴이 많아 공통 스타일 계층이 필요하다
- 점수: `7.4 / 10`
- 근거:
  - [`settings/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/settings/index.vue#L424) 이후와 [`security/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/security/index.vue#L248) 이후를 보면 `hero`, `section-header`, `otp-grid`, `otp panel`, `22px/28px padding`, 모바일 1열 전환 규칙이 거의 같은 구조로 반복된다.
  - [`notices/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/notices/index.vue#L274) 이후, [`dashboard/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/dashboard/index.vue#L450) 이후, [`vault/index.vue`](/home/glassworld/workspace/gw-home/gw-home-ui/pages/vault/index.vue#L276) 이후에도 `display: grid`, `gap: 16px`, `content-panel` 조합, `section-header` 계열 스타일이 반복된다.
  - `@media (max-width: 768px)` 대응도 여러 페이지에서 비슷한 1열 전환 규칙으로 중복된다.
- 영향:
  - 화면을 조금만 수정해도 각 페이지의 scoped CSS를 따로 손봐야 해서 스타일 유지비가 커진다.
  - `assets/css` 또는 공통 레이아웃 컴포넌트 기준으로 `page-hero`, `page-section-header`, `panel-grid`, `otp-setup-grid`, `page-actions` 같은 공통 클래스나 토큰을 만들면 줄 수와 중복 수정이 함께 줄어든다.

## Recommended Next Actions

1. 1차 리팩토링 범위를 `admin/accounts`, `settings`, `security`, `login`, `dashboard`, `notices`로 고정하고, 300줄 초과 페이지 중 책임 밀도가 높은 화면부터 처리한다.
2. 먼저 공통 로직을 `useOtpSetupFlow`, `utils/markdown.ts`, `utils/date.ts`처럼 뽑고, 그 다음 페이지를 얇게 만드는 순서로 진행한다.
3. CSS는 공통 토큰과 레이아웃 패턴부터 분리하고, 페이지 scoped CSS는 화면 고유 스타일만 남기도록 정리한다.
4. SVG는 `AppHeader`부터 파일 또는 아이콘 컴포넌트로 바꾸고, 이후 페이지 내부 타입과 상수 선언을 feature-local 타입 파일로 이동한다.
