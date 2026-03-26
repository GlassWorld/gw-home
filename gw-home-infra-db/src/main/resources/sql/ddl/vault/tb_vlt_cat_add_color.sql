ALTER TABLE tb_vlt_cat
    ADD COLUMN color VARCHAR(7);

COMMENT ON COLUMN tb_vlt_cat.color IS '카테고리 색상 HEX';
