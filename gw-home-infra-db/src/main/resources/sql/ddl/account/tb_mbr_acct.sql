CREATE TABLE tb_mbr_acct (
    mbr_acct_idx   BIGSERIAL    PRIMARY KEY,
    mbr_acct_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lgn_id         VARCHAR(100) NOT NULL UNIQUE,
    pwd            VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    acct_stat      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    lgn_fail_cnt   INT          NOT NULL DEFAULT 0,
    lck_yn         BOOLEAN      NOT NULL DEFAULT FALSE,
    lck_at         TIMESTAMPTZ,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at         TIMESTAMPTZ
);

-- 계정 잠금 기능 운영 반영용 ALTER SQL
-- ALTER TABLE tb_mbr_acct
--   ADD COLUMN acct_stat VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
--   ADD COLUMN lgn_fail_cnt INT NOT NULL DEFAULT 0,
--   ADD COLUMN lck_yn BOOLEAN NOT NULL DEFAULT FALSE,
--   ADD COLUMN lck_at TIMESTAMPTZ;

-- 계정 잠금 기능 롤백 SQL
-- ALTER TABLE tb_mbr_acct
--   DROP COLUMN acct_stat,
--   DROP COLUMN lgn_fail_cnt,
--   DROP COLUMN lck_yn,
--   DROP COLUMN lck_at;
