CREATE TABLE tb_vlt_crd_cat (
    vlt_crd_cat_idx BIGSERIAL PRIMARY KEY,
    tb_vlt_crd_idx  BIGINT NOT NULL REFERENCES tb_vlt_crd(tb_vlt_crd_idx) ON DELETE CASCADE,
    tb_vlt_cat_idx  BIGINT NOT NULL REFERENCES tb_vlt_cat(tb_vlt_cat_idx) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tb_vlt_crd_idx, tb_vlt_cat_idx)
);

CREATE INDEX idx_vlt_crd_cat_crd_idx ON tb_vlt_crd_cat (tb_vlt_crd_idx);
CREATE INDEX idx_vlt_crd_cat_cat_idx ON tb_vlt_crd_cat (tb_vlt_cat_idx);

COMMENT ON TABLE tb_vlt_crd_cat IS '개인 자격증명-카테고리 매핑';
COMMENT ON COLUMN tb_vlt_crd_cat.vlt_crd_cat_idx IS '자격증명-카테고리 매핑 PK';
COMMENT ON COLUMN tb_vlt_crd_cat.tb_vlt_crd_idx IS '개인 자격증명 PK';
COMMENT ON COLUMN tb_vlt_crd_cat.tb_vlt_cat_idx IS 'Vault 카테고리 PK';
COMMENT ON COLUMN tb_vlt_crd_cat.created_at IS '생성 일시';
