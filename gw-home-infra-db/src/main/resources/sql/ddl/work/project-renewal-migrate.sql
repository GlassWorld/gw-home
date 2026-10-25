-- 리뉴얼 2·4단계 기존 DB 적용용. 5단계는 추가 테이블 없이 이 스키마를 재사용한다.
-- 대상: 현재 앱 설정의 home 스키마. 전체 파일을 실행하고 오류 시 ROLLBACK 후 원인을 확인한다.
-- 원본: tb_wrk_todo_completion_migrate.sql (트랜잭션 제외), tb_wrk_ntf.sql.
-- 메일 발송 여부와 무관하게 필요하다. 보고서·Git 데이터, 과거 완료일은 변경하지 않는다.
BEGIN;
SET LOCAL search_path TO home;
SET LOCAL lock_timeout = '5s';

-- 잘못된 스키마 또는 기반 테이블 누락 시 아무 변경 없이 중단한다.
-- 달러 인용을 해석하지 않는 DB 클라이언트에서도 블록을 하나의 문장으로 처리한다.
DO '
DECLARE
    missing_tables TEXT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ''home'') THEN
        RAISE EXCEPTION ''home 스키마가 없습니다. 신규 DB 생성 순서를 먼저 확인하세요.'';
    END IF;
    SELECT string_agg(name, '', '' ORDER BY name) INTO missing_tables
    FROM (VALUES (''tb_mbr_acct''), (''tb_work_unit''), (''tb_wrk_todo'')) AS required(name)
    WHERE to_regclass(format(''home.%I'', name)) IS NULL;
    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION ''기반 테이블이 없습니다: %. 기존 DB용 SQL은 기반 테이블을 자동 생성하지 않습니다.'', missing_tables;
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = ''home.tb_wrk_todo''::regclass AND attname = ''cmpl_at''
          AND NOT attisdropped AND atttypid <> ''timestamptz''::regtype
    ) THEN
        RAISE EXCEPTION ''home.tb_wrk_todo.cmpl_at 타입이 TIMESTAMPTZ가 아닙니다. 자동 변환하지 않습니다.'';
    END IF;
END';

-- 2단계 원본 시작
-- 기존 DB에 새 API를 배포하기 전에 실행한다. 완료 시각은 수정 시각으로 역산하지 않는다.
ALTER TABLE tb_wrk_todo ADD COLUMN IF NOT EXISTS cmpl_at TIMESTAMPTZ;
COMMENT ON COLUMN tb_wrk_todo.cmpl_at IS '최하위 TODO 직접 완료 시각, 기존 완료일 미상은 NULL';
-- 본인 할 일의 일정 정렬과 필터를 지원한다.
CREATE INDEX IF NOT EXISTS idx_wrk_todo_owner_schedule
    ON tb_wrk_todo (mbr_acct_idx, due_dt, strt_dt, wrk_todo_idx) WHERE del_at IS NULL;
-- 2단계 원본 끝

