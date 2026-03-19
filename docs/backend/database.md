# Database

## DDL 규칙

### PK / UUID

```sql
{table}_idx    BIGSERIAL PRIMARY KEY                              -- 내부 PK (조인, 외래키 전용)
{table}_uuid   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()    -- 외부 노출 전용
```

- API 응답에 `_idx` 노출 금지
- 외부 식별자는 반드시 `_uuid` 사용

### Timestamp

```sql
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
```

- `TIMESTAMP` (without timezone) 사용 금지
- `updated_at`은 UPDATE 트리거 또는 애플리케이션에서 갱신

### Audit

```sql
created_by  VARCHAR(100)   -- 로그인 ID (이메일 또는 username)
updated_by  VARCHAR(100)
```

### 소프트 삭제

```sql
del_at  TIMESTAMPTZ    -- NULL이면 정상, 값 있으면 삭제
```

## 테이블 네이밍

```
tb_{축약도메인}_{축약대상}
예: tb_brd_pst, tb_brd_cmt, tb_mbr_acct
```

## 파일 관리

- 파일 업로드는 `file` 도메인이 전담
- 업로드 후 반환된 URL을 각 도메인에서 저장
- 게시글 본문 이미지: `board` 테이블에 URL 문자열로 저장
- 파일 메타데이터(원본명, 크기, MIME)는 `tb_file` 테이블에만 존재

## 예시 DDL

```sql
CREATE TABLE tb_brd_pst (
    brd_pst_idx   BIGSERIAL PRIMARY KEY,
    brd_pst_uuid  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    ttl            VARCHAR(300) NOT NULL,
    cntnt          TEXT         NOT NULL,
    img_url        TEXT,                        -- 본문 이미지 URL

    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at         TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_uuid ON tb_brd_pst (brd_pst_uuid);
```

## DDL 파일 위치

```
{project}-infra-db/src/main/resources/sql/ddl/{domain}/{table}.sql
```

전체 스키마 재생성: `sql/ddl/all-ddl.sql`
(`DROP TABLE IF EXISTS ... CASCADE` + `CREATE TABLE` + `COMMENT ON COLUMN` 포함, 재실행 가능)
