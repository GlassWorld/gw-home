# 08. Domain: comment

## 목표
댓글 및 대댓글 CRUD

## DDL

```sql
-- sql/ddl/comment/tb_board_comment.sql
CREATE TABLE tb_board_comment (
    board_comment_idx    BIGSERIAL    PRIMARY KEY,
    board_comment_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    board_post_idx       BIGINT       NOT NULL,
    parent_comment_idx   BIGINT,                      -- NULL이면 최상위 댓글
    member_account_idx   BIGINT       NOT NULL,
    content              VARCHAR(2000) NOT NULL,
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
CREATE INDEX idx_board_comment_post ON tb_board_comment (board_post_idx);
CREATE INDEX idx_board_comment_parent ON tb_board_comment (parent_comment_idx);
```

## 생성 파일

```
api/src/main/java/com/gw/api/comment/
├── controller/CommentController.java
├── service/CommentService.java
├── mapper/CommentMapper.java
└── dto/
    ├── CreateCommentRequest.java
    ├── UpdateCommentRequest.java
    └── CommentResponse.java

infra-db/src/main/resources/mapper/comment/CommentMapper.xml
infra-db/src/main/resources/sql/ddl/comment/tb_board_comment.sql
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
List<CommentDto> selectCommentsByPostIdx(@Param("boardPostIdx") Long boardPostIdx);
void insertComment(CommentDto comment);
int updateComment(CommentDto comment);
int deleteComment(@Param("uuid") String uuid);
long countCommentsByPostIdx(@Param("boardPostIdx") Long boardPostIdx);
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
- 삭제: `deleted_at` = now(), 대댓글은 유지 (부모만 삭제 표시)
- 삭제된 댓글 content = "삭제된 댓글입니다" 표시

## 완료 체크

- [ ] DDL 생성
- [ ] CommentMapper (interface + XML)
- [ ] DTO 생성
- [ ] CommentService (트리 구조 조립 포함)
- [ ] CommentController
