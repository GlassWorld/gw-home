# Skill: create-domain-structure

## 입력

```
DOMAIN: {도메인명}
```

## 출력 (생성 파일 목록)

```
{project}-api/src/main/java/com/gw/api/
├── controller/{domain}/   {Domain}Controller.java
├── service/{domain}/      {Domain}Service.java
└── dto/{domain}/
    ├── Create{Domain}Request.java
    └── {Domain}Response.java
{project}-share/src/main/java/com/gw/share/
├── vo/{domain}/           {Domain}Vo.java
└── jvo/{domain}/          {Domain}Jvo.java  # 필요 시 생성
{project}-infra-db/src/main/java/com/gw/infra/db/
└── mapper/{domain}/       {Domain}Mapper.java

{project}-infra-db/src/main/resources/mapper/{domain}/
└── {Domain}Mapper.xml
```

## 규칙

- [ ] 패키지명은 소문자 단수형
- [ ] Controller: `@RestController`, `@RequestMapping("/api/v1/{domain}s")`
- [ ] Mapper XML namespace = Mapper 인터페이스 fully qualified name
- [ ] DTO에 `_idx` 필드 포함 금지

## Mapper XML 기본 구조

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.gw.infra.db.mapper.{domain}.{Domain}Mapper">

    <select id="select{Domain}" resultType="{Domain}Vo">
        SELECT {domain}_uuid, created_at
        FROM tb_{domain}
    </select>

</mapper>
```
