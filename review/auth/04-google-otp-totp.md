# Review: Google OTP (TOTP) 2차 인증

> 작업 분류: **HEAVY**
> DB 스키마 변경 + 인증/보안 구조 변경 + share 모듈 변경 + Full-stack 동시 작업

---

## 작업 개요

Google Authenticator 호환 TOTP 기반 2차 인증을 기존 ID/PW 로그인에 추가.
OTP 미사용 계정은 기존 로그인 흐름 유지, OTP 사용 계정은 1차(ID/PW) → 2차(OTP) 순서로 인증 완료 후 JWT 발급.

---

## 변경 목적

셀프 프로젝트 보안 강화. 계정 탈취 시 2차 방어선 제공.

---

## 설계 결정 사항

### 1. otpTempToken 필요 여부

**필요함. 사용.**

1차 인증(ID/PW) 성공 후 즉시 JWT를 발급하면 OTP 검증 전에도 인증된 상태가 된다.
`type: "otp_temp"` claim을 가진 단기 JWT(5분)를 발급하고,
`POST /api/v1/auth/otp/verify` 에서 이 토큰을 Authorization 헤더로 받아 검증 후 실 JWT 발급.

- `JwtProvider` 에 `generateOtpTempToken(loginId)` 메서드 추가
- 5분 만료, type=`"otp_temp"` — DB 저장 불필요 (메모리 검증만)
- `JwtAuthenticationFilter` 에서 `otp_temp` 타입 토큰은 일반 인증 Principal로 등록하지 않음
  → `/api/v1/auth/otp/verify` 만 이 토큰을 허용

### 2. Secret 암호화

**AES-128/GCM, 자체 구현 `OtpSecretEncryptor` util.**

- Jasypt 의존성 추가 없이 `javax.crypto.Cipher` 사용
- 키는 `application.yml`: `otp.encryption-key` (Base64 인코딩된 16바이트)
- `OtpSecretEncryptor.encrypt(secret)` / `.decrypt(encrypted)` 형태로 분리

### 3. TOTP 라이브러리

**`com.eatthepath:java-otp:0.4.0`**

- RFC 6238 TOTP 준수
- ±1 window(±30초) 허용
- 의존성 간단, 별도 설정 불필요

### 4. 로그인 응답 타입 변경

`POST /api/v1/auth/login` 응답이 항상 `TokenResponse`였던 것을 `LoginResponse`로 교체.

```
LoginResponse {
  loginStatus: "SUCCESS" | "OTP_REQUIRED"
  tokenResponse: TokenResponse | null   // SUCCESS 시 채움
  otpTempToken: String | null           // OTP_REQUIRED 시 채움
}
```

프론트 `use-auth.ts` 의 `login()` 함수가 이 분기를 처리하도록 수정.

### 5. OTP 엔드포인트 보안

| 엔드포인트 | 보안 |
|-----------|------|
| `POST /api/v1/auth/otp/verify` | `permitAll()` — 내부에서 otpTempToken 직접 검증 |
| `POST /api/v1/auth/otp/setup` | `authenticated()` (실 access token 필요) |
| `POST /api/v1/auth/otp/activate` | `authenticated()` |
| `POST /api/v1/auth/otp/disable` | `authenticated()` |
| `GET /api/v1/auth/otp/status` | `authenticated()` |

---

## 예상 영향 범위

### Backend

#### DB (HEAVY)
| 대상 | 변경 내용 |
|------|-----------|
| `tb_mbr_acct` | 컬럼 4개 추가: `otp_enabled`, `otp_secret`, `otp_fail_cnt`, `otp_last_failed_at` |

#### share 모듈
| 파일 | 변경 내용 |
|------|-----------|
| `AcctVo.java` | OTP 필드 4개 추가 |

#### infra-db 모듈
| 파일 | 변경 내용 |
|------|-----------|
| `AccountMapper.java` | OTP 관련 메서드 추가 (`updateOtpSecret`, `updateOtpEnabled`, `updateOtpFail`, `resetOtpFail`) |
| `AccountMapper.xml` | 해당 SQL 추가, SELECT 컬럼 확장 |

#### api 모듈
| 파일 | 변경 내용 |
|------|-----------|
| `JwtProvider.java` | `generateOtpTempToken()`, `isOtpTempToken()` 추가 |
| `JwtAuthenticationFilter.java` | `otp_temp` 타입 토큰 필터 통과 제외 처리 |
| `SecurityConfig.java` | `/api/v1/auth/otp/verify` permitAll 추가 |
| `AuthController.java` | OTP 엔드포인트 5개 추가 |
| `AuthService.java` | `login()` 반환 타입 변경, OTP 검증/설정/해제 메서드 추가 |
| `LoginResponse.java` (신규) | loginStatus + tokenResponse + otpTempToken |
| `OtpSetupResponse.java` (신규) | otpAuthUrl, qrDataUrl(or Base64) |
| `OtpVerifyRequest.java` (신규) | otpCode, otpTempToken |
| `OtpActivateRequest.java` (신규) | otpCode |
| `OtpDisableRequest.java` (신규) | otpCode (현재 OTP 코드로 재확인) |
| `OtpStatusResponse.java` (신규) | otpEnabled |
| `OtpSecretEncryptor.java` (신규 util) | AES-GCM 암/복호화 |
| `OtpTotpUtil.java` (신규 util) | TOTP 생성/검증 |

### Frontend

