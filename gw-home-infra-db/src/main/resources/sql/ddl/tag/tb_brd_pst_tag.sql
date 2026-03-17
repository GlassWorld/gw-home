CREATE TABLE tb_brd_pst_tag (
    brd_pst_tag_idx  BIGSERIAL   PRIMARY KEY,
    brd_pst_idx      BIGINT      NOT NULL,
    tag_idx          BIGINT      NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (brd_pst_idx, tag_idx)
);

CREATE INDEX idx_brd_pst_tag_brd_pst ON tb_brd_pst_tag (brd_pst_idx);
CREATE INDEX idx_brd_pst_tag_tag ON tb_brd_pst_tag (tag_idx);
