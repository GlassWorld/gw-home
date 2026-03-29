CREATE TABLE tb_wrk_wkl_rpt (
    wrk_wkl_rpt_idx BIGSERIAL PRIMARY KEY,
    wrk_wkl_rpt_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL,
    wk_strt_dt DATE NOT NULL,
    wk_end_dt DATE NOT NULL,
    ttl VARCHAR(200) NOT NULL,
    cntn TEXT NOT NULL,
    opn_yn CHAR(1) NOT NULL DEFAULT 'N',
    pbls_at TIMESTAMPTZ,
    gen_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT chk_wrk_wkl_rpt_opn_yn CHECK (opn_yn IN ('Y', 'N')),
    CONSTRAINT chk_wrk_wkl_rpt_gen_type CHECK (gen_type IN ('MANUAL', 'OPENAI', 'RULE_BASED')),
    CONSTRAINT fk_wrk_wkl_rpt_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE INDEX idx_wrk_wkl_rpt_owner_week ON tb_wrk_wkl_rpt (mbr_acct_idx, wk_strt_dt DESC, wk_end_dt DESC);
CREATE INDEX idx_wrk_wkl_rpt_open ON tb_wrk_wkl_rpt (opn_yn, pbls_at DESC);
