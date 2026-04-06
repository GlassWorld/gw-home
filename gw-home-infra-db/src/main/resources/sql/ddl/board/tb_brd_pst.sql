CREATE TABLE tb_brd_pst (
    brd_pst_idx    BIGSERIAL    PRIMARY KEY,
    brd_pst_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_ctgr_idx   BIGINT,
    mbr_acct_idx   BIGINT       NOT NULL,
    ttl            VARCHAR(300) NOT NULL,
    cntnt          TEXT         NOT NULL,
    view_cnt       INT          NOT NULL DEFAULT 0,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at         TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_ctgr ON tb_brd_pst (brd_ctgr_idx);
CREATE INDEX idx_brd_pst_mbr_acct ON tb_brd_pst (mbr_acct_idx);
CREATE INDEX idx_brd_pst_created_at ON tb_brd_pst (created_at DESC);
