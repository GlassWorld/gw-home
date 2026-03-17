# 11. Domain: favorite

## 목표
게시글 좋아요 토글

## DDL

```sql
-- sql/ddl/favorite/tb_favorite.sql
CREATE TABLE tb_favorite (
    favorite_idx        BIGSERIAL   PRIMARY KEY,
    favorite_uuid       UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    target_type         VARCHAR(20) NOT NULL,   -- BOARD_POST
    target_idx          BIGINT      NOT NULL,
    member_account_idx  BIGINT      NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (target_type, target_idx, member_account_idx)
);
CREATE INDEX idx_favorite_target ON tb_favorite (target_type, target_idx);
CREATE INDEX idx_favorite_member ON tb_favorite (member_account_idx);
```

## 생성 파일

```
api/src/main/java/com/gw/api/favorite/
├── controller/FavoriteController.java
├── service/FavoriteService.java
├── mapper/FavoriteMapper.java
└── dto/
    └── FavoriteResponse.java

infra-db/src/main/resources/mapper/favorite/FavoriteMapper.xml
infra-db/src/main/resources/sql/ddl/favorite/tb_favorite.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/boards/{boardPostUuid}/favorite` | 좋아요 토글 | 필요 |
| GET | `/api/v1/boards/{boardPostUuid}/favorite/count` | 좋아요 수 조회 | 불필요 |

## Mapper 메서드

```java
void insertFavorite(FavoriteDto favorite);
int deleteFavorite(@Param("targetType") String targetType,
                   @Param("targetIdx") Long targetIdx,
                   @Param("memberAccountIdx") Long memberAccountIdx);
boolean existsFavorite(@Param("targetType") String targetType,
                       @Param("targetIdx") Long targetIdx,
                       @Param("memberAccountIdx") Long memberAccountIdx);
long countFavorite(@Param("targetType") String targetType,
                   @Param("targetIdx") Long targetIdx);
```

## FavoriteResponse

```
favorited: boolean,
favoriteCount: long
```

## 서비스 규칙

- 토글: 이미 좋아요 → 취소, 없으면 → 추가
- `target_type` 현재 `BOARD_POST`만 사용
- 카운트는 실시간 COUNT 쿼리 사용

## 완료 체크

- [ ] DDL 생성
- [ ] FavoriteMapper (interface + XML)
- [ ] DTO 생성
- [ ] FavoriteService (토글 로직)
- [ ] FavoriteController
