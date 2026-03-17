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

- [ ] PK: `{table}_idx BIGSERIAL PRIMARY KEY`
- [ ] UUID: `{table}_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()`
- [ ] Timestamp: `TIMESTAMPTZ NOT NULL DEFAULT now()`
- [ ] Audit: `created_by VARCHAR(100) NOT NULL`, `updated_by VARCHAR(100)`
- [ ] 소프트 삭제: `deleted_at TIMESTAMPTZ`
- [ ] 테이블명: `tb_{domain}_{entity}` (snake_case)
- [ ] 컬럼명: snake_case

## 예시

```sql
CREATE TABLE tb_board_post (
    board_post_idx   BIGSERIAL PRIMARY KEY,
    board_post_uuid  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    title            VARCHAR(300) NOT NULL,
    content          TEXT         NOT NULL,

    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_board_post_uuid ON tb_board_post (board_post_uuid);
```
