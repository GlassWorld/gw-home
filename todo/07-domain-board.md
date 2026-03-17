# 07. Domain: board

## 목표
게시글 CRUD, 목록 조회(페이징/검색), 카테고리

## DDL

```sql
-- sql/ddl/board/tb_board_category.sql
CREATE TABLE tb_board_category (
    board_category_idx   BIGSERIAL   PRIMARY KEY,
    board_category_uuid  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name                 VARCHAR(50) NOT NULL UNIQUE,
    sort_order           INT         NOT NULL DEFAULT 0,
    created_by           VARCHAR(100) NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- sql/ddl/board/tb_board_post.sql
CREATE TABLE tb_board_post (
    board_post_idx       BIGSERIAL    PRIMARY KEY,
    board_post_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    board_category_idx   BIGINT       NOT NULL,
    member_account_idx   BIGINT       NOT NULL,
    title                VARCHAR(300) NOT NULL,
    content              TEXT         NOT NULL,  -- 이미지는 URL로 본문에 포함
    view_count           INT          NOT NULL DEFAULT 0,
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
CREATE INDEX idx_board_post_category ON tb_board_post (board_category_idx);
CREATE INDEX idx_board_post_author ON tb_board_post (member_account_idx);
CREATE INDEX idx_board_post_created ON tb_board_post (created_at DESC);
```

## 생성 파일

```
api/src/main/java/com/gw/api/board/
├── controller/BoardController.java
├── service/BoardService.java
├── mapper/BoardMapper.java
└── dto/
    ├── CreateBoardPostRequest.java
    ├── UpdateBoardPostRequest.java
    ├── BoardPostResponse.java
    ├── BoardPostSummaryResponse.java
    └── BoardPostListRequest.java

infra-db/src/main/resources/mapper/board/BoardMapper.xml
infra-db/src/main/resources/sql/ddl/board/
├── tb_board_category.sql
└── tb_board_post.sql
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
BoardPostDto selectBoardPostByUuid(@Param("uuid") String uuid);
List<BoardPostSummaryDto> selectBoardPostList(@Param("req") BoardPostListRequest req);
long countBoardPostList(@Param("req") BoardPostListRequest req);
void insertBoardPost(BoardPostDto post);
int updateBoardPost(BoardPostDto post);
int deleteBoardPost(@Param("uuid") String uuid);
int incrementViewCount(@Param("uuid") String uuid);

// 카테고리
List<BoardCategoryDto> selectAllCategories();
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

- 조회 시 `view_count` +1 (별도 UPDATE)
- 수정/삭제: `member_account_idx` 일치 검증
- 삭제: `deleted_at` = now()
- 목록 쿼리: `deleted_at IS NULL` 필터 필수

## 완료 체크

- [ ] DDL 생성
- [ ] BoardMapper (interface + XML)
- [ ] DTO 생성
- [ ] BoardService
- [ ] BoardController
- [ ] 페이징 + 검색 쿼리 동작 확인
