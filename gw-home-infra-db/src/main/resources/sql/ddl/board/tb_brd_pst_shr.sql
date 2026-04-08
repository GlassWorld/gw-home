CREATE TABLE tb_brd_pst_shr (
    brd_pst_shr_idx   BIGSERIAL    PRIMARY KEY,
    brd_pst_shr_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_pst_idx       BIGINT       NOT NULL,
    shr_tkn           VARCHAR(255) NOT NULL UNIQUE,
    pwd_hash          VARCHAR(255),
    pwd_use_yn        VARCHAR(1)   NOT NULL DEFAULT 'N',
    actv_yn           VARCHAR(1)   NOT NULL DEFAULT 'Y',
    expr_at           TIMESTAMPTZ  NOT NULL,
    rvkd_at           TIMESTAMPTZ,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at            TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_shr_brd_pst ON tb_brd_pst_shr (brd_pst_idx);
CREATE INDEX idx_brd_pst_shr_expr_at ON tb_brd_pst_shr (expr_at);

COMMENT ON TABLE tb_brd_pst_shr IS '게시글 외부 공유';
COMMENT ON COLUMN tb_brd_pst_shr.brd_pst_shr_idx IS '게시글 외부 공유 PK';
COMMENT ON COLUMN tb_brd_pst_shr.brd_pst_shr_uuid IS '게시글 외부 공유 UUID';
COMMENT ON COLUMN tb_brd_pst_shr.brd_pst_idx IS '게시글 PK';
COMMENT ON COLUMN tb_brd_pst_shr.shr_tkn IS '공유 토큰';
COMMENT ON COLUMN tb_brd_pst_shr.pwd_hash IS '공유 비밀번호 해시';
COMMENT ON COLUMN tb_brd_pst_shr.pwd_use_yn IS '공유 비밀번호 사용 여부';
COMMENT ON COLUMN tb_brd_pst_shr.actv_yn IS '공유 활성 여부';
COMMENT ON COLUMN tb_brd_pst_shr.expr_at IS '공유 만료 일시';
COMMENT ON COLUMN tb_brd_pst_shr.rvkd_at IS '공유 해제 일시';
COMMENT ON COLUMN tb_brd_pst_shr.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_brd_pst_shr.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_brd_pst_shr.created_at IS '생성 일시';
COMMENT ON COLUMN tb_brd_pst_shr.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_brd_pst_shr.del_at IS '삭제 일시';
