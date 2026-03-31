CREATE TABLE IF NOT EXISTS tb_wrk_unit_git_prj (
    wrk_unit_git_prj_idx BIGSERIAL PRIMARY KEY,
    work_unit_idx BIGINT NOT NULL,
    wrk_git_prj_idx BIGINT NOT NULL,
    mbr_acct_idx BIGINT NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT uq_wrk_unit_git_prj UNIQUE (work_unit_idx, wrk_git_prj_idx),
    CONSTRAINT fk_wrk_unit_git_prj_work_unit FOREIGN KEY (work_unit_idx) REFERENCES tb_work_unit (work_unit_idx),
    CONSTRAINT fk_wrk_unit_git_prj_git_prj FOREIGN KEY (wrk_git_prj_idx) REFERENCES tb_wrk_git_prj (wrk_git_prj_idx),
    CONSTRAINT fk_wrk_unit_git_prj_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE INDEX IF NOT EXISTS idx_wrk_unit_git_prj_work_unit
    ON tb_wrk_unit_git_prj (work_unit_idx, del_at);
