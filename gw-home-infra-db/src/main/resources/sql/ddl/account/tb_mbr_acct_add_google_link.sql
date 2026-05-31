-- Google 계정 연동 정보를 추가한다
ALTER TABLE tb_mbr_acct
  ADD COLUMN google_sub VARCHAR(255),
  ADD COLUMN google_email VARCHAR(255),
  ADD COLUMN google_linked_at TIMESTAMPTZ;

-- 활성 계정 기준 Google 계정 식별자 중복을 방지한다
CREATE UNIQUE INDEX ux_tb_mbr_acct_google_sub
  ON tb_mbr_acct (google_sub)
  WHERE google_sub IS NOT NULL AND del_at IS NULL;

-- Google 계정 연동 정보 추가를 롤백한다
DROP INDEX ux_tb_mbr_acct_google_sub;

ALTER TABLE tb_mbr_acct
  DROP COLUMN google_sub,
  DROP COLUMN google_email,
  DROP COLUMN google_linked_at;
