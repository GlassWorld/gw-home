# 04. Domain: account

## 목표
회원 가입, 탈퇴, 계정 정보 관리

## DDL

```sql
-- sql/ddl/account/tb_member_account.sql
CREATE TABLE tb_member_account (
    member_account_idx   BIGSERIAL    PRIMARY KEY,
    member_account_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    login_id             VARCHAR(100) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    role                 VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER, ADMIN
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
```

## 생성 파일

```
api/src/main/java/com/gw/api/account/
├── controller/AccountController.java
├── service/AccountService.java
├── mapper/AccountMapper.java
└── dto/
    ├── SignUpRequest.java
    ├── AccountResponse.java
    └── UpdateAccountRequest.java

infra-db/src/main/resources/mapper/account/AccountMapper.xml
infra-db/src/main/resources/sql/ddl/account/tb_member_account.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/accounts` | 회원 가입 | 불필요 |
| GET | `/api/v1/accounts/me` | 내 계정 조회 | 필요 |
| PUT | `/api/v1/accounts/me` | 계정 수정 (이메일) | 필요 |
| DELETE | `/api/v1/accounts/me` | 탈퇴 (소프트) | 필요 |

## Mapper 메서드

```java
void insertAccount(AccountDto account);
AccountDto selectAccountByLoginId(@Param("loginId") String loginId);
AccountDto selectAccountByUuid(@Param("uuid") String uuid);
int updateAccount(AccountDto account);
int deleteAccount(@Param("uuid") String uuid);  // deleted_at = now()
boolean existsByLoginId(@Param("loginId") String loginId);
boolean existsByEmail(@Param("email") String email);
```

## SignUpRequest 검증 규칙

```
loginId: @NotBlank, @Size(min=4, max=30), @Pattern(alphanumeric + underscore)
password: @NotBlank, @Size(min=8, max=100)
email: @NotBlank, @Email
```

## AccountResponse (외부 노출)

```
memberAccountUuid, loginId, email, role, createdAt
// _idx 제외
```

## 서비스 규칙

- 가입 시 `loginId`, `email` 중복 확인
- 비밀번호 `BCryptPasswordEncoder` 암호화
- 탈퇴: `deleted_at` = now() (소프트 삭제)
- `created_by` = 가입 시 `loginId`

## 완료 체크

- [x] DDL 생성
- [x] AccountMapper (interface + XML)
- [x] SignUpRequest / AccountResponse / UpdateAccountRequest DTO
- [x] AccountService
- [x] AccountController
- [x] API 테스트 (POST /api/v1/accounts)
