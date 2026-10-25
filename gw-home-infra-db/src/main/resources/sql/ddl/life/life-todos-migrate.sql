-- 기존 home 스키마에 일상 테이블만 추가한다. 전체 파일을 한 번에 실행한다.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL search_path TO home;

-- 사용자별 생활 카테고리를 업무와 독립적으로 저장한다.
CREATE TABLE IF NOT EXISTS tb_lf_todo_cat (
    lf_todo_cat_idx BIGSERIAL PRIMARY KEY,
    lf_todo_cat_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL REFERENCES tb_mbr_acct(mbr_acct_idx),
    nm VARCHAR(50) NOT NULL CHECK (length(btrim(nm)) > 0),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    UNIQUE (lf_todo_cat_idx, mbr_acct_idx)
);

-- 같은 사용자의 활성 카테고리는 대소문자 구분 없이 유일하다.
CREATE UNIQUE INDEX IF NOT EXISTS ux_lf_todo_cat_owner_name ON tb_lf_todo_cat (mbr_acct_idx, lower(nm)) WHERE del_at IS NULL;

-- 일상 할 일은 제목만 필수이며 업무·트리·메일과 관계를 갖지 않는다.
CREATE TABLE IF NOT EXISTS tb_lf_todo (
    lf_todo_idx BIGSERIAL PRIMARY KEY,
    lf_todo_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL REFERENCES tb_mbr_acct(mbr_acct_idx),
    lf_todo_cat_idx BIGINT,
    ttl VARCHAR(200) NOT NULL CHECK (length(btrim(ttl)) > 0),
    memo VARCHAR(4000),
    due_dt DATE,
    cmpl_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT fk_lf_todo_owned_category FOREIGN KEY (lf_todo_cat_idx, mbr_acct_idx)
        REFERENCES tb_lf_todo_cat(lf_todo_cat_idx, mbr_acct_idx)
);

-- 본인의 활성 할 일을 날짜·카테고리 조건으로 조회한다.
CREATE INDEX IF NOT EXISTS idx_lf_todo_owner_date ON tb_lf_todo (mbr_acct_idx, due_dt, lf_todo_idx) WHERE del_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_lf_todo_owner_category ON tb_lf_todo (mbr_acct_idx, lf_todo_cat_idx) WHERE del_at IS NULL;

-- 각 필드의 생활 도메인 의미와 완료 시각 정책을 기록한다.
COMMENT ON TABLE tb_lf_todo_cat IS '사용자별 생활 카테고리';
COMMENT ON COLUMN tb_lf_todo_cat.lf_todo_cat_idx IS '생활 카테고리 PK';
COMMENT ON COLUMN tb_lf_todo_cat.lf_todo_cat_uuid IS '생활 카테고리 외부 UUID';
COMMENT ON COLUMN tb_lf_todo_cat.mbr_acct_idx IS '소유 회원 계정 PK';
COMMENT ON COLUMN tb_lf_todo_cat.nm IS '카테고리 이름';
COMMENT ON COLUMN tb_lf_todo_cat.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_lf_todo_cat.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_lf_todo_cat.created_at IS '생성 일시';
COMMENT ON COLUMN tb_lf_todo_cat.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_lf_todo_cat.del_at IS '삭제 일시';
COMMENT ON TABLE tb_lf_todo IS '업무와 독립된 일상 할 일';
COMMENT ON COLUMN tb_lf_todo.lf_todo_idx IS '일상 할 일 PK';
COMMENT ON COLUMN tb_lf_todo.lf_todo_uuid IS '일상 할 일 외부 UUID';
COMMENT ON COLUMN tb_lf_todo.mbr_acct_idx IS '소유 회원 계정 PK';
COMMENT ON COLUMN tb_lf_todo.lf_todo_cat_idx IS '생활 카테고리 PK, 미분류는 NULL';
COMMENT ON COLUMN tb_lf_todo.ttl IS '일상 할 일 제목';
COMMENT ON COLUMN tb_lf_todo.memo IS '선택 메모';
COMMENT ON COLUMN tb_lf_todo.due_dt IS '선택 일정 날짜';
COMMENT ON COLUMN tb_lf_todo.cmpl_at IS '완료 시각, 미완료·완료 취소는 NULL';
COMMENT ON COLUMN tb_lf_todo.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_lf_todo.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_lf_todo.created_at IS '생성 일시';
COMMENT ON COLUMN tb_lf_todo.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_lf_todo.del_at IS '삭제 일시';

COMMIT;
