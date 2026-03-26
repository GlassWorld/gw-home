-- 프로필 메모 컬럼을 추가한다
ALTER TABLE tb_mbr_prfl
  ADD COLUMN memo TEXT;

-- 프로필 메모 컬럼 추가를 롤백한다
ALTER TABLE tb_mbr_prfl
  DROP COLUMN memo;
