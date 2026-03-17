CREATE TABLE tb_auth_rfsh_tkn (
    auth_rfsh_tkn_idx   BIGSERIAL    PRIMARY KEY,
    auth_rfsh_tkn_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx        BIGINT       NOT NULL,
    tkn_hash            VARCHAR(255) NOT NULL,
    expr_at             TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at              TIMESTAMPTZ
);

CREATE INDEX idx_auth_rfsh_tkn_mbr_acct ON tb_auth_rfsh_tkn (mbr_acct_idx);
