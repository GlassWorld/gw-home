CREATE TABLE tb_brd_pst_file (
    brd_pst_file_idx   BIGSERIAL    PRIMARY KEY,
    brd_pst_file_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_pst_idx        BIGINT       NOT NULL,
    file_idx           BIGINT       NOT NULL,
    file_role          VARCHAR(30)  NOT NULL,
    sort_ord           INTEGER      NOT NULL DEFAULT 0,
    created_by         VARCHAR(100) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at             TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_file_brd_pst ON tb_brd_pst_file (brd_pst_idx);
CREATE INDEX idx_brd_pst_file_file ON tb_brd_pst_file (file_idx);
