CREATE TABLE tb_brd_cmt (
    brd_cmt_idx       BIGSERIAL    PRIMARY KEY,
    brd_cmt_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_pst_idx       BIGINT       NOT NULL,
    prnt_brd_cmt_idx  BIGINT,
    mbr_acct_idx      BIGINT       NOT NULL,
    cntnt             VARCHAR(2000) NOT NULL,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at            TIMESTAMPTZ
);

CREATE INDEX idx_brd_cmt_brd_pst ON tb_brd_cmt (brd_pst_idx);
CREATE INDEX idx_brd_cmt_prnt ON tb_brd_cmt (prnt_brd_cmt_idx);
