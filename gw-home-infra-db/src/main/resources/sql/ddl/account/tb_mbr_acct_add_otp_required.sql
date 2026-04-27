-- 관리자 선택형 OTP 요구 여부 컬럼을 추가한다
ALTER TABLE tb_mbr_acct
  ADD COLUMN otp_required BOOLEAN NOT NULL DEFAULT TRUE;

-- 관리자 선택형 OTP 요구 여부 컬럼 추가를 롤백한다
ALTER TABLE tb_mbr_acct
  DROP COLUMN otp_required;
