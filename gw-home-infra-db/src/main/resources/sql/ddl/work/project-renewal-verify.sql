-- 읽기 전용: 적용 전·후 모두 실행할 수 있다. 어떤 테이블이나 데이터도 변경하지 않는다.
-- required = true 중 ok = false가 있으면 리뉴얼 DB 준비가 완료되지 않은 것이다.
-- required = false는 보고서·Git 보존 현황이다. 적용 전후를 비교하며, 누락만으로 새로 만들지 않는다.
WITH expected_tables(name, required) AS (
    VALUES
        ('tb_mbr_acct', true),
        ('tb_work_unit', true),
        ('tb_wrk_todo', true),
        ('tb_wrk_ntf_stg', true),
        ('tb_wrk_todo_rmd', true),
        ('tb_wrk_ntf_dlvr', true),
        ('tb_wrk_dly_rpt', false),
        ('tb_wrk_dly_rpt_wrk_unit', false),
        ('tb_wrk_wkl_rpt', false),
        ('tb_wrk_git_acct', false),
        ('tb_wrk_git_prj', false),
        ('tb_wrk_unit_git_cfg', false),
        ('tb_wrk_unit_git_prj', false)
), expected_columns(table_name, column_name, data_type) AS (
    VALUES
        ('tb_wrk_todo', 'cmpl_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'wrk_ntf_stg_idx', 'bigint'),
        ('tb_wrk_ntf_stg', 'wrk_ntf_stg_uuid', 'uuid'),
        ('tb_wrk_ntf_stg', 'mbr_acct_idx', 'bigint'),
        ('tb_wrk_ntf_stg', 'rcpt_email', 'character varying'),
        ('tb_wrk_ntf_stg', 'vrfd_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'vrfy_hash', 'character varying'),
        ('tb_wrk_ntf_stg', 'vrfy_expires_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'vrfy_sent_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'vrfy_tries', 'integer'),
        ('tb_wrk_ntf_stg', 'vrfy_window_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'vrfy_count', 'integer'),
        ('tb_wrk_ntf_stg', 'dgst_enabled', 'boolean'),
        ('tb_wrk_ntf_stg', 'rmd_enabled', 'boolean'),
        ('tb_wrk_ntf_stg', 'tm_zone', 'character varying'),
        ('tb_wrk_ntf_stg', 'dgst_time', 'character varying'),
        ('tb_wrk_ntf_stg', 'dgst_cycle', 'character varying'),
        ('tb_wrk_ntf_stg', 'next_dgst_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'ver', 'bigint'),
        ('tb_wrk_ntf_stg', 'created_by', 'character varying'),
        ('tb_wrk_ntf_stg', 'updated_by', 'character varying'),
        ('tb_wrk_ntf_stg', 'created_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'updated_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_stg', 'del_at', 'timestamp with time zone'),
        ('tb_wrk_todo_rmd', 'wrk_todo_rmd_idx', 'bigint'),
        ('tb_wrk_todo_rmd', 'wrk_todo_rmd_uuid', 'uuid'),
        ('tb_wrk_todo_rmd', 'mbr_acct_idx', 'bigint'),
        ('tb_wrk_todo_rmd', 'wrk_todo_idx', 'bigint'),
        ('tb_wrk_todo_rmd', 'send_at', 'timestamp with time zone'),
        ('tb_wrk_todo_rmd', 'enabled', 'boolean'),
        ('tb_wrk_todo_rmd', 'ver', 'bigint'),
        ('tb_wrk_todo_rmd', 'strt_dt', 'date'),
        ('tb_wrk_todo_rmd', 'due_dt', 'date'),
        ('tb_wrk_todo_rmd', 'created_by', 'character varying'),
        ('tb_wrk_todo_rmd', 'updated_by', 'character varying'),
        ('tb_wrk_todo_rmd', 'created_at', 'timestamp with time zone'),
        ('tb_wrk_todo_rmd', 'updated_at', 'timestamp with time zone'),
        ('tb_wrk_todo_rmd', 'del_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_dlvr', 'wrk_ntf_dlvr_idx', 'bigint'),
        ('tb_wrk_ntf_dlvr', 'wrk_ntf_dlvr_uuid', 'uuid'),
        ('tb_wrk_ntf_dlvr', 'mbr_acct_idx', 'bigint'),
        ('tb_wrk_ntf_dlvr', 'wrk_todo_rmd_idx', 'bigint'),
        ('tb_wrk_ntf_dlvr', 'kind', 'character varying'),
        ('tb_wrk_ntf_dlvr', 'dedup_key', 'character varying'),
        ('tb_wrk_ntf_dlvr', 'stg_ver', 'bigint'),
        ('tb_wrk_ntf_dlvr', 'rmd_ver', 'bigint'),
        ('tb_wrk_ntf_dlvr', 'due_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_dlvr', 'next_try_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_dlvr', 'sts', 'character varying'),
        ('tb_wrk_ntf_dlvr', 'attempts', 'integer'),
        ('tb_wrk_ntf_dlvr', 'claim_token', 'uuid'),
        ('tb_wrk_ntf_dlvr', 'lease_until', 'timestamp with time zone'),
        ('tb_wrk_ntf_dlvr', 'sent_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_dlvr', 'result_cd', 'character varying'),
        ('tb_wrk_ntf_dlvr', 'created_by', 'character varying'),
        ('tb_wrk_ntf_dlvr', 'updated_by', 'character varying'),
        ('tb_wrk_ntf_dlvr', 'created_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_dlvr', 'updated_at', 'timestamp with time zone'),
        ('tb_wrk_ntf_dlvr', 'del_at', 'timestamp with time zone')
), expected_indexes(table_name, index_name, is_unique) AS (
    VALUES
        ('tb_wrk_todo', 'idx_wrk_todo_owner_schedule', false),
        ('tb_wrk_todo', 'ux_wrk_todo_idx_owner', true),
        ('tb_wrk_ntf_stg', 'idx_wrk_ntf_stg_due', false),
        ('tb_wrk_todo_rmd', 'ux_wrk_todo_rmd_idx_owner', true),
        ('tb_wrk_ntf_dlvr', 'idx_wrk_ntf_dlvr_due', false),
        ('tb_wrk_ntf_dlvr', 'idx_wrk_ntf_dlvr_owner', false)
)
SELECT 'TABLE' AS kind, name AS object_name, required,
       to_regclass(format('home.%I', name)) IS NOT NULL AS ok,
       'home' AS expected
FROM expected_tables
UNION ALL
SELECT 'COLUMN', e.table_name || '.' || e.column_name, true,
       EXISTS (
           SELECT 1 FROM pg_attribute a
           WHERE a.attrelid = to_regclass(format('home.%I', e.table_name))
             AND a.attname = e.column_name AND a.attnum > 0 AND NOT a.attisdropped
             AND format_type(a.atttypid, NULL) = e.data_type
       ), e.data_type
FROM expected_columns e
UNION ALL
SELECT 'INDEX', e.index_name, true,
       EXISTS (
           SELECT 1 FROM pg_index i
           WHERE i.indexrelid = to_regclass(format('home.%I', e.index_name))
             AND i.indrelid = to_regclass(format('home.%I', e.table_name))
             AND i.indisvalid AND i.indisready AND i.indisunique = e.is_unique
       ), e.table_name
FROM expected_indexes e
ORDER BY kind, object_name;
