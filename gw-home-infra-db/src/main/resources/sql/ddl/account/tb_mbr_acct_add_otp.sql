-- OTP 2차 인증 컬럼을 추가한다
ALTER TABLE tb_mbr_acct
  ADD COLUMN otp_enabled        BOOLEAN     NOT NULL DEFAULT FALSE,
  ADD COLUMN otp_secret         TEXT,
  ADD COLUMN otp_fail_cnt       INT         NOT NULL DEFAULT 0,
  ADD COLUMN otp_last_failed_at TIMESTAMPTZ;

-- OTP 2차 인증 컬럼 추가를 롤백한다
ALTER TABLE tb_mbr_acct
  DROP COLUMN otp_enabled,
  DROP COLUMN otp_secret,
  DROP COLUMN otp_fail_cnt,
  DROP COLUMN otp_last_failed_at;
