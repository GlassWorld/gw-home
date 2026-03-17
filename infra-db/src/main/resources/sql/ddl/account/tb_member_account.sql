CREATE TABLE tb_member_account (
    member_account_idx   BIGSERIAL    PRIMARY KEY,
    member_account_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    login_id             VARCHAR(100) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    role                 VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
