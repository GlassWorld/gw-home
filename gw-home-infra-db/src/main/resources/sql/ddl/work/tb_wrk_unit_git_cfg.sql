CREATE TABLE IF NOT EXISTS tb_wrk_unit_git_cfg (
    wrk_unit_git_cfg_idx BIGSERIAL PRIMARY KEY,
    wrk_unit_git_cfg_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    work_unit_idx BIGINT NOT NULL,
    mbr_acct_idx BIGINT NOT NULL,
    prvd_cd VARCHAR(20) NOT NULL,
    repo_url VARCHAR(500) NOT NULL,
    auth_nm VARCHAR(200) NOT NULL,
    acs_tokn_enc TEXT,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT chk_wrk_unit_git_cfg_prvd_cd CHECK (prvd_cd IN ('GITHUB', 'GITLAB')),
    CONSTRAINT fk_wrk_unit_git_cfg_work_unit FOREIGN KEY (work_unit_idx) REFERENCES tb_work_unit (work_unit_idx),
    CONSTRAINT fk_wrk_unit_git_cfg_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE INDEX IF NOT EXISTS idx_wrk_unit_git_cfg_work_unit
    ON tb_wrk_unit_git_cfg (work_unit_idx, del_at);

CREATE INDEX IF NOT EXISTS idx_wrk_unit_git_cfg_mbr_acct
    ON tb_wrk_unit_git_cfg (mbr_acct_idx, del_at);
