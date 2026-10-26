-- 기존 home 스키마에 매일 체크 테이블을 추가한다.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL search_path TO home;

-- 매일 반복 항목의 시작일과 마지막 체크 가능 날짜를 보존한다.
CREATE TABLE IF NOT EXISTS tb_lf_daily (
    lf_daily_idx BIGSERIAL PRIMARY KEY,
    lf_daily_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL REFERENCES tb_mbr_acct(mbr_acct_idx),
    ttl VARCHAR(200) NOT NULL CHECK (length(btrim(ttl)) > 0),
    start_dt DATE NOT NULL,
    end_dt DATE CHECK (end_dt IS NULL OR end_dt >= start_dt),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    UNIQUE (lf_daily_idx, mbr_acct_idx)
);

-- 항목·현지 날짜당 완료 기록을 하나만 저장하며 취소는 del_at으로 구분한다.
CREATE TABLE IF NOT EXISTS tb_lf_daily_check (
    lf_daily_check_idx BIGSERIAL PRIMARY KEY,
    lf_daily_check_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lf_daily_idx BIGINT NOT NULL,
    mbr_acct_idx BIGINT NOT NULL,
    check_dt DATE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    UNIQUE (lf_daily_idx, check_dt),
    FOREIGN KEY (lf_daily_idx, mbr_acct_idx) REFERENCES tb_lf_daily(lf_daily_idx, mbr_acct_idx)
);

-- 소유자의 월별 항목과 완료 기록 조회를 지원한다.
CREATE INDEX IF NOT EXISTS idx_lf_daily_owner ON tb_lf_daily (mbr_acct_idx, start_dt) WHERE del_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_lf_daily_check_owner_date ON tb_lf_daily_check (mbr_acct_idx, check_dt) WHERE del_at IS NULL;

-- 날짜는 현지 달력 날짜이며 종료일 당일까지 체크할 수 있다.
COMMENT ON TABLE tb_lf_daily IS '매일 반복하는 일상 체크 항목';
COMMENT ON COLUMN tb_lf_daily.start_dt IS '반복 시작 날짜';
COMMENT ON COLUMN tb_lf_daily.end_dt IS '마지막 체크 가능 날짜, NULL은 반복 중';
COMMENT ON TABLE tb_lf_daily_check IS '항목별 날짜별 완료 기록';
COMMENT ON COLUMN tb_lf_daily_check.check_dt IS '사용자 시간대의 현지 날짜';
COMMENT ON COLUMN tb_lf_daily_check.del_at IS 'NULL은 완료, 값이 있으면 완료 취소';

COMMIT;
