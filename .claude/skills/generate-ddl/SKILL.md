---
name: generate-ddl
description: Generate PostgreSQL DDL script with standard PK, UUID, audit columns, soft delete, and index.
tags:
  - database
  - ddl
  - postgresql
  - schema
---

# generate-ddl

## Use When

- A new table needs to be created
- Schema must be defined before implementing Mapper/Service
- DDL file must be placed in the infra-db module

## Read First

- `docs/backend/database.md`
- `docs/backend/domain.md`

## Input

```
TABLE: {테이블명}
DOMAIN: {도메인명}
COLUMNS: {컬럼 목록과 타입}
```

## Output

Provide implementation:

- `{project}-infra-db/src/main/resources/sql/ddl/{domain}/{table}.sql`

## Must Follow

- PK: `{abbr_table}_idx BIGSERIAL PRIMARY KEY`
- UUID: `{abbr_table}_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()`
- Timestamp: `TIMESTAMPTZ NOT NULL DEFAULT now()`
- Audit: `created_by VARCHAR(100) NOT NULL`, `updated_by VARCHAR(100)`
- Soft delete: `del_at TIMESTAMPTZ`
- Table name: `tb_{abbr_domain}_{abbr_entity}` (abbreviated snake_case)
- Column names: abbreviated snake_case
- Create UUID index after table
- Add `COMMENT ON COLUMN` for all columns

## Never

- ❌ Use `TIMESTAMP` without timezone — always `TIMESTAMPTZ`
- ❌ Expose `_idx` as primary key in API layer
- ❌ Use non-abbreviated table or column names (except audit fields)

## Example

```sql
CREATE TABLE tb_brd_pst (
    brd_pst_idx   BIGSERIAL PRIMARY KEY,
    brd_pst_uuid  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    ttl            VARCHAR(300) NOT NULL,
    cntnt          TEXT         NOT NULL,

    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at         TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_uuid ON tb_brd_pst (brd_pst_uuid);

COMMENT ON COLUMN tb_brd_pst.brd_pst_idx  IS '게시글 PK';
COMMENT ON COLUMN tb_brd_pst.brd_pst_uuid IS '게시글 UUID (외부 노출)';
COMMENT ON COLUMN tb_brd_pst.ttl          IS '제목';
COMMENT ON COLUMN tb_brd_pst.del_at       IS '삭제 일시 (소프트 삭제)';
```
