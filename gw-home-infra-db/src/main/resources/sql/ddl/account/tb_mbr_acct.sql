CREATE TABLE tb_mbr_acct (
    mbr_acct_idx   BIGSERIAL    PRIMARY KEY,
    mbr_acct_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lgn_id         VARCHAR(100) NOT NULL UNIQUE,
    pwd            VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at         TIMESTAMPTZ
);
