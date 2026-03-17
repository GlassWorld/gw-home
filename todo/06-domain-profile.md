# 06. Domain: profile

## 목표
프로필 조회/수정 (닉네임, 자기소개, 프로필 이미지 URL)

## DDL

```sql
-- sql/ddl/profile/tb_mbr_prfl.sql
CREATE TABLE tb_mbr_prfl (
    mbr_prfl_idx    BIGSERIAL    PRIMARY KEY,
    mbr_prfl_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx    BIGINT       NOT NULL UNIQUE,  -- FK to tb_mbr_acct
    nick_nm         VARCHAR(50)  NOT NULL UNIQUE,
    intro           VARCHAR(500),
    prfl_img_url    VARCHAR(1000),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mbr_prfl_mbr_acct ON tb_mbr_prfl (mbr_acct_idx);
```

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/
├── controller/profile/ProfileController.java
├── service/profile/ProfileService.java
└── dto/profile/
    ├── ProfileResponse.java
    └── UpdateProfileRequest.java

{project}-infra-db/src/main/java/com/gw/infra/db/mapper/profile/ProfileMapper.java
{project}-infra-db/src/main/resources/mapper/profile/ProfileMapper.xml
{project}-infra-db/src/main/resources/sql/ddl/profile/tb_mbr_prfl.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/profiles/{profileUuid}` | 프로필 조회 | 불필요 |
| GET | `/api/v1/profiles/me` | 내 프로필 조회 | 필요 |
| PUT | `/api/v1/profiles/me` | 프로필 수정 | 필요 |

## Mapper 메서드

```java
PrflVo selectProfileByUuid(@Param("uuid") String uuid);
PrflVo selectProfileByAccountIdx(@Param("mbrAcctIdx") Long mbrAcctIdx);
int updateProfile(PrflVo prfl);
void insertProfile(PrflVo prfl);  // 회원 가입 시 자동 생성용
```

## UpdateProfileRequest

```
nickname: @Size(max=50)
introduction: @Size(max=500)
profileImageUrl: URL (file 도메인에서 업로드 후 전달)
```

## ProfileResponse (외부 노출)

```
memberProfileUuid, nickname, introduction, profileImageUrl, createdAt
```

## 서비스 규칙

- 프로필은 회원 1:1 관계
- `account` 가입 완료 시 기본 프로필 자동 생성 (nickname = loginId)
- 프로필 이미지 변경: file 도메인에서 업로드 후 URL만 받아서 저장

## 완료 체크

- [x] DDL 생성
- [x] ProfileMapper (interface + XML)
- [x] DTO 생성
- [x] ProfileService
- [x] ProfileController
