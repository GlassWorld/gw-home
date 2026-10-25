-- 선택 적용: 배포 전 tb_mbr_prfl 백업을 보관하고 프로필 쓰기가 적은 시간에 실행한다.
-- 보고서 메뉴 네 경로만 제거한다. 나머지 값·순서, 보고서/업무/TODO/Git 데이터는 보존한다.
-- 잘못된 JSON·배열이 아닌 값은 건너뛰며, 재실행 시 이미 정리된 행은 수정하지 않는다.
BEGIN;

DO $$
DECLARE
    profile_row RECORD;
    original_items JSONB;
    retained_items JSONB;
BEGIN
    -- 후보 행을 잠가 정리 도중 다른 즐겨찾기 저장 결과를 덮어쓰지 않는다.
    FOR profile_row IN
        SELECT mbr_prfl_idx, fav_menu_json
        FROM tb_mbr_prfl
        WHERE fav_menu_json LIKE '%reports%'
        FOR UPDATE
    LOOP
        BEGIN
            original_items := profile_row.fav_menu_json::JSONB;
        EXCEPTION WHEN invalid_text_representation THEN
            CONTINUE;
        END;
        IF jsonb_typeof(original_items) IS DISTINCT FROM 'array' THEN
            CONTINUE;
        END IF;

        -- 정확히 일치하는 경로만 제외하고 원래 배열 순서를 유지한다.
        SELECT COALESCE(jsonb_agg(item ORDER BY position), '[]'::JSONB)
        INTO retained_items
        FROM jsonb_array_elements(original_items) WITH ORDINALITY AS items(item, position)
        WHERE item NOT IN (
            '"/weekly-reports"'::JSONB,
            '"/work/daily-reports"'::JSONB,
            '"/work/weekly-reports"'::JSONB,
            '"/admin/daily-reports"'::JSONB
        );

        IF retained_items IS DISTINCT FROM original_items THEN
            UPDATE tb_mbr_prfl
            SET fav_menu_json = retained_items::TEXT,
                updated_by = 'report-retirement',
                updated_at = now()
            WHERE mbr_prfl_idx = profile_row.mbr_prfl_idx;
        END IF;
    END LOOP;
END $$;

COMMIT;
