# 04. Domain: account

## 목표
회원 가입, 탈퇴, 계정 정보 관리

## DDL

```sql
-- sql/ddl/account/tb_mbr_acct.sql
CREATE TABLE tb_mbr_acct (
    mbr_acct_idx   BIGSERIAL    PRIMARY KEY,
    mbr_acct_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lgn_id         VARCHAR(100) NOT NULL UNIQUE,
    pwd            VARCHAR(255) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    role                 VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER, ADMIN
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at               TIMESTAMPTZ
);
```

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/
├── controller/account/AccountController.java
├── service/account/AccountService.java
└── dto/account/
    ├── SignUpRequest.java
    ├── AccountResponse.java
    └── UpdateAccountRequest.java
{project}-infra-db/src/main/java/com/gw/infra/db/
└── mapper/account/AccountMapper.java

{project}-infra-db/src/main/resources/mapper/account/AccountMapper.xml
{project}-infra-db/src/main/resources/sql/ddl/account/tb_mbr_acct.sql
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
void insertAccount(AcctVo acct);
AcctVo selectAccountByLoginId(@Param("lgnId") String lgnId);
AcctVo selectAccountByUuid(@Param("uuid") String uuid);
AcctVo selectAccountByIdx(@Param("idx") Long idx);
int updateAccount(AcctVo acct);
int deleteAccount(@Param("uuid") String uuid);  // del_at = now()
boolean existsByLoginId(@Param("lgnId") String lgnId);
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
- 탈퇴: `del_at` = now() (소프트 삭제)
- `created_by` = 가입 시 `loginId`

## 완료 체크

- [x] DDL 생성
- [x] AccountMapper (interface + XML)
- [x] SignUpRequest / AccountResponse / UpdateAccountRequest DTO
- [x] AccountService
- [x] AccountController
- [x] API 테스트 (POST /api/v1/accounts)
