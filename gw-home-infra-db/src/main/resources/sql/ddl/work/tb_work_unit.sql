CREATE TABLE tb_work_unit (
    work_unit_idx BIGSERIAL PRIMARY KEY,
    work_unit_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL,
    ttl VARCHAR(200) NOT NULL,
    dscr TEXT,
    ctgr VARCHAR(100),
    sts VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    use_cnt INTEGER NOT NULL DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT chk_work_unit_sts CHECK (sts IN ('IN_PROGRESS', 'DONE', 'ON_HOLD')),
    CONSTRAINT chk_work_unit_use_yn CHECK (use_yn IN ('Y', 'N')),
    CONSTRAINT fk_work_unit_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE INDEX idx_work_unit_mbr_acct ON tb_work_unit (mbr_acct_idx);
CREATE INDEX idx_work_unit_listing ON tb_work_unit (mbr_acct_idx, use_yn, updated_at DESC, created_at DESC);
CREATE INDEX idx_work_unit_last_used_at ON tb_work_unit (mbr_acct_idx, last_used_at DESC);