| 파일 | 변경 내용 |
|------|-----------|
| `composables/use-auth.ts` | `login()` 반환 타입 변경, OTP 분기 처리 |
| `composables/useOtpApi.ts` (신규) | OTP setup/activate/verify/disable/status API |
| `types/api/auth.ts` | `LoginResponse` 타입 추가 |
| `pages/login.vue` | loginStep 상태 추가, OTP 입력 단계 분기 |
| `components/auth/OtpCodeInput.vue` (신규) | 6자리 숫자 입력 컴포넌트 |
| `pages/security/index.vue` (신규) | OTP 설정/해제 UI (3단계 플로우) |
| `components/common/AppHeader.vue` | 보안 설정 네비게이션 링크 추가 |

---

## DB 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성
  ```sql
  ALTER TABLE tb_mbr_acct
    ADD COLUMN otp_enabled        BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN otp_secret         TEXT,
    ADD COLUMN otp_fail_cnt       INT         NOT NULL DEFAULT 0,
    ADD COLUMN otp_last_failed_at TIMESTAMPTZ;
  ```
- [ ] 기존 데이터 영향 검토 (DEFAULT FALSE/NULL → 기존 계정 전부 OTP 비활성화, 영향 없음)
- [ ] NULL/DEFAULT 정책 확인 (`otp_secret` NULL 가능, 활성화 전까지 NULL)
- [ ] 인덱스 영향 확인 (OTP 컬럼 인덱스 불필요)
- [ ] 롤백 SQL 작성
  ```sql
  ALTER TABLE tb_mbr_acct
    DROP COLUMN otp_enabled,
    DROP COLUMN otp_secret,
    DROP COLUMN otp_fail_cnt,
    DROP COLUMN otp_last_failed_at;
  ```
- [ ] 운영 반영 순서: DB → 백엔드 빌드/배포 → 프론트 배포
- [ ] 데이터 마이그레이션 필요 여부: 불필요 (신규 컬럼, 기본값 설정)

---

## 보안 검토 포인트

| 항목 | 방침 |
|------|------|
| otpTempToken 탈취 | 5분 만료 + HTTPS 전제, DB 저장 안 해 revoke 불가 → 만료로 충분 |
| OTP 브루트포스 | 5회 실패 시 `otp_last_failed_at` 기록, 30분간 차단. `otp_fail_cnt` 리셋은 성공 시 |
| secret key 노출 | DB에 AES-GCM 암호화 후 저장, 키는 환경변수/application.yml |
| OTP 재사용 공격 | java-otp 라이브러리 window ±1 처리 내장. 동일 코드 재사용 차단은 이번 범위 제외 (복잡도 대비 셀프 프로젝트에서 리스크 낮음) |
| 자신의 OTP 강제 해제 | disable 시 현재 OTP 코드 재확인 필수 |
| QR 코드 데이터 | otpauth:// URL을 클라이언트에서 직접 QR 렌더링 (서버는 URL만 반환, 이미지 생성 불필요) |

---

## 리스크 분석

| 리스크 | 대응 |
|--------|------|
| `AuthService.login()` 반환 타입 변경으로 프론트 `use-auth.ts` 영향 | 타입 분기 명확히 처리, 기존 흐름 유지 |
| `JwtAuthenticationFilter` otp_temp 토큰 처리 누락 | 필터에서 type 확인 후 skip 처리 |
| `AcctVo` share 모듈 변경 → 전체 백엔드 영향 | OTP 필드만 추가, 기존 로직 무변경 |
| QR 라이브러리 없이 otpauth URL만 제공 | 프론트에서 `qrcode` npm 패키지로 클라이언트 렌더링 |
| 복구코드 없음 → OTP 분실 시 로그인 불가 | Admin의 `forceDisableOtp()` API로 긴급 해제 경로 확보 (선택 구현) |

---

## 제외 범위

- remember device 기능
- 복구코드 (Backup Code)
- OTP 재사용 방지 (used-code 목록 관리)
- Admin OTP 강제 해제 (리스크 항목 해소 용도로 검토)

---

## 전체 파일 목록 (신규 + 변경)

### 신규 파일 (12개)
```
gw-home-api/.../dto/auth/LoginResponse.java
gw-home-api/.../dto/auth/OtpSetupResponse.java
gw-home-api/.../dto/auth/OtpVerifyRequest.java
gw-home-api/.../dto/auth/OtpActivateRequest.java
gw-home-api/.../dto/auth/OtpDisableRequest.java
gw-home-api/.../dto/auth/OtpStatusResponse.java
gw-home-api/.../util/OtpSecretEncryptor.java
gw-home-api/.../util/OtpTotpUtil.java
gw-home-ui/composables/useOtpApi.ts
gw-home-ui/components/auth/OtpCodeInput.vue
gw-home-ui/pages/security/index.vue
gw-home-infra-db/.../sql/ddl/account/tb_mbr_acct_add_otp.sql  (ALTER + rollback)
```

### 변경 파일 (11개)
```
gw-home-share/.../vo/account/AcctVo.java
gw-home-infra-db/.../mapper/account/AccountMapper.java
gw-home-infra-db/.../mapper/account/AccountMapper.xml
gw-home-api/.../jwt/JwtProvider.java
gw-home-api/.../jwt/JwtAuthenticationFilter.java
gw-home-api/.../config/SecurityConfig.java
gw-home-api/.../controller/auth/AuthController.java
gw-home-api/.../service/auth/AuthService.java
gw-home-ui/composables/use-auth.ts
gw-home-ui/types/api/auth.ts
gw-home-ui/pages/login.vue
gw-home-ui/components/common/AppHeader.vue
```

---

## 관련 문서

- 기존 login 설계: `review/auth/02-login-redesign.md`
- 기존 JWT 흐름: `gw-home-api/.../jwt/JwtProvider.java`
- 기존 login API: `gw-home-api/.../service/auth/AuthService.java`
- 계정 테이블: `gw-home-infra-db/.../sql/ddl/account/tb_mbr_acct.sql`
