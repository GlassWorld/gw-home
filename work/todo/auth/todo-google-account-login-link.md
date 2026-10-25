# Google 계정 연동 로그인 계획

- 정리일: 2026-10-25
- 상태: 구현 커밋 반영 완료. 아래 본문은 구현 전 검토·계획으로 보존하며 신규 구현 지시로 사용하지 않는다.
- 구현 근거: `ff3a77d (API), 6c7c488 (UI), f97e496 (배포 환경변수)`.
- 관련 문서: [검토](../../review/auth/review-google-account-login-link.md)

## 현재 구현과 검증 범위

- AuthService의 Google 시작/로그인/연동/해제, AcctVo.googleSub와 계정 DDL, SettingsGoogleSection·로그인 버튼·콜백 화면이 구현되어 있다.
- 실제 Google 제공자 로그인·연동/해제와 대상 DB 마이그레이션 여부는 이번 문서 정리에서 검증하지 않았다.
- 이번 변경은 작업 이력 정리이며 해당 기능의 새 코드·정책 변경은 포함하지 않는다.

## Goal

- 목적:
  - 사용자가 환경설정에서 Google 계정을 기존 계정에 연동하고, 로그인 화면에서 비밀번호/OTP 외에 Google 로그인으로도 인증할 수 있게 한다.

## Scope

- 포함:
  - Google OAuth 설정값 추가
  - Google 계정 연동 상태 저장 구조 추가
  - 인증 API에 Google 로그인 시작/콜백 처리 추가
  - 계정 설정 API에 Google 연동 상태 조회/해제 기능 추가
  - 설정 화면의 Google 계정 연동 섹션 추가
  - 로그인 화면의 Google 로그인 버튼과 콜백 처리 화면 추가
  - 관련 백엔드 테스트와 프론트 타입 보강
- 제외:
  - Google 신규 가입 자동 생성
  - 여러 Google 계정 동시 연동
  - 다른 OAuth 제공자 확장
  - Google Admin SDK 또는 조직 도메인 제한

## Current State — 구현 전 기록

- 현재 상태:
  - 로그인은 `POST /api/v1/auth/login`에서 ID/비밀번호를 검증한 뒤 OTP 상태로 분기한다.
  - OTP 설정/검증/해제 API와 프론트 흐름은 이미 존재한다.
  - 환경설정 화면에는 프로필, 비밀번호, OTP 관련 섹션이 있다.
  - Google OAuth 연동 상태를 저장하거나 조회하는 구조는 없다.
- 제약:
  - 인증/보안 변경이므로 회귀 테스트가 필요하다.
  - Google OAuth Client ID/Secret과 Redirect URI는 환경변수 기반으로 관리해야 한다.
  - Google 이메일만으로 기존 계정을 자동 매칭하지 않고, 로그인된 사용자가 설정에서 명시적으로 연동한 provider subject를 기준으로 로그인해야 한다.

## Action Items — 구현 당시 계획

1. 백엔드 설정과 의존성을 확인하고 Google OAuth 검증 방식을 결정한다.
2. 계정 Google 연동 필드 또는 연동 테이블 DDL, VO/JVO, Mapper를 추가한다.
3. Google OAuth 시작, 콜백, 연동 상태 조회, 연동 해제 API와 DTO/convert를 구현한다.
4. Google 로그인 성공 후 OTP를 생략하고 기존 `LoginResponse`/`TokenResponse` 발급 흐름으로 연결한다.
5. 설정 화면에 Google 계정 연동 섹션을 추가하고 `pages/settings/index.vue`에 연결한다.
6. 로그인 화면에 Google 로그인 버튼과 OAuth 콜백 처리 페이지를 추가한다.
7. 백엔드 인증 테스트와 프론트 타입체크를 실행해 회귀를 확인한다.

## Done Criteria

- 로그인된 사용자가 환경설정에서 Google 계정을 연결하고 연결 상태를 확인할 수 있다.
- 로그인된 사용자가 환경설정에서 Google 계정 연결을 해제할 수 있다.
- 연결된 Google 계정으로 로그인하면 기존 토큰 발급 흐름으로 인증된다.
- OTP 요구 계정도 Google 로그인은 OTP를 생략하고 바로 인증된다.
- 연결되지 않은 Google 계정은 기존 계정에 임의로 로그인할 수 없다.
- 관련 백엔드 테스트와 프론트 타입체크가 통과한다.
