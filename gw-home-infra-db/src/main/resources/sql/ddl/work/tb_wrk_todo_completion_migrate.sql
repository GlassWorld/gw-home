-- 기존 DB에 새 API를 배포하기 전에 실행한다. 완료 시각은 수정 시각으로 역산하지 않는다.
BEGIN;
ALTER TABLE tb_wrk_todo ADD COLUMN IF NOT EXISTS cmpl_at TIMESTAMPTZ;
COMMENT ON COLUMN tb_wrk_todo.cmpl_at IS '최하위 TODO 직접 완료 시각, 기존 완료일 미상은 NULL';
-- 본인 할 일의 일정 정렬과 필터를 지원한다.
CREATE INDEX IF NOT EXISTS idx_wrk_todo_owner_schedule
    ON tb_wrk_todo (mbr_acct_idx, due_dt, strt_dt, wrk_todo_idx) WHERE del_at IS NULL;
COMMIT;
