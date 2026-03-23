---
name: create-mapper
description: Create MyBatis Mapper interface and XML with VO/JVO, resultType, dynamic queries, and BaseVo alias.
tags:
  - database
  - mybatis
  - mapper
  - backend
---

# create-mapper

## Use When

- A new Mapper interface and XML file are needed for a domain
- Mapper methods must be added to an existing Mapper
- VO or JVO class needs to be created alongside the Mapper

## Read First

- `docs/backend/backend-rules.md`
- `docs/backend/database.md`

## Input

```
DOMAIN: {도메인명}
TABLE: {테이블명}
METHODS: {메서드 목록}
```

## Output

Provide implementation:

- `{project}-infra-db/src/main/java/com/gw/infra/db/mapper/{domain}/{Domain}Mapper.java`
- `{project}-infra-db/src/main/resources/mapper/{domain}/{Domain}Mapper.xml`
- `{project}-share/src/main/java/com/gw/share/vo/{domain}/{Domain}Vo.java` (if not exists)

## Must Follow

- `@Mapper` annotation on interface
- Method prefix: `select` / `insert` / `update` / `delete`
- XML `id` = method name exactly
- Dynamic query: `<if>`, `<choose>`, `<foreach>`
- Single-table query: `resultType="{Domain}Vo"`, join query: `resultType="{Domain}Jvo"`
- Map PK/UUID columns as `AS idx`, `AS uuid` (BaseVo fields)
- VO/JVO: Lombok `@SuperBuilder` + field comments matching DB column comments
- All VO/JVO class and field names follow DDL abbreviation convention

## Never

- ❌ Include package name in `resultType`
- ❌ Use `resultMap` — use `resultType` by default
- ❌ Use JPA or `@Repository`
- ❌ Define VO/JVO in infra-db module — always in `share` module

## Example

```java
// Mapper Interface
@Mapper
public interface BoardMapper {
    BrdPstVo selectBoardByUuid(@Param("uuid") String uuid);
    List<BrdPstVo> selectBoardList(@Param("req") BrdPstListSrchVo req);
    int insertBoard(@Param("board") BrdPstVo board);
    int updateBoard(@Param("board") BrdPstVo board);
    int deleteBoard(@Param("uuid") String uuid);
}
```

```xml
<!-- Mapper XML -->
<mapper namespace="com.gw.infra.db.mapper.board.BoardMapper">

    <select id="selectBoardByUuid" resultType="BrdPstVo">
        SELECT brd_pst_idx AS idx,
               brd_pst_uuid AS uuid,
               ttl, cntnt, created_by, created_at
        FROM tb_brd_pst
        WHERE brd_pst_uuid = #{uuid}
          AND del_at IS NULL
    </select>

    <select id="selectBoardList" resultType="BrdPstVo">
        SELECT brd_pst_uuid AS uuid, ttl, created_by, created_at
        FROM tb_brd_pst
        WHERE del_at IS NULL
        <if test="req.kwd != null and req.kwd != ''">
          AND ttl LIKE CONCAT('%', #{req.kwd}, '%')
        </if>
        ORDER BY created_at DESC
        LIMIT #{req.size} OFFSET #{req.offset}
    </select>

</mapper>
```
