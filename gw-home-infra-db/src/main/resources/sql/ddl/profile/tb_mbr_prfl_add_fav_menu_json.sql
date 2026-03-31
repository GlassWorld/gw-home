-- 프로필 즐겨찾기 메뉴 컬럼을 추가한다
ALTER TABLE tb_mbr_prfl
  ADD COLUMN fav_menu_json TEXT;

-- 프로필 즐겨찾기 메뉴 컬럼 추가를 롤백한다
ALTER TABLE tb_mbr_prfl
  DROP COLUMN fav_menu_json;
