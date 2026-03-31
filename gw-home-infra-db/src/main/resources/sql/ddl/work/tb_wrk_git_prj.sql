CREATE TABLE IF NOT EXISTS tb_wrk_git_prj (
    wrk_git_prj_idx BIGSERIAL PRIMARY KEY,
    wrk_git_prj_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    wrk_git_acct_idx BIGINT NOT NULL,
    mbr_acct_idx BIGINT NOT NULL,
    prj_nm VARCHAR(200) NOT NULL,
    repo_url VARCHAR(500) NOT NULL,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT chk_wrk_git_prj_use_yn CHECK (use_yn IN ('Y', 'N')),
    CONSTRAINT fk_wrk_git_prj_acct FOREIGN KEY (wrk_git_acct_idx) REFERENCES tb_wrk_git_acct (wrk_git_acct_idx),
    CONSTRAINT fk_wrk_git_prj_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE INDEX IF NOT EXISTS idx_wrk_git_prj_owner
    ON tb_wrk_git_prj (mbr_acct_idx, use_yn, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_wrk_git_prj_acct
    ON tb_wrk_git_prj (wrk_git_acct_idx, use_yn, updated_at DESC);