-- 4단계 원본 시작
-- 4단계 추가 스키마: 기존 업무·TODO·보고서 데이터를 변경하지 않는다.
CREATE TABLE IF NOT EXISTS tb_wrk_ntf_stg (
    wrk_ntf_stg_idx BIGSERIAL PRIMARY KEY,
    wrk_ntf_stg_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL UNIQUE REFERENCES tb_mbr_acct(mbr_acct_idx),
    rcpt_email VARCHAR(254) NOT NULL DEFAULT '',
    vrfd_at TIMESTAMPTZ,
    vrfy_hash VARCHAR(100),
    vrfy_expires_at TIMESTAMPTZ,
    vrfy_sent_at TIMESTAMPTZ,
    vrfy_tries INTEGER NOT NULL DEFAULT 0,
    vrfy_window_at TIMESTAMPTZ,
    vrfy_count INTEGER NOT NULL DEFAULT 0,
    dgst_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    rmd_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    tm_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul',
    dgst_time VARCHAR(5) NOT NULL DEFAULT '09:00',
    dgst_cycle VARCHAR(10) NOT NULL DEFAULT 'DAILY' CHECK (dgst_cycle IN ('DAILY','WEEKDAYS')),
    next_dgst_at TIMESTAMPTZ,
    ver BIGINT NOT NULL DEFAULT 1,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_wrk_ntf_stg_due ON tb_wrk_ntf_stg(next_dgst_at) WHERE dgst_enabled AND del_at IS NULL;

-- 계정과 TODO를 함께 제한하는 외래 키를 추가한다.
CREATE UNIQUE INDEX IF NOT EXISTS ux_wrk_todo_idx_owner ON tb_wrk_todo(wrk_todo_idx, mbr_acct_idx);
CREATE TABLE IF NOT EXISTS tb_wrk_todo_rmd (
    wrk_todo_rmd_idx BIGSERIAL PRIMARY KEY,
    wrk_todo_rmd_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL REFERENCES tb_mbr_acct(mbr_acct_idx),
    wrk_todo_idx BIGINT NOT NULL UNIQUE,
    send_at TIMESTAMPTZ NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ver BIGINT NOT NULL DEFAULT 1,
    strt_dt DATE,
    due_dt DATE,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    FOREIGN KEY (wrk_todo_idx,mbr_acct_idx) REFERENCES tb_wrk_todo(wrk_todo_idx,mbr_acct_idx)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_wrk_todo_rmd_idx_owner ON tb_wrk_todo_rmd(wrk_todo_rmd_idx,mbr_acct_idx);
CREATE TABLE IF NOT EXISTS tb_wrk_ntf_dlvr (
    wrk_ntf_dlvr_idx BIGSERIAL PRIMARY KEY,
    wrk_ntf_dlvr_uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx BIGINT NOT NULL REFERENCES tb_mbr_acct(mbr_acct_idx),
    wrk_todo_rmd_idx BIGINT,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('DIGEST','REMINDER')),
    dedup_key VARCHAR(150) NOT NULL,
    stg_ver BIGINT NOT NULL,
    rmd_ver BIGINT,
    due_at TIMESTAMPTZ NOT NULL,
    next_try_at TIMESTAMPTZ NOT NULL,
    sts VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK (sts IN ('PENDING','CLAIMED','SENDING','SENT','RETRY','FAILED','CANCELLED','UNKNOWN')),
    attempts INTEGER NOT NULL DEFAULT 0,
    claim_token UUID,
    lease_until TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    result_cd VARCHAR(40),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at TIMESTAMPTZ,
    UNIQUE(mbr_acct_idx,dedup_key),
    FOREIGN KEY (wrk_todo_rmd_idx,mbr_acct_idx) REFERENCES tb_wrk_todo_rmd(wrk_todo_rmd_idx,mbr_acct_idx)
);
CREATE INDEX IF NOT EXISTS idx_wrk_ntf_dlvr_due ON tb_wrk_ntf_dlvr(next_try_at,wrk_ntf_dlvr_idx) WHERE sts IN ('PENDING','RETRY');
CREATE INDEX IF NOT EXISTS idx_wrk_ntf_dlvr_owner ON tb_wrk_ntf_dlvr(mbr_acct_idx,created_at DESC);
COMMENT ON TABLE tb_wrk_ntf_stg IS '본인 메일 알림 설정과 수신 주소 확인';
COMMENT ON TABLE tb_wrk_todo_rmd IS '최하위 TODO 지정 시각 알림 예약';
COMMENT ON TABLE tb_wrk_ntf_dlvr IS '메일 발송 회차·시도·결과 이력, 본문과 확인 코드는 저장하지 않음';

-- 알림 설정·예약·발송 이력의 컬럼 의미를 기록한다.
COMMENT ON COLUMN tb_wrk_ntf_stg.wrk_ntf_stg_idx IS '알림 설정 PK';
COMMENT ON COLUMN tb_wrk_ntf_stg.wrk_ntf_stg_uuid IS '알림 설정 UUID (외부 노출)';
COMMENT ON COLUMN tb_wrk_ntf_stg.mbr_acct_idx IS '소유 회원 계정 PK';
COMMENT ON COLUMN tb_wrk_ntf_stg.rcpt_email IS '본인 수신 주소';
COMMENT ON COLUMN tb_wrk_ntf_stg.vrfd_at IS '수신 주소 확인 완료 시각';
COMMENT ON COLUMN tb_wrk_ntf_stg.vrfy_hash IS '수신 확인 코드 해시';
COMMENT ON COLUMN tb_wrk_ntf_stg.vrfy_expires_at IS '수신 확인 코드 만료 시각';
COMMENT ON COLUMN tb_wrk_ntf_stg.vrfy_sent_at IS '최근 수신 확인 메일 요청 시각';
COMMENT ON COLUMN tb_wrk_ntf_stg.vrfy_tries IS '현재 확인 코드의 실패 횟수';
COMMENT ON COLUMN tb_wrk_ntf_stg.vrfy_window_at IS '확인 메일 발급 제한 구간 시작 시각';
COMMENT ON COLUMN tb_wrk_ntf_stg.vrfy_count IS '제한 구간 내 확인 메일 발급 횟수';
COMMENT ON COLUMN tb_wrk_ntf_stg.dgst_enabled IS '정기 요약 활성 여부';
COMMENT ON COLUMN tb_wrk_ntf_stg.rmd_enabled IS '개별 알림 활성 여부';
COMMENT ON COLUMN tb_wrk_ntf_stg.tm_zone IS 'IANA 시간대';
COMMENT ON COLUMN tb_wrk_ntf_stg.dgst_time IS '현지 요약 시각 HH:mm';
COMMENT ON COLUMN tb_wrk_ntf_stg.dgst_cycle IS '요약 주기 DAILY 또는 WEEKDAYS';
COMMENT ON COLUMN tb_wrk_ntf_stg.next_dgst_at IS '다음 정기 요약 발송 예정 시각';
COMMENT ON COLUMN tb_wrk_ntf_stg.ver IS '설정 변경 버전';
COMMENT ON COLUMN tb_wrk_ntf_stg.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_ntf_stg.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_wrk_ntf_stg.created_at IS '생성 일시';
COMMENT ON COLUMN tb_wrk_ntf_stg.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_wrk_ntf_stg.del_at IS '삭제 일시';
COMMENT ON COLUMN tb_wrk_todo_rmd.wrk_todo_rmd_idx IS 'TODO 알림 예약 PK';
COMMENT ON COLUMN tb_wrk_todo_rmd.wrk_todo_rmd_uuid IS 'TODO 알림 예약 UUID (외부 노출)';
COMMENT ON COLUMN tb_wrk_todo_rmd.mbr_acct_idx IS '소유 회원 계정 PK';
COMMENT ON COLUMN tb_wrk_todo_rmd.wrk_todo_idx IS '알림 대상 TODO PK';
COMMENT ON COLUMN tb_wrk_todo_rmd.send_at IS '개별 알림 지정 시각';
COMMENT ON COLUMN tb_wrk_todo_rmd.enabled IS '예약 활성 여부';
COMMENT ON COLUMN tb_wrk_todo_rmd.ver IS '설정 변경 버전';
COMMENT ON COLUMN tb_wrk_todo_rmd.strt_dt IS '예약 동기화용 TODO 시작일 스냅샷';
COMMENT ON COLUMN tb_wrk_todo_rmd.due_dt IS '예약 동기화용 TODO 마감일 스냅샷';
COMMENT ON COLUMN tb_wrk_todo_rmd.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_todo_rmd.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_wrk_todo_rmd.created_at IS '생성 일시';
COMMENT ON COLUMN tb_wrk_todo_rmd.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_wrk_todo_rmd.del_at IS '삭제 일시';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.wrk_ntf_dlvr_idx IS '메일 발송 회차 PK';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.wrk_ntf_dlvr_uuid IS '메일 발송 회차 UUID (외부 노출)';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.mbr_acct_idx IS '소유 회원 계정 PK';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.wrk_todo_rmd_idx IS '개별 알림 예약 PK, 요약은 NULL';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.kind IS '발송 종류 DIGEST 또는 REMINDER';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.dedup_key IS '계정 내 중복 발송 방지 키';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.stg_ver IS '발송 예약 당시 알림 설정 버전';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.rmd_ver IS '발송 예약 당시 개별 알림 버전';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.due_at IS '최초 발송 예정 시각';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.next_try_at IS '다음 발송 시도 시각';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.sts IS '발송 상태 PENDING CLAIMED SENDING SENT RETRY FAILED CANCELLED UNKNOWN';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.attempts IS '발송 시도 횟수';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.claim_token IS '작업자 점유 토큰';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.lease_until IS '작업자 점유 만료 시각';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.sent_at IS '발송 성공 시각';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.result_cd IS '실패 또는 취소 사유 코드, 메일 본문은 저장하지 않음';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.created_at IS '생성 일시';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_wrk_ntf_dlvr.del_at IS '삭제 일시';
-- 4단계 원본 끝
COMMIT;
