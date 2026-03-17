# 10. Domain: tag

## 목표
태그 생성/조회 및 게시글-태그 매핑 관리

## DDL

```sql
-- sql/ddl/tag/tb_tag.sql
CREATE TABLE tb_tag (
    tag_idx      BIGSERIAL    PRIMARY KEY,
    tag_uuid     UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm           VARCHAR(50)  NOT NULL UNIQUE,
    created_by   VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- sql/ddl/tag/tb_brd_pst_tag.sql
CREATE TABLE tb_brd_pst_tag (
    brd_pst_tag_idx  BIGSERIAL   PRIMARY KEY,
    brd_pst_idx      BIGINT      NOT NULL,
    tag_idx          BIGINT      NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (brd_pst_idx, tag_idx)
);
CREATE INDEX idx_brd_pst_tag_brd_pst ON tb_brd_pst_tag (brd_pst_idx);
CREATE INDEX idx_brd_pst_tag_tag ON tb_brd_pst_tag (tag_idx);
```

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/
├── controller/tag/TagController.java
├── service/tag/TagService.java
└── dto/tag/
    ├── TagResponse.java
    └── AttachTagRequest.java

{project}-infra-db/src/main/java/com/gw/infra/db/mapper/tag/TagMapper.java
{project}-infra-db/src/main/resources/mapper/tag/TagMapper.xml
{project}-infra-db/src/main/resources/sql/ddl/tag/
├── tb_tag.sql
└── tb_brd_pst_tag.sql
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
List<TagVo> selectAllTags();
List<TagVo> selectTagsByKeyword(@Param("kwd") String kwd);
List<TagVo> selectTagsByBrdPstIdx(@Param("brdPstIdx") Long brdPstIdx);
TagVo selectTagByNm(@Param("nm") String nm);
void insertTag(TagVo tag);
void insertBrdPstTag(@Param("brdPstIdx") Long brdPstIdx, @Param("tagIdx") Long tagIdx);
int deleteBrdPstTag(@Param("brdPstIdx") Long brdPstIdx, @Param("tagIdx") Long tagIdx);
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

- [x] DDL 생성
- [x] TagMapper (interface + XML)
- [x] DTO 생성
- [x] TagService
- [x] TagController
