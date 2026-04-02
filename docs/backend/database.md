# 데이터베이스 규칙

## 이 문서의 목적

이 문서는 DDL 작성과 테이블 설계 시 지켜야 할 공통 기준을 설명한다.

## 기본 식별자 규칙

```sql
{table}_idx    BIGSERIAL PRIMARY KEY
{table}_uuid   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()
```

- 내부 조인과 참조에는 `_idx`를 사용한다
- 외부 노출과 API 식별에는 `_uuid`를 사용한다

## 시간 컬럼 규칙

```sql
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
```

- `TIMESTAMP`는 사용하지 않는다
- `updated_at`은 애플리케이션 또는 트리거로 갱신한다

## 감사 컬럼 규칙

```sql
created_by  VARCHAR(100)
updated_by  VARCHAR(100)
```

- `created_by`, `updated_by`에는 내부 숫자 키가 아니라 로그인 ID를 저장한다

## 소프트 삭제 규칙

```sql
del_at  TIMESTAMPTZ
```

- `NULL`이면 정상 데이터
- 값이 있으면 삭제된 데이터로 본다

## 테이블 네이밍

```text
tb_{축약도메인}_{축약대상}
예: tb_brd_pst, tb_brd_cmt, tb_mbr_acct
```

## 파일 도메인 원칙

- 파일 업로드는 `file` 도메인이 전담한다
- 다른 도메인은 업로드 결과 URL만 저장한다
- 게시글 본문 이미지도 URL 문자열로 저장한다
- 파일 메타데이터는 `tb_file`에만 저장한다

## 예시 DDL

```sql
CREATE TABLE tb_brd_pst (
    brd_pst_idx   BIGSERIAL PRIMARY KEY,
    brd_pst_uuid  UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    ttl           VARCHAR(300) NOT NULL,
    cntnt         TEXT NOT NULL,
    img_url       TEXT,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at        TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_uuid ON tb_brd_pst (brd_pst_uuid);
```

## DDL 파일 위치

```text
{project}-infra-db/src/main/resources/sql/ddl/{domain}/{table}.sql
```

- 기준은 도메인별 개별 DDL 파일이다
- 문서용 집계 스크립트는 [all-ddl.sql](/home/glassworld/workspace/gw-home/docs/all-ddl.sql) 이다
- 개별 DDL을 바꾸면 집계 스크립트도 함께 최신화한다
