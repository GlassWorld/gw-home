# 06. Domain: profile

## 목표
프로필 조회/수정 (닉네임, 자기소개, 프로필 이미지 URL)

## DDL

```sql
-- sql/ddl/profile/tb_member_profile.sql
CREATE TABLE tb_member_profile (
    member_profile_idx    BIGSERIAL    PRIMARY KEY,
    member_profile_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    member_account_idx    BIGINT       NOT NULL UNIQUE,  -- FK to tb_member_account
    nickname              VARCHAR(50)  NOT NULL UNIQUE,
    introduction          VARCHAR(500),
    profile_image_url     VARCHAR(1000),
    created_by            VARCHAR(100) NOT NULL,
    updated_by            VARCHAR(100),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_member_profile_account ON tb_member_profile (member_account_idx);
```

## 생성 파일

```
api/src/main/java/com/gw/api/profile/
├── controller/ProfileController.java
├── service/ProfileService.java
├── mapper/ProfileMapper.java
└── dto/
    ├── ProfileResponse.java
    └── UpdateProfileRequest.java

infra-db/src/main/resources/mapper/profile/ProfileMapper.xml
infra-db/src/main/resources/sql/ddl/profile/tb_member_profile.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/profiles/{profileUuid}` | 프로필 조회 | 불필요 |
| GET | `/api/v1/profiles/me` | 내 프로필 조회 | 필요 |
| PUT | `/api/v1/profiles/me` | 프로필 수정 | 필요 |

## Mapper 메서드

```java
ProfileDto selectProfileByUuid(@Param("uuid") String uuid);
ProfileDto selectProfileByAccountIdx(@Param("memberAccountIdx") Long memberAccountIdx);
int updateProfile(ProfileDto profile);
void insertProfile(ProfileDto profile);  // 회원 가입 시 자동 생성용
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

- [ ] DDL 생성
- [ ] ProfileMapper (interface + XML)
- [ ] DTO 생성
- [ ] ProfileService
- [ ] ProfileController
