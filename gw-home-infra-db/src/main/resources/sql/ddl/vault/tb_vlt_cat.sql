CREATE TABLE tb_vlt_cat (
    tb_vlt_cat_idx  BIGSERIAL    PRIMARY KEY,
    tb_vlt_cat_uuid UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm              VARCHAR(100) NOT NULL,
    dsc             TEXT,
    color           VARCHAR(7),
    sort_ord        INT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    del_at          TIMESTAMPTZ
);

CREATE INDEX idx_vlt_cat_del_at ON tb_vlt_cat (del_at);

COMMENT ON TABLE tb_vlt_cat IS 'Vault 카테고리';
COMMENT ON COLUMN tb_vlt_cat.tb_vlt_cat_idx IS 'Vault 카테고리 PK';
COMMENT ON COLUMN tb_vlt_cat.tb_vlt_cat_uuid IS 'Vault 카테고리 UUID';
COMMENT ON COLUMN tb_vlt_cat.nm IS '카테고리명';
COMMENT ON COLUMN tb_vlt_cat.dsc IS '카테고리 설명';
COMMENT ON COLUMN tb_vlt_cat.color IS '카테고리 색상 HEX';
COMMENT ON COLUMN tb_vlt_cat.sort_ord IS '정렬 순서';
COMMENT ON COLUMN tb_vlt_cat.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_vlt_cat.created_at IS '생성 일시';
COMMENT ON COLUMN tb_vlt_cat.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_vlt_cat.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_vlt_cat.del_at IS '삭제 일시';
