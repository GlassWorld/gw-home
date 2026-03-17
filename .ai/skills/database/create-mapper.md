# Skill: create-mapper

## 입력

```
DOMAIN: {도메인명}
TABLE: {테이블명}
METHODS: {메서드 목록}
```

## 출력

- `api/.../mapper/{Domain}Mapper.java`
- `infra-db/.../mapper/{domain}/{Domain}Mapper.xml`

## 규칙

- [ ] 인터페이스에 `@Mapper` 어노테이션
- [ ] 메서드명 접두사: `select` / `insert` / `update` / `delete`
- [ ] XML id = 메서드명과 정확히 일치
- [ ] 동적 쿼리는 `<if>`, `<choose>`, `<foreach>` 사용
- [ ] `resultMap` 사용 권장 (직접 필드 매핑)

## 예시

### Mapper Interface

```java
@Mapper
public interface BoardMapper {
    BoardDto selectBoardByUuid(@Param("boardUuid") String boardUuid);
    List<BoardDto> selectBoardList(@Param("req") BoardListRequest req);
    int insertBoard(@Param("board") BoardDto board);
    int updateBoard(@Param("board") BoardDto board);
    int deleteBoard(@Param("boardUuid") String boardUuid);
}
```

### Mapper XML

```xml
<mapper namespace="com.gw.api.board.mapper.BoardMapper">

    <resultMap id="boardResultMap" type="com.gw.api.board.dto.BoardDto">
        <id property="boardUuid" column="board_uuid"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="createdBy" column="created_by"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <select id="selectBoardByUuid" resultMap="boardResultMap">
        SELECT board_uuid, title, content, created_by, created_at
        FROM tb_board_post
        WHERE board_uuid = #{boardUuid}
          AND deleted_at IS NULL
    </select>

    <select id="selectBoardList" resultMap="boardResultMap">
        SELECT board_uuid, title, created_by, created_at
        FROM tb_board_post
        WHERE deleted_at IS NULL
        <if test="req.keyword != null and req.keyword != ''">
          AND title LIKE CONCAT('%', #{req.keyword}, '%')
        </if>
        ORDER BY created_at DESC
        LIMIT #{req.size} OFFSET #{req.offset}
    </select>

</mapper>
```
