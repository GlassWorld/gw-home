CREATE TABLE IF NOT EXISTS tb_wrk_git_acct (
    wrk_git_acct_idx BIGSERIAL PRIMARY KEY,
    wrk_git_acct_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL,
    prvd_cd VARCHAR(20) NOT NULL,
    acct_lbl VARCHAR(100) NOT NULL,
    auth_nm VARCHAR(200) NOT NULL,
    acs_tokn_enc TEXT,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT chk_wrk_git_acct_prvd_cd CHECK (prvd_cd IN ('GITLAB')),
    CONSTRAINT chk_wrk_git_acct_use_yn CHECK (use_yn IN ('Y', 'N')),
    CONSTRAINT fk_wrk_git_acct_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE INDEX IF NOT EXISTS idx_wrk_git_acct_owner
    ON tb_wrk_git_acct (mbr_acct_idx, use_yn, updated_at DESC);
