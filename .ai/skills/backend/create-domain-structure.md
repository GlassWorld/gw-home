# Skill: create-domain-structure

## 입력

```
DOMAIN: {도메인명}
```

## 출력 (생성 파일 목록)

```
api/src/main/java/com/gw/api/{domain}/
├── controller/   {Domain}Controller.java
├── service/      {Domain}Service.java
├── mapper/       {Domain}Mapper.java
└── dto/
    ├── Create{Domain}Request.java
    └── {Domain}Response.java

infra-db/src/main/resources/mapper/{domain}/
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
<mapper namespace="com.gw.api.{domain}.mapper.{Domain}Mapper">

    <resultMap id="{domain}ResultMap" type="com.gw.api.{domain}.dto.{Domain}Response">
        <id property="{domain}Uuid" column="{domain}_uuid"/>
        <!-- 필드 매핑 -->
    </resultMap>

</mapper>
```
