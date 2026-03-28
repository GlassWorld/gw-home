INSERT INTO tb_vlt_crd_cat (
    tb_vlt_crd_idx,
    tb_vlt_cat_idx
)
SELECT
    tb_vlt_crd_idx,
    vlt_cat_idx
FROM tb_vlt_crd
WHERE vlt_cat_idx IS NOT NULL
ON CONFLICT (tb_vlt_crd_idx, tb_vlt_cat_idx) DO NOTHING;
