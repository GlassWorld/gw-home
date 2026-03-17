# 10. Domain: tag

## 목표
태그 생성/조회 및 게시글-태그 매핑 관리

## DDL

```sql
-- sql/ddl/tag/tb_tag.sql
CREATE TABLE tb_tag (
    tag_idx    BIGSERIAL   PRIMARY KEY,
    tag_uuid   UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- sql/ddl/tag/tb_board_post_tag.sql
CREATE TABLE tb_board_post_tag (
    board_post_tag_idx   BIGSERIAL PRIMARY KEY,
    board_post_idx       BIGINT    NOT NULL,
    tag_idx              BIGINT    NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (board_post_idx, tag_idx)
);
CREATE INDEX idx_board_post_tag_post ON tb_board_post_tag (board_post_idx);
CREATE INDEX idx_board_post_tag_tag ON tb_board_post_tag (tag_idx);
```

## 생성 파일

```
api/src/main/java/com/gw/api/tag/
├── controller/TagController.java
├── service/TagService.java
├── mapper/TagMapper.java
└── dto/
    ├── TagResponse.java
    └── AttachTagRequest.java

infra-db/src/main/resources/mapper/tag/TagMapper.xml
infra-db/src/main/resources/sql/ddl/tag/
├── tb_tag.sql
└── tb_board_post_tag.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/tags` | 태그 목록 전체 | 불필요 |
| GET | `/api/v1/tags/search` | 태그 검색 (`?keyword=`) | 불필요 |
| POST | `/api/v1/boards/{boardPostUuid}/tags` | 게시글에 태그 추가 | 필요 (본인) |
| DELETE | `/api/v1/boards/{boardPostUuid}/tags/{tagUuid}` | 태그 제거 | 필요 (본인) |

## Mapper 메서드

```java
List<TagDto> selectAllTags();
List<TagDto> selectTagsByKeyword(@Param("keyword") String keyword);
List<TagDto> selectTagsByPostIdx(@Param("boardPostIdx") Long boardPostIdx);
TagDto selectTagByName(@Param("name") String name);
void insertTag(TagDto tag);
void insertBoardPostTag(@Param("boardPostIdx") Long boardPostIdx, @Param("tagIdx") Long tagIdx);
int deleteBoardPostTag(@Param("boardPostIdx") Long boardPostIdx, @Param("tagIdx") Long tagIdx);
```

## TagResponse

```
tagUuid, name
```

## 서비스 규칙

- 태그 추가 시 없으면 자동 생성 (upsert 방식)
- 태그명은 소문자 정규화 처리
- 게시글 상세 응답에 태그 목록 포함 (board 서비스에서 호출)

## 완료 체크

- [ ] DDL 생성
- [ ] TagMapper (interface + XML)
- [ ] DTO 생성
- [ ] TagService
- [ ] TagController
