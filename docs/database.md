# Database

## DDL 규칙

### PK / UUID
```sql
{table}_idx    BIGSERIAL PRIMARY KEY          -- 내부 PK (조인, 외래키 전용)
{table}_uuid   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()  -- 외부 노출 전용
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
deleted_at  TIMESTAMPTZ    -- NULL이면 정상, 값 있으면 삭제
```

## 파일 관리

- 파일 업로드는 `file` 도메인이 전담
- 업로드 후 반환된 URL을 각 도메인에서 저장
- 게시글 본문 이미지: `board` 테이블에 URL 문자열로 저장
- 파일 메타데이터(원본명, 크기, MIME)는 `tb_file` 테이블에만 존재

## 테이블 네이밍

```
tb_{도메인}_{대상}
예: tb_board_post, tb_board_comment, tb_member_account
```

## 예시 DDL

```sql
CREATE TABLE tb_board_post (
    board_post_idx   BIGSERIAL PRIMARY KEY,
    board_post_uuid  UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title            VARCHAR(300) NOT NULL,
    content          TEXT NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);
```
