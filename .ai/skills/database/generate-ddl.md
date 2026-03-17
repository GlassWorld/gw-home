# Skill: generate-ddl

## 입력

```
TABLE: {테이블명}
DOMAIN: {도메인명}
COLUMNS: {컬럼 목록과 타입}
```

## 출력

- `infra-db/src/main/resources/sql/ddl/{domain}/{table}.sql`

## 규칙

- [ ] PK: `{abbr_table}_idx BIGSERIAL PRIMARY KEY`
- [ ] UUID: `{abbr_table}_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()`
- [ ] Timestamp: `TIMESTAMPTZ NOT NULL DEFAULT now()`
- [ ] Audit: `created_by VARCHAR(100) NOT NULL`, `updated_by VARCHAR(100)`
- [ ] 소프트 삭제: `del_at TIMESTAMPTZ`
- [ ] 테이블명: `tb_{abbr_domain}_{abbr_entity}` (축약 snake_case)
- [ ] 컬럼명: 축약 snake_case

## 예시

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
```
