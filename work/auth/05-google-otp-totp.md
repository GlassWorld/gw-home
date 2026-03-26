# Work: Google OTP (TOTP) 2차 인증

> 연관 문서: [review/auth/04-google-otp-totp.md](../../review/auth/04-google-otp-totp.md), [todo/auth/05-google-otp-totp.md](../../todo/auth/05-google-otp-totp.md)

## 작업 범위

- `tb_mbr_acct` OTP 컬럼 추가 및 롤백 SQL 작성
- account/auth 백엔드에 OTP 저장, 검증, 임시 토큰 발급 흐름 추가
- 로그인 페이지 OTP 분기 및 보안 설정 화면 추가

## 구현 순서

1. DB DDL, `AcctVo`, `AccountMapper`에 OTP 필드와 SQL 반영
2. OTP util, DTO, JWT/AuthService/AuthController/SecurityConfig 반영
3. 프론트 타입, composable, OTP 입력 컴포넌트, 보안 설정 화면 반영
4. todo 체크 및 가능한 범위 빌드/타입 검증 수행

## 운영 반영 순서

1. `tb_mbr_acct_add_otp.sql` 반영
2. Backend 배포
3. Frontend 배포
4. 로그인 OTP 검증 및 보안 설정 화면 점검
5. 문제 발생 시 애플리케이션 롤백 후 OTP 컬럼 롤백 SQL 실행
