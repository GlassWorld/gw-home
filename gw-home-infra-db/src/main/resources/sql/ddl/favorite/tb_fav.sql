CREATE TABLE tb_fav (
    fav_idx       BIGSERIAL    PRIMARY KEY,
    fav_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    trgt_type     VARCHAR(20)  NOT NULL,
    trgt_idx      BIGINT       NOT NULL,
    mbr_acct_idx  BIGINT       NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (trgt_type, trgt_idx, mbr_acct_idx)
);

CREATE INDEX idx_fav_trgt ON tb_fav (trgt_type, trgt_idx);
CREATE INDEX idx_fav_mbr_acct ON tb_fav (mbr_acct_idx);

COMMENT ON TABLE tb_fav IS '좋아요';
COMMENT ON COLUMN tb_fav.fav_idx IS '좋아요 PK';
COMMENT ON COLUMN tb_fav.fav_uuid IS '좋아요 UUID';
COMMENT ON COLUMN tb_fav.trgt_type IS '대상 타입';
COMMENT ON COLUMN tb_fav.trgt_idx IS '대상 PK';
COMMENT ON COLUMN tb_fav.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_fav.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_fav.created_at IS '생성 일시';
