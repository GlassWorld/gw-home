CREATE TABLE IF NOT EXISTS tb_wrk_dly_rpt (
    wrk_dly_rpt_idx BIGSERIAL PRIMARY KEY,
    wrk_dly_rpt_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL,
    rpt_dt DATE NOT NULL,
    spcl_note TEXT,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    CONSTRAINT uq_wrk_dly_rpt_owner_date UNIQUE (mbr_acct_idx, rpt_dt),
    CONSTRAINT fk_wrk_dly_rpt_mbr_acct FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_wrk_dly_rpt_idx_owner ON tb_wrk_dly_rpt (wrk_dly_rpt_idx, mbr_acct_idx);
CREATE UNIQUE INDEX IF NOT EXISTS ux_work_unit_idx_owner ON tb_work_unit (work_unit_idx, mbr_acct_idx);

CREATE TABLE IF NOT EXISTS tb_wrk_dly_rpt_wrk_unit (
    wrk_dly_rpt_wrk_unit_idx BIGSERIAL PRIMARY KEY,
    wrk_dly_rpt_idx BIGINT NOT NULL,
    mbr_acct_idx BIGINT NOT NULL,
    work_unit_idx BIGINT NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wrk_dly_rpt_wrk_unit UNIQUE (wrk_dly_rpt_idx, work_unit_idx),
    CONSTRAINT fk_wrk_dly_rpt_wrk_unit_owner FOREIGN KEY (mbr_acct_idx) REFERENCES tb_mbr_acct (mbr_acct_idx),
    CONSTRAINT fk_wrk_dly_rpt_wrk_unit_rpt FOREIGN KEY (wrk_dly_rpt_idx, mbr_acct_idx) REFERENCES tb_wrk_dly_rpt (wrk_dly_rpt_idx, mbr_acct_idx) ON DELETE CASCADE,
    CONSTRAINT fk_wrk_dly_rpt_wrk_unit_unit FOREIGN KEY (work_unit_idx, mbr_acct_idx) REFERENCES tb_work_unit (work_unit_idx, mbr_acct_idx)
);

ALTER TABLE tb_wrk_dly_rpt_wrk_unit
    ADD COLUMN IF NOT EXISTS mbr_acct_idx BIGINT;

UPDATE tb_wrk_dly_rpt_wrk_unit report_work_unit
SET mbr_acct_idx = daily_report.mbr_acct_idx
FROM tb_wrk_dly_rpt daily_report
WHERE report_work_unit.mbr_acct_idx IS NULL
  AND report_work_unit.wrk_dly_rpt_idx = daily_report.wrk_dly_rpt_idx;

ALTER TABLE tb_wrk_dly_rpt_wrk_unit
    ALTER COLUMN mbr_acct_idx SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_wrk_dly_rpt_owner_date ON tb_wrk_dly_rpt (mbr_acct_idx, rpt_dt DESC);
CREATE INDEX IF NOT EXISTS idx_wrk_dly_rpt_date ON tb_wrk_dly_rpt (rpt_dt DESC);
CREATE INDEX IF NOT EXISTS idx_wrk_dly_rpt_wrk_unit_rpt ON tb_wrk_dly_rpt_wrk_unit (wrk_dly_rpt_idx);
CREATE INDEX IF NOT EXISTS idx_wrk_dly_rpt_wrk_unit_owner ON tb_wrk_dly_rpt_wrk_unit (mbr_acct_idx);
CREATE INDEX IF NOT EXISTS idx_wrk_dly_rpt_wrk_unit_unit ON tb_wrk_dly_rpt_wrk_unit (work_unit_idx);
CREATE UNIQUE INDEX IF NOT EXISTS ux_wrk_dly_rpt_wrk_unit_report_work_unit ON tb_wrk_dly_rpt_wrk_unit (wrk_dly_rpt_idx, work_unit_idx);
