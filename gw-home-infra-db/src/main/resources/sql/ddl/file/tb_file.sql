CREATE TABLE tb_file (
    file_idx      BIGSERIAL    PRIMARY KEY,
    file_uuid     UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    orgnl_nm      VARCHAR(500) NOT NULL,
    strg_nm       VARCHAR(500) NOT NULL,
    file_path     VARCHAR(1000) NOT NULL,
    file_url      VARCHAR(1000) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    upldr_type    VARCHAR(50)  NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at        TIMESTAMPTZ
);

CREATE INDEX idx_file_created_by ON tb_file (created_by);
