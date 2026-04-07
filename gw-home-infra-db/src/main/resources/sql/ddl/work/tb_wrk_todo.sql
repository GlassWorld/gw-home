CREATE TABLE tb_wrk_todo (
    wrk_todo_idx       BIGSERIAL    PRIMARY KEY,
    wrk_todo_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    work_unit_idx      BIGINT       NOT NULL,
    mbr_acct_idx       BIGINT       NOT NULL,
    prnt_wrk_todo_idx  BIGINT,
    ttl                VARCHAR(200) NOT NULL,
    dscr               TEXT,
    sts                VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    prgs_rt            INTEGER      NOT NULL DEFAULT 0,
    strt_dt            DATE,
    due_dt             DATE,
    sort_ord           INTEGER      NOT NULL DEFAULT 1,
    created_by         VARCHAR(100) NOT NULL,
    updated_by         VARCHAR(100),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at             TIMESTAMPTZ,
    CONSTRAINT chk_wrk_todo_sts CHECK (sts IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_wrk_todo_prgs_rt CHECK (prgs_rt >= 0 AND prgs_rt <= 100)
);

CREATE INDEX idx_wrk_todo_work_unit ON tb_wrk_todo (work_unit_idx, del_at);
CREATE INDEX idx_wrk_todo_owner ON tb_wrk_todo (mbr_acct_idx, del_at);
CREATE INDEX idx_wrk_todo_parent_sort ON tb_wrk_todo (work_unit_idx, prnt_wrk_todo_idx, sort_ord, del_at);

COMMENT ON TABLE tb_wrk_todo IS '업무 TODO 트리';
COMMENT ON COLUMN tb_wrk_todo.wrk_todo_idx IS '업무 TODO PK';
COMMENT ON COLUMN tb_wrk_todo.wrk_todo_uuid IS '업무 TODO UUID';
COMMENT ON COLUMN tb_wrk_todo.work_unit_idx IS '업무 PK';
COMMENT ON COLUMN tb_wrk_todo.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_wrk_todo.prnt_wrk_todo_idx IS '부모 업무 TODO PK';
COMMENT ON COLUMN tb_wrk_todo.ttl IS 'TODO 제목';
COMMENT ON COLUMN tb_wrk_todo.dscr IS 'TODO 설명';
COMMENT ON COLUMN tb_wrk_todo.sts IS 'TODO 상태';
COMMENT ON COLUMN tb_wrk_todo.prgs_rt IS '진행률';
COMMENT ON COLUMN tb_wrk_todo.strt_dt IS '시작일';
COMMENT ON COLUMN tb_wrk_todo.due_dt IS '마감일';
COMMENT ON COLUMN tb_wrk_todo.sort_ord IS '정렬 순서';
COMMENT ON COLUMN tb_wrk_todo.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_todo.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_wrk_todo.created_at IS '생성 일시';
COMMENT ON COLUMN tb_wrk_todo.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_wrk_todo.del_at IS '삭제 일시';
