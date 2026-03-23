---
name: create-domain-structure
description: Create full backend domain package structure including Controller, Service, DTO, Mapper interface, Mapper XML, VO.
tags:
  - backend
  - domain
  - spring
  - mybatis
---

# create-domain-structure

## Use When

- A new backend domain needs to be created from scratch
- All layers (Controller, Service, Mapper, VO) must be scaffolded together
- Starting a new domain like account, board, comment, etc.

## Read First

- `docs/backend/backend-rules.md`
- `docs/common/architecture.md`
- `docs/backend/domain.md`

## Input

```
DOMAIN: {도메인명}
```

## Output

Provide full file list and implementation:

```
{project}-api/src/main/java/com/gw/api/
├── controller/{domain}/   {Domain}Controller.java
├── service/{domain}/      {Domain}Service.java
└── dto/{domain}/
    ├── Create{Domain}Request.java
    └── {Domain}Response.java

{project}-share/src/main/java/com/gw/share/
├── vo/{domain}/           {Domain}Vo.java
└── jvo/{domain}/          {Domain}Jvo.java  # if join query needed

{project}-infra-db/src/main/java/com/gw/infra/db/
└── mapper/{domain}/       {Domain}Mapper.java

{project}-infra-db/src/main/resources/mapper/{domain}/
└── {Domain}Mapper.xml
```

## Must Follow

- Package name: lowercase singular
- Controller: `@RestController`, `@RequestMapping("/api/v1/{domain}s")`
- Mapper XML namespace = Mapper interface fully qualified name
- VO/JVO class and field names follow abbreviation convention matching DDL
- Common PK/UUID/audit fields go in `BaseVo`
- Map primary PK/UUID columns as `AS idx`, `AS uuid` in XML

## Never

- ❌ Include `_idx` field in DTO
- ❌ Use JPA or `@Entity`
- ❌ Put business logic in Controller
- ❌ Use `resultMap` — use `resultType` by default
