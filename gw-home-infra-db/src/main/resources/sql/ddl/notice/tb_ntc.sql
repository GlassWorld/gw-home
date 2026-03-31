CREATE TABLE IF NOT EXISTS tb_ntc (
    ntc_idx BIGSERIAL PRIMARY KEY,
    ntc_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    ttl VARCHAR(300) NOT NULL,
    cntnt TEXT NOT NULL,
    view_cnt INT NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ntc_created_at ON tb_ntc (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ntc_del_at_created_at ON tb_ntc (del_at, created_at DESC);
