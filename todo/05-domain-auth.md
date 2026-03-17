# 05. Domain: auth

## 목표
로그인, 로그아웃, JWT 액세스/리프레시 토큰 발급 및 갱신

## DDL

```sql
-- sql/ddl/auth/tb_auth_refresh_token.sql
CREATE TABLE tb_auth_refresh_token (
    auth_refresh_token_idx   BIGSERIAL    PRIMARY KEY,
    auth_refresh_token_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    member_account_idx       BIGINT       NOT NULL,
    token_hash               VARCHAR(255) NOT NULL,
    expires_at               TIMESTAMPTZ  NOT NULL,
    created_by               VARCHAR(100) NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ
);
CREATE INDEX idx_auth_refresh_token_member ON tb_auth_refresh_token (member_account_idx);
```

## 생성 파일

```
api/src/main/java/com/gw/api/auth/
├── controller/AuthController.java
├── service/AuthService.java
├── mapper/AuthMapper.java
├── dto/
│   ├── LoginRequest.java
│   ├── TokenResponse.java
│   └── RefreshRequest.java
└── jwt/
    ├── JwtProvider.java
    └── JwtAuthenticationFilter.java

api/src/main/java/com/gw/api/config/
└── SecurityConfig.java

infra-db/src/main/resources/mapper/auth/AuthMapper.xml
infra-db/src/main/resources/sql/ddl/auth/tb_auth_refresh_token.sql
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
- 로그아웃: refresh token `deleted_at` = now()
- 갱신: refresh token 유효성 검증 → 새 access token 발급
- refresh token은 해시 저장 (`SHA-256`)

## SecurityConfig 화이트리스트

```
POST /api/v1/accounts
POST /api/v1/auth/login
POST /api/v1/auth/refresh
```

## 완료 체크

- [ ] DDL 생성
- [ ] JwtProvider 생성
- [ ] JwtAuthenticationFilter 생성
- [ ] SecurityConfig 생성
- [ ] AuthMapper (interface + XML)
- [ ] AuthService
- [ ] AuthController
- [ ] 로그인 → 토큰 발급 테스트
- [ ] 토큰으로 인증 API 호출 테스트
