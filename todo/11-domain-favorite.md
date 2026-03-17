# 11. Domain: favorite

## 목표
게시글 좋아요 토글

## DDL

```sql
-- sql/ddl/favorite/tb_fav.sql
CREATE TABLE tb_fav (
    fav_idx             BIGSERIAL   PRIMARY KEY,
    fav_uuid            UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    trgt_type           VARCHAR(20) NOT NULL,   -- BOARD_POST
    trgt_idx            BIGINT      NOT NULL,
    mbr_acct_idx        BIGINT      NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (trgt_type, trgt_idx, mbr_acct_idx)
);
CREATE INDEX idx_fav_trgt ON tb_fav (trgt_type, trgt_idx);
CREATE INDEX idx_fav_mbr_acct ON tb_fav (mbr_acct_idx);
```

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/controller/favorite/FavoriteController.java
{project}-api/src/main/java/com/gw/api/service/favorite/FavoriteService.java
{project}-api/src/main/java/com/gw/api/dto/favorite/FavoriteResponse.java
{project}-share/src/main/java/com/gw/share/vo/favorite/FavVo.java
{project}-infra-db/src/main/java/com/gw/infra/db/mapper/favorite/FavoriteMapper.java
{project}-infra-db/src/main/resources/mapper/favorite/FavoriteMapper.xml
{project}-infra-db/src/main/resources/sql/ddl/favorite/tb_fav.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/boards/{boardPostUuid}/favorite` | 좋아요 토글 | 필요 |
| GET | `/api/v1/boards/{boardPostUuid}/favorite/count` | 좋아요 수 조회 | 불필요 |

## Mapper 메서드

```java
void insertFavorite(FavVo favorite);
int deleteFavorite(@Param("trgtType") String trgtType,
                   @Param("trgtIdx") Long trgtIdx,
                   @Param("mbrAcctIdx") Long mbrAcctIdx);
boolean existsFavorite(@Param("trgtType") String trgtType,
                       @Param("trgtIdx") Long trgtIdx,
                       @Param("mbrAcctIdx") Long mbrAcctIdx);
long countFavorite(@Param("trgtType") String trgtType,
                   @Param("trgtIdx") Long trgtIdx);
```

## FavoriteResponse

```
favorited: boolean,
favoriteCount: long
```

## 서비스 규칙

- 토글: 이미 좋아요 → 취소, 없으면 → 추가
- `trgt_type` 현재 `BOARD_POST`만 사용
- 카운트는 실시간 COUNT 쿼리 사용
- 게시글 상세/목록 `fav_cnt`는 `tb_fav` 기준으로 집계

## 완료 체크

- [x] DDL 생성
- [x] FavoriteMapper (interface + XML)
- [x] DTO 생성
- [x] FavoriteService (토글 로직)
- [x] FavoriteController
