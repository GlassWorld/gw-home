CREATE TABLE tb_vlt_crd (
    tb_vlt_crd_idx  BIGSERIAL    PRIMARY KEY,
    tb_vlt_crd_uuid UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx    BIGINT       NOT NULL,
    ttl             VARCHAR(200) NOT NULL,
    lgn_id          VARCHAR(200),
    pwd             TEXT         NOT NULL,
    memo            TEXT,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    del_at          TIMESTAMPTZ
);

CREATE INDEX idx_vlt_crd_mbr_acct_del_at ON tb_vlt_crd (mbr_acct_idx, del_at);
CREATE INDEX idx_vlt_crd_created_by_del_at ON tb_vlt_crd (created_by, del_at);

COMMENT ON TABLE tb_vlt_crd IS '개인 자격증명';
COMMENT ON COLUMN tb_vlt_crd.tb_vlt_crd_idx IS '개인 자격증명 PK';
COMMENT ON COLUMN tb_vlt_crd.tb_vlt_crd_uuid IS '개인 자격증명 UUID';
COMMENT ON COLUMN tb_vlt_crd.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_vlt_crd.ttl IS '제목';
COMMENT ON COLUMN tb_vlt_crd.lgn_id IS '로그인 ID';
COMMENT ON COLUMN tb_vlt_crd.pwd IS '비밀번호';
COMMENT ON COLUMN tb_vlt_crd.memo IS '통합 메모';
COMMENT ON COLUMN tb_vlt_crd.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_vlt_crd.created_at IS '생성 일시';
COMMENT ON COLUMN tb_vlt_crd.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_vlt_crd.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_vlt_crd.del_at IS '삭제 일시';
