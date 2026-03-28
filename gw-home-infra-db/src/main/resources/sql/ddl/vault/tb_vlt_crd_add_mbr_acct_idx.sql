ALTER TABLE tb_vlt_crd
    ADD COLUMN IF NOT EXISTS mbr_acct_idx BIGINT;

UPDATE tb_vlt_crd credential
SET mbr_acct_idx = account.mbr_acct_idx
FROM tb_mbr_acct account
WHERE credential.mbr_acct_idx IS NULL
  AND credential.created_by = account.lgn_id;

ALTER TABLE tb_vlt_crd
    ALTER COLUMN mbr_acct_idx SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vlt_crd_mbr_acct_del_at ON tb_vlt_crd (mbr_acct_idx, del_at);

COMMENT ON COLUMN tb_vlt_crd.mbr_acct_idx IS '회원 계정 PK';
