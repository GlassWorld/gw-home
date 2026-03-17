# 05. Domain: auth

## 목표
로그인, 로그아웃, JWT 액세스/리프레시 토큰 발급 및 갱신

## DDL

```sql
-- sql/ddl/auth/tb_auth_rfsh_tkn.sql
CREATE TABLE tb_auth_rfsh_tkn (
    auth_rfsh_tkn_idx   BIGSERIAL    PRIMARY KEY,
    auth_rfsh_tkn_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx        BIGINT       NOT NULL,
    tkn_hash            VARCHAR(255) NOT NULL,
    expr_at             TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at              TIMESTAMPTZ
);
CREATE INDEX idx_auth_rfsh_tkn_mbr_acct ON tb_auth_rfsh_tkn (mbr_acct_idx);
```

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/
├── controller/auth/AuthController.java
├── service/auth/AuthService.java
├── dto/auth/
│   ├── LoginRequest.java
│   ├── TokenResponse.java
│   └── RefreshRequest.java
└── jwt/
    ├── JwtProvider.java
    └── JwtAuthenticationFilter.java

{project}-api/src/main/java/com/gw/api/config/
└── SecurityConfig.java

{project}-infra-db/src/main/java/com/gw/infra/db/mapper/auth/AuthMapper.java
{project}-infra-db/src/main/resources/mapper/auth/AuthMapper.xml
{project}-infra-db/src/main/resources/sql/ddl/auth/tb_auth_rfsh_tkn.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/auth/login` | 로그인 | 불필요 |
| POST | `/api/v1/auth/logout` | 로그아웃 | 필요 |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 | 불필요 (refresh token) |

## JwtProvider

```java
String generateAccessToken(String loginId, String role)  // 만료: 30분
String generateRefreshToken(String loginId)              // 만료: 7일
String extractLoginId(String token)
boolean validate(String token)
```

## TokenResponse

```
accessToken, refreshToken, tokenType("Bearer"), expiresIn(초)
```

## 서비스 규칙

- 로그인: `loginId` + `password` 검증 → 두 토큰 발급 → refresh token DB 저장
- 로그아웃: refresh token `del_at` = now()
- 갱신: refresh token 유효성 검증 → 새 access token 발급
- refresh token은 해시 저장 (`SHA-256`)

## SecurityConfig 화이트리스트

```
POST /api/v1/accounts
POST /api/v1/auth/login
POST /api/v1/auth/refresh
```

## 완료 체크

- [x] DDL 생성
- [x] JwtProvider 생성
- [x] JwtAuthenticationFilter 생성
- [x] SecurityConfig 생성
- [x] AuthMapper (interface + XML)
- [x] AuthService
- [x] AuthController
- [x] 로그인 → 토큰 발급 테스트
- [x] 토큰으로 인증 API 호출 테스트
