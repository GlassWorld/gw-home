CREATE TABLE tb_tag (
    tag_idx      BIGSERIAL    PRIMARY KEY,
    tag_uuid     UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm           VARCHAR(50)  NOT NULL UNIQUE,
    created_by   VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
