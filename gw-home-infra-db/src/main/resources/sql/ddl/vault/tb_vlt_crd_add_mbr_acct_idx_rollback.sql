DROP INDEX IF EXISTS idx_vlt_crd_mbr_acct_del_at;

ALTER TABLE tb_vlt_crd
    DROP COLUMN IF EXISTS mbr_acct_idx;
