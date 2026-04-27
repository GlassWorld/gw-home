# 관리자 OTP 선택 기능 검토

## Scope

- 대상:
  - 관리자 계정 관리 화면에서 OTP 사용 여부를 선택하는 기능
  - 로그인 시 OTP 요구 정책 분기
  - 관리자용 계정 관리 API와 계정 응답 모델
- 관점:
  - 현재 OTP 정책과의 충돌 여부
  - 최소 변경으로 요구사항을 수용할 수 있는지
  - 운영 중 계정의 기존 로그인 흐름에 미치는 영향

## Summary

- 한줄 요약:
  - 현재 구조는 `otp_enabled`만으로 OTP 등록 상태와 로그인 강제 정책을 동시에 표현하고 있어, 관리자가 선택하는 정책을 넣으려면 계정 정책 필드를 분리해야 한다.
- 전체 판단:
  - Full-stack + HEAVY 작업이다. 관리자 화면, 관리자 API, 로그인 정책, 계정 모델이 함께 변경된다.

## Findings or Scores

- 항목:
  - 로그인 정책과 OTP 등록 상태가 한 필드에 결합되어 있다.
- 점수:
  - 위험도 높음
- 근거:
  - `AuthService.login()`은 `otp_enabled = true`이면 OTP 인증, `otp_enabled = false`이면 `OTP_SETUP_REQUIRED`를 반환한다.
  - 즉 현재는 "OTP 등록 안 됨"이 곧 "다음 로그인에서 등록 강제"를 의미한다.
- 영향:
  - 관리자 선택 기능을 단순 토글로 추가하면 로그인 흐름이 깨지거나 의미가 뒤섞일 수 있다.

- 항목:
  - 관리자 기능은 현재 OTP 초기화만 제공한다.
- 점수:
  - 위험도 중간
- 근거:
  - 관리자 서비스와 화면에는 OTP 실패 초기화, OTP 재등록 초기화만 있고, OTP 요구 정책을 수정하는 API가 없다.
- 영향:
  - 관리자 화면 버튼 추가만으로는 요구사항을 충족할 수 없고 신규 API와 응답 필드가 필요하다.

- 항목:
  - 현행 DB 스키마에는 관리자 선택용 정책 필드가 없다.
- 점수:
  - 위험도 높음
- 근거:
  - 계정 VO와 DDL에는 `otp_enabled`, `otp_secret`, 실패 횟수만 존재한다.
- 영향:
  - "OTP 사용 여부를 관리자가 선택"을 계정 정책으로 해석하면 DDL 변경이 필요하다.

## Recommended Next Actions

1. OTP 정책과 OTP 등록 상태를 분리한다. 예: `otp_required` 또는 동등한 정책 필드를 계정에 추가한다.
2. 로그인 로직을 `otp_required=false -> 일반 로그인 성공`, `otp_required=true + otp_enabled=false -> OTP_SETUP_REQUIRED`, `otp_required=true + otp_enabled=true -> OTP_REQUIRED`로 재정의한다.
3. 관리자 계정 상세/수정 API와 관리자 모달 UI에 OTP 정책 선택 기능을 추가하고, 회귀 테스트를 보강한다.
