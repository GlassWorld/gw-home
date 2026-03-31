CREATE TABLE tb_mbr_prfl (
    mbr_prfl_idx    BIGSERIAL    PRIMARY KEY,
    mbr_prfl_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx    BIGINT       NOT NULL UNIQUE,
    nick_nm         VARCHAR(50)  NOT NULL UNIQUE,
    intro           VARCHAR(500),
    prfl_img_url    VARCHAR(1000),
    fav_menu_json   TEXT,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_mbr_prfl_mbr_acct ON tb_mbr_prfl (mbr_acct_idx);
