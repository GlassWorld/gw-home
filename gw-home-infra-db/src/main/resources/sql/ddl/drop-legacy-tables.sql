-- ============================================================
-- 구버전 테이블 삭제 DDL
-- 대상: tb_acnt 설계 기반 테이블 (현재 tb_mbr_acct 기반으로 대체됨)
-- 실행 전 확인: home 스키마 데이터 백업 여부
-- 롤백: 해당 테이블 재생성 필요 (DDL 별도 보관)
-- ============================================================

-- 1. FK 참조 테이블 먼저 삭제
DROP TABLE IF EXISTS home.tb_brd_pst_fav;   -- tb_acnt.acnt_idx 참조
DROP TABLE IF EXISTS home.tb_acnt_prof;     -- tb_acnt.acnt_idx 참조

-- 2. 루트 테이블 삭제
DROP TABLE IF EXISTS home.tb_acnt;
