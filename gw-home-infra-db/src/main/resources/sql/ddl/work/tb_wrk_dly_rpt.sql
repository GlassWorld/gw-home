CREATE TABLE tb_wrk_dly_rpt (
    wrk_dly_rpt_idx BIGSERIAL PRIMARY KEY,
    wrk_dly_rpt_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL,
    rpt_dt DATE NOT NULL,
    cntn TEXT NOT NULL,
    sts VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    spcl_note TEXT,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT uq_wrk_dly_rpt_owner_date UNIQUE (mbr_acct_idx, rpt_dt),
    CONSTRAINT chk_wrk_dly_rpt_sts CHECK (sts IN ('PLANNED', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT fk_wrk_dly_rpt_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE INDEX idx_wrk_dly_rpt_owner_date ON tb_wrk_dly_rpt (mbr_acct_idx, rpt_dt DESC);
CREATE INDEX idx_wrk_dly_rpt_date ON tb_wrk_dly_rpt (rpt_dt DESC);
CREATE INDEX idx_wrk_dly_rpt_status_date ON tb_wrk_dly_rpt (sts, rpt_dt DESC);
