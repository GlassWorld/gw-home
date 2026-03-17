# Skill: create-mapper

## 입력

```
DOMAIN: {도메인명}
TABLE: {테이블명}
METHODS: {메서드 목록}
```

## 출력

- `{project}-infra-db/src/main/java/com/gw/infra/db/mapper/{domain}/{Domain}Mapper.java`
- `{project}-infra-db/src/main/resources/mapper/{domain}/{Domain}Mapper.xml`

## 규칙

- [ ] 인터페이스에 `@Mapper` 어노테이션
- [ ] 메서드명 접두사: `select` / `insert` / `update` / `delete`
- [ ] XML id = 메서드명과 정확히 일치
- [ ] 동적 쿼리는 `<if>`, `<choose>`, `<foreach>` 사용
- [ ] 기본 조회는 `resultType="{Domain}Vo"` 사용
- [ ] 조인 조회는 `resultType="{Domain}Jvo"` 사용
- [ ] `resultType`에는 패키지명을 적지 않음
- [ ] `VO` / `JVO`는 `share` 모듈에 생성
- [ ] `mapUnderscoreToCamelCase=true` 기준으로 컬럼 alias를 최소화
- [ ] DDL, `VO`, `JVO` 클래스명과 필드명은 모두 축약형 기준으로 맞춤
- [ ] 공통 PK/UUID/감사 컬럼은 `BaseVo`로 올리고 대표 PK/UUID는 `AS idx`, `AS uuid`로 alias 처리
- [ ] `VO`는 단일 테이블 컬럼을 그대로 유지
- [ ] 조인으로 늘어난 필드나 의미가 바뀐 필드는 `JVO`로 분리
- [ ] `VO` / `JVO`는 Lombok `@SuperBuilder`와 필드 주석을 사용

## 예시

### Mapper Interface

```java
@Mapper
public interface BoardMapper {
    BrdVo selectBoardByUuid(@Param("uuid") String uuid);
    List<BrdVo> selectBoardList(@Param("req") BrdListReq req);
    int insertBoard(@Param("board") BrdVo board);
    int updateBoard(@Param("board") BrdVo board);
    int deleteBoard(@Param("uuid") String uuid);
}
```

### Mapper XML

```xml
<mapper namespace="com.gw.infra.db.mapper.board.BoardMapper">
    <select id="selectBoardByUuid" resultType="BrdVo">
        SELECT brd_idx AS idx, brd_uuid AS uuid, ttl, cntnt, created_by, created_at
        FROM tb_brd
        WHERE brd_uuid = #{uuid}
          AND del_at IS NULL
    </select>

    <select id="selectBoardList" resultType="BrdVo">
        SELECT brd_uuid AS uuid, ttl, created_by, created_at
        FROM tb_brd
        WHERE del_at IS NULL
        <if test="req.kwd != null and req.kwd != ''">
          AND ttl LIKE CONCAT('%', #{req.kwd}, '%')
        </if>
        ORDER BY created_at DESC
        LIMIT #{req.size} OFFSET #{req.offset}
    </select>

</mapper>
```
