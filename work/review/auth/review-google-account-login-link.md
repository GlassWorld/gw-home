# Google 계정 연동 로그인 검토

## Scope

- 대상:
  - 환경설정 화면의 Google 계정 연동 UI
  - 로그인 화면의 Google 로그인 진입점
  - Google OAuth 콜백 처리와 기존 JWT 발급 흐름
  - 계정 모델, Mapper, DDL의 외부 로그인 식별자 저장 구조
- 관점:
  - OTP 로그인 정책과의 공존 여부
  - 기존 로그인 ID/비밀번호 계정과 Google 계정 연결 방식
  - 보안, 토큰 발급, 계정 매칭의 최소 변경 범위

## Summary

- 한줄 요약:
  - Google 로그인은 단순 버튼 추가가 아니라 계정 연동 상태 저장, OAuth 콜백 검증, 기존 토큰 발급 흐름 연결이 필요한 Full-stack 인증 변경이다.
- 전체 판단:
  - HEAVY 작업이다. 프론트 설정/로그인 화면, 백엔드 인증 API, 계정 저장 모델, 보안 설정, 테스트가 함께 변경된다.

## Findings or Scores

- 항목:
  - 현재 인증 흐름은 로그인 ID/비밀번호 이후 OTP 상태에 따라 `SUCCESS`, `OTP_SETUP_REQUIRED`, `OTP_REQUIRED`로 분기한다.
- 점수:
  - 위험도 높음
- 근거:
  - Google 로그인 성공 후에도 기존 `TokenResponse` 발급 구조로 귀결되어야 하며, OTP 정책 적용 여부를 명확히 정해야 한다.
- 영향:
  - Google 로그인은 Google 인증을 신뢰하여 OTP를 우회하고, ID/비밀번호 로그인은 기존 OTP 정책을 유지한다.

- 항목:
  - 환경설정에는 OTP와 프로필/비밀번호 섹션이 있으나 외부 계정 연동 섹션은 없다.
- 점수:
  - 위험도 중간
- 근거:
  - `gw-home-ui/features/settings/components` 아래에 설정 섹션 컴포넌트가 분리되어 있어 새 섹션 추가는 자연스럽다.
- 영향:
  - Google 연동 상태 조회, 연결 시작, 연결 해제 API와 프론트 타입이 필요하다.

- 항목:
  - 현재 계정 DDL과 VO에는 Google 제공자 식별자 저장 필드가 확인되지 않는다.
- 점수:
  - 위험도 높음
- 근거:
  - Google 로그인은 이메일만으로 매칭하면 계정 탈취 위험이 있으므로 provider와 provider subject 값을 별도 저장해야 한다.
- 영향:
  - 계정 테이블 또는 별도 외부 계정 연동 테이블 추가가 필요하며, DB 마이그레이션과 Mapper 변경이 수반된다.

## Recommended Next Actions

1. Google 연동 저장 모델을 정한다. 최소 변경은 계정 테이블에 `google_sub`, `google_email`, `google_linked_at` 계열 필드를 추가하는 방식이다.
2. 백엔드에 Google OAuth 시작/콜백/연동 상태/연동 해제 API를 추가하고, 콜백 성공 시 기존 JWT 발급 응답으로 변환한다.
3. 프론트 환경설정에 Google 계정 연동 섹션을 추가하고, 로그인 화면에는 Google 로그인 버튼과 콜백 처리 페이지를 추가한다.
4. Google 로그인은 OTP를 생략하고 바로 기존 JWT 발급 흐름으로 연결한다.
