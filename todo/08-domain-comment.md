# 08. Domain: comment

## 목표
댓글 및 대댓글 CRUD

## DDL

```sql
-- sql/ddl/comment/tb_brd_cmt.sql
CREATE TABLE tb_brd_cmt (
    brd_cmt_idx         BIGSERIAL    PRIMARY KEY,
    brd_cmt_uuid        UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_pst_idx         BIGINT       NOT NULL,
    prnt_brd_cmt_idx    BIGINT,                      -- NULL이면 최상위 댓글
    mbr_acct_idx        BIGINT       NOT NULL,
    cntnt               VARCHAR(2000) NOT NULL,
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at               TIMESTAMPTZ
);
CREATE INDEX idx_brd_cmt_brd_pst ON tb_brd_cmt (brd_pst_idx);
CREATE INDEX idx_brd_cmt_prnt ON tb_brd_cmt (prnt_brd_cmt_idx);
```

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/
├── controller/comment/CommentController.java
├── service/comment/CommentService.java
└── dto/comment/
    ├── CreateCommentRequest.java
    ├── UpdateCommentRequest.java
    └── CommentResponse.java

{project}-infra-db/src/main/java/com/gw/infra/db/mapper/comment/CommentMapper.java
{project}-infra-db/src/main/resources/mapper/comment/CommentMapper.xml
{project}-infra-db/src/main/resources/sql/ddl/comment/tb_brd_cmt.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/boards/{boardPostUuid}/comments` | 댓글 목록 | 불필요 |
| POST | `/api/v1/boards/{boardPostUuid}/comments` | 댓글 작성 | 필요 |
| PUT | `/api/v1/comments/{commentUuid}` | 댓글 수정 | 필요 (본인) |
| DELETE | `/api/v1/comments/{commentUuid}` | 댓글 삭제 | 필요 (본인) |

## Mapper 메서드

```java
List<BrdCmtJvo> selectCommentsByBrdPstIdx(@Param("brdPstIdx") Long brdPstIdx);
void insertComment(BrdCmtVo cmt);
int updateComment(BrdCmtVo cmt);
int deleteComment(@Param("uuid") String uuid);
long countCommentsByBrdPstIdx(@Param("brdPstIdx") Long brdPstIdx);
```

## CreateCommentRequest

```
parentCommentUuid: String (nullable, 대댓글 시 부모 UUID)
content: @NotBlank, @Size(max=2000)
```

## CommentResponse

```
boardCommentUuid, content, author(nickname),
parentCommentUuid (nullable), replies(List<CommentResponse>),
createdAt, updatedAt
```

## 서비스 규칙

- 목록 조회: 최상위 댓글 + 대댓글 트리 구조로 반환
- 삭제: `del_at` = now(), 대댓글은 유지 (부모만 삭제 표시)
- 삭제된 댓글 content = "삭제된 댓글입니다" 표시

## 완료 체크

- [x] DDL 생성
- [x] CommentMapper (interface + XML)
- [x] DTO 생성
- [x] CommentService (트리 구조 조립 포함)
- [x] CommentController
