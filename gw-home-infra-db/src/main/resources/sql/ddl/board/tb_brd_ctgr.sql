CREATE TABLE tb_brd_ctgr (
    brd_ctgr_idx   BIGSERIAL    PRIMARY KEY,
    brd_ctgr_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm             VARCHAR(50)  NOT NULL UNIQUE,
    sort_ord       INT          NOT NULL DEFAULT 0,
    created_by     VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
