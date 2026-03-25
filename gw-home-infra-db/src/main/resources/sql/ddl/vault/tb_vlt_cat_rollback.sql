DROP INDEX IF EXISTS idx_vlt_crd_vlt_cat_idx;
ALTER TABLE tb_vlt_crd DROP COLUMN IF EXISTS vlt_cat_idx;
DROP TABLE IF EXISTS tb_vlt_cat;
