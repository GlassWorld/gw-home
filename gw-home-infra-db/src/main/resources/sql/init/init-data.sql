-- [개발용 초기 데이터] 운영 환경에서는 자동 실행하지 말고 별도 시드 전략을 사용한다.
-- BCrypt 해시 생성 예시:
-- PasswordUtil.encodeWithBcrypt("admin!@34")
-- Rollback SQL:
-- DELETE FROM tb_mbr_prfl
-- WHERE mbr_acct_idx = (SELECT mbr_acct_idx FROM tb_mbr_acct WHERE lgn_id = 'admin');
-- DELETE FROM tb_mbr_acct WHERE lgn_id = 'admin';

INSERT INTO tb_mbr_acct (
    lgn_id,
    pwd,
    email,
    role,
    created_by
)
SELECT
    'admin',
    '$2a$10$okcY7.JTQKdPlB5TKyEipuKjRL6gepDk5h7caUU./mvql0sT.oExS',
    'chjsa11@naver.com',
    'ADMIN',
    'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_mbr_acct
    WHERE lgn_id = 'admin'
       OR email = 'chjsa11@naver.com'
)
ON CONFLICT (lgn_id) DO NOTHING;

INSERT INTO tb_mbr_prfl (
    mbr_acct_idx,
    nick_nm,
    intro,
    created_by
)
SELECT
    acct.mbr_acct_idx,
    '관리자',
    '기본 관리자 계정',
    'system'
FROM tb_mbr_acct acct
WHERE acct.lgn_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM tb_mbr_prfl
      WHERE mbr_acct_idx = acct.mbr_acct_idx
  )
  AND NOT EXISTS (
      SELECT 1
      FROM tb_mbr_prfl
      WHERE nick_nm = '관리자'
  );
