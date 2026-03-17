# 07. Domain: board

## 목표
게시글 CRUD, 목록 조회(페이징/검색), 카테고리

## DDL

```sql
-- sql/ddl/board/tb_brd_ctgr.sql
CREATE TABLE tb_brd_ctgr (
    brd_ctgr_idx   BIGSERIAL   PRIMARY KEY,
    brd_ctgr_uuid  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm             VARCHAR(50) NOT NULL UNIQUE,
    sort_ord       INT         NOT NULL DEFAULT 0,
    created_by     VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- sql/ddl/board/tb_brd_pst.sql
CREATE TABLE tb_brd_pst (
    brd_pst_idx       BIGSERIAL    PRIMARY KEY,
    brd_pst_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_ctgr_idx      BIGINT       NOT NULL,
    mbr_acct_idx      BIGINT       NOT NULL,
    ttl               VARCHAR(300) NOT NULL,
    cntnt             TEXT         NOT NULL,  -- 이미지는 URL로 본문에 포함
    view_cnt          INT          NOT NULL DEFAULT 0,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at            TIMESTAMPTZ
);
CREATE INDEX idx_brd_pst_ctgr ON tb_brd_pst (brd_ctgr_idx);
CREATE INDEX idx_brd_pst_mbr_acct ON tb_brd_pst (mbr_acct_idx);
CREATE INDEX idx_brd_pst_created_at ON tb_brd_pst (created_at DESC);
```

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/
├── controller/board/BoardController.java
├── service/board/BoardService.java
└── dto/board/
    ├── CreateBoardPostRequest.java
    ├── UpdateBoardPostRequest.java
    ├── BoardPostResponse.java
    ├── BoardPostSummaryResponse.java
    └── BoardPostListRequest.java

{project}-infra-db/src/main/java/com/gw/infra/db/mapper/board/BoardMapper.java
{project}-infra-db/src/main/resources/mapper/board/BoardMapper.xml
{project}-infra-db/src/main/resources/sql/ddl/board/
├── tb_brd_ctgr.sql
└── tb_brd_pst.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/boards` | 게시글 목록 (페이징) | 불필요 |
| GET | `/api/v1/boards/{boardPostUuid}` | 게시글 상세 | 불필요 |
| POST | `/api/v1/boards` | 게시글 작성 | 필요 |
| PUT | `/api/v1/boards/{boardPostUuid}` | 게시글 수정 | 필요 (본인) |
| DELETE | `/api/v1/boards/{boardPostUuid}` | 게시글 삭제 | 필요 (본인) |

## Mapper 메서드

```java
// 게시글
BrdPstJvo selectBoardPostByUuid(@Param("uuid") String uuid);
List<BrdPstSmryJvo> selectBoardPostList(@Param("req") BrdPstListSrchVo req);
long countBoardPostList(@Param("req") BoardPostListRequest req);
void insertBoardPost(BrdPstVo post);
int updateBoardPost(BrdPstVo post);
int deleteBoardPost(@Param("uuid") String uuid);
int incrementViewCount(@Param("uuid") String uuid);

// 카테고리
List<BrdCtgrVo> selectAllCategories();
```

## BoardPostListRequest

```
categoryUuid: String (nullable)
keyword: String (nullable, title 검색)
page: int (default 1)
size: int (default 20)
```

## BoardPostResponse (상세)

```
boardPostUuid, categoryName, title, content, viewCount,
author(nickname), favoriteCount, commentCount,
createdAt, updatedAt
// _idx 제외
```

## BoardPostSummaryResponse (목록)

```
boardPostUuid, categoryName, title, viewCount,
author(nickname), favoriteCount, commentCount, createdAt
```

## 서비스 규칙

- 조회 시 `view_cnt` +1 (별도 UPDATE)
- 수정/삭제: `mbr_acct_idx` 일치 검증
- 삭제: `del_at` = now()
- 목록 쿼리: `del_at IS NULL` 필터 필수

## 완료 체크

- [x] DDL 생성
- [x] BoardMapper (interface + XML)
- [x] DTO 생성
- [x] BoardService
- [x] BoardController
- [x] 페이징 + 검색 쿼리 동작 확인
