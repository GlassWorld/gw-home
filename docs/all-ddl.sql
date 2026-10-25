-- 참고용 생성 스냅샷: 기존 DB에는 실행하지 않는다. 전체 앱의 설치 자동화 파일은 아니다.
-- 기존 DB는 sql/ddl/work/project-renewal-migrate.sql과 docs/backend/database.md를 따른다.
-- PostgreSQL 15 기준. 빈 home 스키마에만 생성하며, 기존 테이블·데이터를 삭제하지 않는다.
BEGIN;
CREATE SCHEMA IF NOT EXISTS home;
SET LOCAL search_path TO home;
-- 달러 인용을 해석하지 않는 DB 클라이언트에서도 블록을 하나의 문장으로 처리한다.
DO '
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = ''home'' AND c.relkind IN (''r'', ''p'', ''v'', ''m'', ''f'')
    ) THEN
        RAISE EXCEPTION ''home 스키마가 비어 있지 않습니다. 기존 DB용 개별 마이그레이션을 사용하세요.'';
    END IF;
END';

CREATE TABLE tb_mbr_acct (
    mbr_acct_idx   BIGSERIAL    PRIMARY KEY,
    mbr_acct_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lgn_id         VARCHAR(100) NOT NULL UNIQUE,
    pwd            VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    acct_stat      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    lgn_fail_cnt   INT          NOT NULL DEFAULT 0,
    lck_yn         BOOLEAN      NOT NULL DEFAULT FALSE,
    lck_at         TIMESTAMPTZ,
    otp_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    otp_secret     TEXT,
    otp_fail_cnt   INT          NOT NULL DEFAULT 0,
    otp_last_failed_at TIMESTAMPTZ,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at         TIMESTAMPTZ
);

COMMENT ON TABLE tb_mbr_acct IS '회원 계정';
COMMENT ON COLUMN tb_mbr_acct.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_mbr_acct.mbr_acct_uuid IS '회원 계정 UUID';
COMMENT ON COLUMN tb_mbr_acct.lgn_id IS '로그인 ID';
COMMENT ON COLUMN tb_mbr_acct.pwd IS '비밀번호 해시';
COMMENT ON COLUMN tb_mbr_acct.email IS '이메일';
COMMENT ON COLUMN tb_mbr_acct.role IS '권한';
COMMENT ON COLUMN tb_mbr_acct.acct_stat IS '계정 상태';
COMMENT ON COLUMN tb_mbr_acct.lgn_fail_cnt IS '로그인 실패 횟수';
COMMENT ON COLUMN tb_mbr_acct.lck_yn IS '계정 잠금 여부';
COMMENT ON COLUMN tb_mbr_acct.lck_at IS '계정 잠금 일시';
COMMENT ON COLUMN tb_mbr_acct.otp_enabled IS 'OTP 활성화 여부';
COMMENT ON COLUMN tb_mbr_acct.otp_secret IS '암호화된 OTP 시크릿';
COMMENT ON COLUMN tb_mbr_acct.otp_fail_cnt IS 'OTP 실패 횟수';
COMMENT ON COLUMN tb_mbr_acct.otp_last_failed_at IS '마지막 OTP 실패 일시';
COMMENT ON COLUMN tb_mbr_acct.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_mbr_acct.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_mbr_acct.created_at IS '생성 일시';
COMMENT ON COLUMN tb_mbr_acct.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_mbr_acct.del_at IS '삭제 일시';

CREATE TABLE tb_auth_rfsh_tkn (
    auth_rfsh_tkn_idx   BIGSERIAL    PRIMARY KEY,
    auth_rfsh_tkn_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx        BIGINT       NOT NULL,
    tkn_hash            VARCHAR(255) NOT NULL,
    expr_at             TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at              TIMESTAMPTZ
);

CREATE INDEX idx_auth_rfsh_tkn_mbr_acct ON tb_auth_rfsh_tkn (mbr_acct_idx);

COMMENT ON TABLE tb_auth_rfsh_tkn IS '리프레시 토큰';
COMMENT ON COLUMN tb_auth_rfsh_tkn.auth_rfsh_tkn_idx IS '리프레시 토큰 PK';
COMMENT ON COLUMN tb_auth_rfsh_tkn.auth_rfsh_tkn_uuid IS '리프레시 토큰 UUID';
COMMENT ON COLUMN tb_auth_rfsh_tkn.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_auth_rfsh_tkn.tkn_hash IS '리프레시 토큰 해시';
COMMENT ON COLUMN tb_auth_rfsh_tkn.expr_at IS '만료 일시';
COMMENT ON COLUMN tb_auth_rfsh_tkn.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_auth_rfsh_tkn.created_at IS '생성 일시';
COMMENT ON COLUMN tb_auth_rfsh_tkn.del_at IS '삭제 일시';

CREATE TABLE tb_mbr_prfl (
    mbr_prfl_idx    BIGSERIAL    PRIMARY KEY,
    mbr_prfl_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx    BIGINT       NOT NULL UNIQUE,
    nick_nm         VARCHAR(50)  NOT NULL UNIQUE,
    intro           VARCHAR(500),
    prfl_img_url    VARCHAR(1000),
    memo            TEXT,
    fav_menu_json   TEXT,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_mbr_prfl_mbr_acct ON tb_mbr_prfl (mbr_acct_idx);

COMMENT ON TABLE tb_mbr_prfl IS '회원 프로필';
COMMENT ON COLUMN tb_mbr_prfl.mbr_prfl_idx IS '프로필 PK';
COMMENT ON COLUMN tb_mbr_prfl.mbr_prfl_uuid IS '프로필 UUID';
COMMENT ON COLUMN tb_mbr_prfl.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_mbr_prfl.nick_nm IS '닉네임';
COMMENT ON COLUMN tb_mbr_prfl.intro IS '자기소개';
COMMENT ON COLUMN tb_mbr_prfl.prfl_img_url IS '프로필 이미지 URL';
COMMENT ON COLUMN tb_mbr_prfl.memo IS '개인 메모';
COMMENT ON COLUMN tb_mbr_prfl.fav_menu_json IS '헤더 즐겨찾기 메뉴 JSON';
COMMENT ON COLUMN tb_mbr_prfl.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_mbr_prfl.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_mbr_prfl.created_at IS '생성 일시';
COMMENT ON COLUMN tb_mbr_prfl.updated_at IS '수정 일시';

CREATE TABLE tb_vlt_cat (
    tb_vlt_cat_idx  BIGSERIAL    PRIMARY KEY,
    tb_vlt_cat_uuid UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm              VARCHAR(100) NOT NULL,
    dsc             TEXT,
    color           VARCHAR(7),
    sort_ord        INT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    del_at          TIMESTAMPTZ
);

CREATE INDEX idx_vlt_cat_del_at ON tb_vlt_cat (del_at);

COMMENT ON TABLE tb_vlt_cat IS 'Vault 카테고리';
COMMENT ON COLUMN tb_vlt_cat.tb_vlt_cat_idx IS 'Vault 카테고리 PK';
COMMENT ON COLUMN tb_vlt_cat.tb_vlt_cat_uuid IS 'Vault 카테고리 UUID';
COMMENT ON COLUMN tb_vlt_cat.nm IS '카테고리명';
COMMENT ON COLUMN tb_vlt_cat.dsc IS '카테고리 설명';
COMMENT ON COLUMN tb_vlt_cat.color IS '카테고리 색상 HEX';
COMMENT ON COLUMN tb_vlt_cat.sort_ord IS '정렬 순서';
COMMENT ON COLUMN tb_vlt_cat.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_vlt_cat.created_at IS '생성 일시';
COMMENT ON COLUMN tb_vlt_cat.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_vlt_cat.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_vlt_cat.del_at IS '삭제 일시';

CREATE TABLE tb_vlt_crd (
    tb_vlt_crd_idx  BIGSERIAL    PRIMARY KEY,
    tb_vlt_crd_uuid UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx    BIGINT       NOT NULL,
    ttl             VARCHAR(200) NOT NULL,
    lgn_id          VARCHAR(200),
    pwd             TEXT         NOT NULL,
    memo            TEXT,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    del_at          TIMESTAMPTZ
);

CREATE INDEX idx_vlt_crd_mbr_acct_del_at ON tb_vlt_crd (mbr_acct_idx, del_at);
CREATE INDEX idx_vlt_crd_created_by_del_at ON tb_vlt_crd (created_by, del_at);

COMMENT ON TABLE tb_vlt_crd IS '개인 자격증명';
COMMENT ON COLUMN tb_vlt_crd.tb_vlt_crd_idx IS '개인 자격증명 PK';
COMMENT ON COLUMN tb_vlt_crd.tb_vlt_crd_uuid IS '개인 자격증명 UUID';
COMMENT ON COLUMN tb_vlt_crd.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_vlt_crd.ttl IS '제목';
COMMENT ON COLUMN tb_vlt_crd.lgn_id IS '로그인 ID';
COMMENT ON COLUMN tb_vlt_crd.pwd IS '비밀번호';
COMMENT ON COLUMN tb_vlt_crd.memo IS '통합 메모';
COMMENT ON COLUMN tb_vlt_crd.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_vlt_crd.created_at IS '생성 일시';
COMMENT ON COLUMN tb_vlt_crd.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_vlt_crd.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_vlt_crd.del_at IS '삭제 일시';

CREATE TABLE tb_vlt_crd_cat (
    vlt_crd_cat_idx BIGSERIAL PRIMARY KEY,
    tb_vlt_crd_idx  BIGINT      NOT NULL REFERENCES tb_vlt_crd(tb_vlt_crd_idx) ON DELETE CASCADE,
    tb_vlt_cat_idx  BIGINT      NOT NULL REFERENCES tb_vlt_cat(tb_vlt_cat_idx) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tb_vlt_crd_idx, tb_vlt_cat_idx)
);

CREATE INDEX idx_vlt_crd_cat_crd_idx ON tb_vlt_crd_cat (tb_vlt_crd_idx);
CREATE INDEX idx_vlt_crd_cat_cat_idx ON tb_vlt_crd_cat (tb_vlt_cat_idx);

COMMENT ON TABLE tb_vlt_crd_cat IS '개인 자격증명-카테고리 매핑';
COMMENT ON COLUMN tb_vlt_crd_cat.vlt_crd_cat_idx IS '자격증명-카테고리 매핑 PK';
COMMENT ON COLUMN tb_vlt_crd_cat.tb_vlt_crd_idx IS '개인 자격증명 PK';
COMMENT ON COLUMN tb_vlt_crd_cat.tb_vlt_cat_idx IS 'Vault 카테고리 PK';
COMMENT ON COLUMN tb_vlt_crd_cat.created_at IS '생성 일시';

CREATE TABLE tb_brd_ctgr (
    brd_ctgr_idx   BIGSERIAL    PRIMARY KEY,
    brd_ctgr_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm             VARCHAR(50)  NOT NULL UNIQUE,
    sort_ord       INT          NOT NULL DEFAULT 0,
    created_by     VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE tb_brd_ctgr IS '게시판 카테고리';
COMMENT ON COLUMN tb_brd_ctgr.brd_ctgr_idx IS '게시판 카테고리 PK';
COMMENT ON COLUMN tb_brd_ctgr.brd_ctgr_uuid IS '게시판 카테고리 UUID';
COMMENT ON COLUMN tb_brd_ctgr.nm IS '카테고리명';
COMMENT ON COLUMN tb_brd_ctgr.sort_ord IS '정렬 순서';
COMMENT ON COLUMN tb_brd_ctgr.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_brd_ctgr.created_at IS '생성 일시';

CREATE TABLE tb_brd_pst (
    brd_pst_idx    BIGSERIAL    PRIMARY KEY,
    brd_pst_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_ctgr_idx   BIGINT,
    mbr_acct_idx   BIGINT       NOT NULL,
    ttl            VARCHAR(300) NOT NULL,
    cntnt          TEXT         NOT NULL,
    view_cnt       INT          NOT NULL DEFAULT 0,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at         TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_ctgr ON tb_brd_pst (brd_ctgr_idx);
CREATE INDEX idx_brd_pst_mbr_acct ON tb_brd_pst (mbr_acct_idx);
CREATE INDEX idx_brd_pst_created_at ON tb_brd_pst (created_at DESC);

COMMENT ON TABLE tb_brd_pst IS '게시글';
COMMENT ON COLUMN tb_brd_pst.brd_pst_idx IS '게시글 PK';
COMMENT ON COLUMN tb_brd_pst.brd_pst_uuid IS '게시글 UUID';
COMMENT ON COLUMN tb_brd_pst.brd_ctgr_idx IS '게시판 카테고리 PK';
COMMENT ON COLUMN tb_brd_pst.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_brd_pst.ttl IS '제목';
COMMENT ON COLUMN tb_brd_pst.cntnt IS '본문';
COMMENT ON COLUMN tb_brd_pst.view_cnt IS '조회 수';
COMMENT ON COLUMN tb_brd_pst.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_brd_pst.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_brd_pst.created_at IS '생성 일시';
COMMENT ON COLUMN tb_brd_pst.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_brd_pst.del_at IS '삭제 일시';

CREATE TABLE tb_brd_pst_shr (
    brd_pst_shr_idx   BIGSERIAL    PRIMARY KEY,
    brd_pst_shr_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_pst_idx       BIGINT       NOT NULL,
    shr_tkn           VARCHAR(255) NOT NULL UNIQUE,
    pwd_hash          VARCHAR(255),
    pwd_use_yn        VARCHAR(1)   NOT NULL DEFAULT 'N',
    actv_yn           VARCHAR(1)   NOT NULL DEFAULT 'Y',
    expr_at           TIMESTAMPTZ  NOT NULL,
    rvkd_at           TIMESTAMPTZ,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at            TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_shr_brd_pst ON tb_brd_pst_shr (brd_pst_idx);
CREATE INDEX idx_brd_pst_shr_expr_at ON tb_brd_pst_shr (expr_at);

COMMENT ON TABLE tb_brd_pst_shr IS '게시글 외부 공유';
COMMENT ON COLUMN tb_brd_pst_shr.brd_pst_shr_idx IS '게시글 외부 공유 PK';
COMMENT ON COLUMN tb_brd_pst_shr.brd_pst_shr_uuid IS '게시글 외부 공유 UUID';
COMMENT ON COLUMN tb_brd_pst_shr.brd_pst_idx IS '게시글 PK';
COMMENT ON COLUMN tb_brd_pst_shr.shr_tkn IS '공유 토큰';
COMMENT ON COLUMN tb_brd_pst_shr.pwd_hash IS '공유 비밀번호 해시';
COMMENT ON COLUMN tb_brd_pst_shr.pwd_use_yn IS '공유 비밀번호 사용 여부';
COMMENT ON COLUMN tb_brd_pst_shr.actv_yn IS '공유 활성 여부';
COMMENT ON COLUMN tb_brd_pst_shr.expr_at IS '공유 만료 일시';
COMMENT ON COLUMN tb_brd_pst_shr.rvkd_at IS '공유 해제 일시';
COMMENT ON COLUMN tb_brd_pst_shr.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_brd_pst_shr.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_brd_pst_shr.created_at IS '생성 일시';
COMMENT ON COLUMN tb_brd_pst_shr.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_brd_pst_shr.del_at IS '삭제 일시';

CREATE TABLE tb_brd_pst_file (
    brd_pst_file_idx   BIGSERIAL    PRIMARY KEY,
    brd_pst_file_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_pst_idx        BIGINT       NOT NULL,
    file_idx           BIGINT       NOT NULL,
    file_role          VARCHAR(30)  NOT NULL,
    sort_ord           INTEGER      NOT NULL DEFAULT 0,
    created_by         VARCHAR(100) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at             TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_file_brd_pst ON tb_brd_pst_file (brd_pst_idx);
CREATE INDEX idx_brd_pst_file_file ON tb_brd_pst_file (file_idx);

COMMENT ON TABLE tb_brd_pst_file IS '게시글 첨부파일 매핑';
COMMENT ON COLUMN tb_brd_pst_file.brd_pst_file_idx IS '게시글 첨부 매핑 PK';
COMMENT ON COLUMN tb_brd_pst_file.brd_pst_file_uuid IS '게시글 첨부 매핑 UUID';
COMMENT ON COLUMN tb_brd_pst_file.brd_pst_idx IS '게시글 PK';
COMMENT ON COLUMN tb_brd_pst_file.file_idx IS '파일 PK';
COMMENT ON COLUMN tb_brd_pst_file.file_role IS '첨부 역할';
COMMENT ON COLUMN tb_brd_pst_file.sort_ord IS '정렬 순서';
COMMENT ON COLUMN tb_brd_pst_file.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_brd_pst_file.created_at IS '생성 일시';
COMMENT ON COLUMN tb_brd_pst_file.del_at IS '삭제 일시';

CREATE TABLE tb_brd_cmt (
    brd_cmt_idx       BIGSERIAL     PRIMARY KEY,
    brd_cmt_uuid      UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    brd_pst_idx       BIGINT        NOT NULL,
    prnt_brd_cmt_idx  BIGINT,
    mbr_acct_idx      BIGINT        NOT NULL,
    cntnt             VARCHAR(2000) NOT NULL,
    created_by        VARCHAR(100)  NOT NULL,
    updated_by        VARCHAR(100),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    del_at            TIMESTAMPTZ
);

CREATE INDEX idx_brd_cmt_brd_pst ON tb_brd_cmt (brd_pst_idx);
CREATE INDEX idx_brd_cmt_prnt ON tb_brd_cmt (prnt_brd_cmt_idx);

COMMENT ON TABLE tb_brd_cmt IS '게시글 댓글';
COMMENT ON COLUMN tb_brd_cmt.brd_cmt_idx IS '댓글 PK';
COMMENT ON COLUMN tb_brd_cmt.brd_cmt_uuid IS '댓글 UUID';
COMMENT ON COLUMN tb_brd_cmt.brd_pst_idx IS '게시글 PK';
COMMENT ON COLUMN tb_brd_cmt.prnt_brd_cmt_idx IS '부모 댓글 PK';
COMMENT ON COLUMN tb_brd_cmt.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_brd_cmt.cntnt IS '댓글 내용';
COMMENT ON COLUMN tb_brd_cmt.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_brd_cmt.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_brd_cmt.created_at IS '생성 일시';
COMMENT ON COLUMN tb_brd_cmt.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_brd_cmt.del_at IS '삭제 일시';

CREATE TABLE tb_file (
    file_idx      BIGSERIAL    PRIMARY KEY,
    file_uuid     UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    orgnl_nm      VARCHAR(500) NOT NULL,
    strg_nm       VARCHAR(500) NOT NULL,
    file_path     VARCHAR(1000) NOT NULL,
    file_url      VARCHAR(1000) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    upldr_type    VARCHAR(50)  NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at        TIMESTAMPTZ
);

CREATE INDEX idx_file_created_by ON tb_file (created_by);

COMMENT ON TABLE tb_file IS '업로드 파일 메타데이터';
COMMENT ON COLUMN tb_file.file_idx IS '파일 PK';
COMMENT ON COLUMN tb_file.file_uuid IS '파일 UUID';
COMMENT ON COLUMN tb_file.orgnl_nm IS '원본 파일명';
COMMENT ON COLUMN tb_file.strg_nm IS '저장 파일명';
COMMENT ON COLUMN tb_file.file_path IS '파일 저장 경로';
COMMENT ON COLUMN tb_file.file_url IS '파일 접근 URL';
COMMENT ON COLUMN tb_file.mime_type IS 'MIME 타입';
COMMENT ON COLUMN tb_file.file_size IS '파일 크기';
COMMENT ON COLUMN tb_file.upldr_type IS '업로더 타입';
COMMENT ON COLUMN tb_file.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_file.created_at IS '생성 일시';
COMMENT ON COLUMN tb_file.del_at IS '삭제 일시';

CREATE TABLE tb_tag (
    tag_idx      BIGSERIAL    PRIMARY KEY,
    tag_uuid     UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nm           VARCHAR(50)  NOT NULL UNIQUE,
    created_by   VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE tb_tag IS '태그';
COMMENT ON COLUMN tb_tag.tag_idx IS '태그 PK';
COMMENT ON COLUMN tb_tag.tag_uuid IS '태그 UUID';
COMMENT ON COLUMN tb_tag.nm IS '태그명';
COMMENT ON COLUMN tb_tag.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_tag.created_at IS '생성 일시';

CREATE TABLE tb_brd_pst_tag (
    brd_pst_tag_idx  BIGSERIAL   PRIMARY KEY,
    brd_pst_idx      BIGINT      NOT NULL,
    tag_idx          BIGINT      NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (brd_pst_idx, tag_idx)
);

CREATE INDEX idx_brd_pst_tag_brd_pst ON tb_brd_pst_tag (brd_pst_idx);
CREATE INDEX idx_brd_pst_tag_tag ON tb_brd_pst_tag (tag_idx);

COMMENT ON TABLE tb_brd_pst_tag IS '게시글-태그 매핑';
COMMENT ON COLUMN tb_brd_pst_tag.brd_pst_tag_idx IS '게시글 태그 매핑 PK';
COMMENT ON COLUMN tb_brd_pst_tag.brd_pst_idx IS '게시글 PK';
COMMENT ON COLUMN tb_brd_pst_tag.tag_idx IS '태그 PK';
COMMENT ON COLUMN tb_brd_pst_tag.created_at IS '생성 일시';

CREATE TABLE tb_fav (
    fav_idx       BIGSERIAL    PRIMARY KEY,
    fav_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    trgt_type     VARCHAR(20)  NOT NULL,
    trgt_idx      BIGINT       NOT NULL,
    mbr_acct_idx  BIGINT       NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_tb_fav_trgt_mbr ON tb_fav (trgt_type, trgt_idx, mbr_acct_idx);
CREATE INDEX idx_fav_trgt ON tb_fav (trgt_type, trgt_idx);
CREATE INDEX idx_fav_mbr_acct ON tb_fav (mbr_acct_idx);

COMMENT ON TABLE tb_fav IS '좋아요';
COMMENT ON COLUMN tb_fav.fav_idx IS '좋아요 PK';
COMMENT ON COLUMN tb_fav.fav_uuid IS '좋아요 UUID';
COMMENT ON COLUMN tb_fav.trgt_type IS '대상 타입';
COMMENT ON COLUMN tb_fav.trgt_idx IS '대상 PK';
COMMENT ON COLUMN tb_fav.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_fav.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_fav.created_at IS '생성 일시';

CREATE TABLE tb_work_unit (
    work_unit_idx   BIGSERIAL    PRIMARY KEY,
    work_unit_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx    BIGINT       NOT NULL,
    ttl             VARCHAR(200) NOT NULL,
    dscr            TEXT,
    ctgr            VARCHAR(100),
    sts             VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    use_yn          CHAR(1)      NOT NULL DEFAULT 'Y',
    use_cnt         INTEGER      NOT NULL DEFAULT 0,
    last_used_at    TIMESTAMPTZ,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at          TIMESTAMPTZ,
    CONSTRAINT chk_work_unit_sts CHECK (sts IN ('IN_PROGRESS', 'DONE', 'ON_HOLD')),
    CONSTRAINT chk_work_unit_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE INDEX idx_work_unit_mbr_acct ON tb_work_unit (mbr_acct_idx);
CREATE INDEX idx_work_unit_listing ON tb_work_unit (mbr_acct_idx, use_yn, updated_at DESC, created_at DESC);
CREATE INDEX idx_work_unit_last_used_at ON tb_work_unit (mbr_acct_idx, last_used_at DESC);
CREATE UNIQUE INDEX ux_work_unit_idx_owner ON tb_work_unit (work_unit_idx, mbr_acct_idx);

COMMENT ON TABLE tb_work_unit IS '업무 등록';
COMMENT ON COLUMN tb_work_unit.work_unit_idx IS '업무 PK';
COMMENT ON COLUMN tb_work_unit.work_unit_uuid IS '업무 UUID';
COMMENT ON COLUMN tb_work_unit.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_work_unit.ttl IS '업무명';
COMMENT ON COLUMN tb_work_unit.dscr IS '업무 설명';
COMMENT ON COLUMN tb_work_unit.ctgr IS '업무 카테고리';
COMMENT ON COLUMN tb_work_unit.sts IS '업무 상태';
COMMENT ON COLUMN tb_work_unit.use_yn IS '사용 여부';
COMMENT ON COLUMN tb_work_unit.use_cnt IS '업무 사용 횟수';
COMMENT ON COLUMN tb_work_unit.last_used_at IS '최근 사용 일시';
COMMENT ON COLUMN tb_work_unit.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_work_unit.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_work_unit.created_at IS '생성 일시';
COMMENT ON COLUMN tb_work_unit.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_work_unit.del_at IS '삭제 일시';

CREATE TABLE tb_wrk_todo (
    wrk_todo_idx       BIGSERIAL    PRIMARY KEY,
    wrk_todo_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    work_unit_idx      BIGINT       NOT NULL,
    mbr_acct_idx       BIGINT       NOT NULL,
    prnt_wrk_todo_idx  BIGINT,
    ttl                VARCHAR(200) NOT NULL,
    dscr               TEXT,
    sts                VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    prgs_rt            INTEGER      NOT NULL DEFAULT 0,
    strt_dt            DATE,
    due_dt             DATE,
    cmpl_at            TIMESTAMPTZ,
    sort_ord           INTEGER      NOT NULL DEFAULT 1,
    created_by         VARCHAR(100) NOT NULL,
    updated_by         VARCHAR(100),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at             TIMESTAMPTZ,
    CONSTRAINT chk_wrk_todo_sts CHECK (sts IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_wrk_todo_prgs_rt CHECK (prgs_rt >= 0 AND prgs_rt <= 100)
);

CREATE INDEX idx_wrk_todo_work_unit ON tb_wrk_todo (work_unit_idx, del_at);
CREATE INDEX idx_wrk_todo_owner ON tb_wrk_todo (mbr_acct_idx, del_at);
CREATE INDEX idx_wrk_todo_parent_sort ON tb_wrk_todo (work_unit_idx, prnt_wrk_todo_idx, sort_ord, del_at);
CREATE INDEX idx_wrk_todo_owner_schedule ON tb_wrk_todo (mbr_acct_idx, due_dt, strt_dt, wrk_todo_idx) WHERE del_at IS NULL;

COMMENT ON TABLE tb_wrk_todo IS '업무 TODO 트리';
COMMENT ON COLUMN tb_wrk_todo.wrk_todo_idx IS '업무 TODO PK';
COMMENT ON COLUMN tb_wrk_todo.wrk_todo_uuid IS '업무 TODO UUID';
COMMENT ON COLUMN tb_wrk_todo.work_unit_idx IS '업무 PK';
COMMENT ON COLUMN tb_wrk_todo.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_wrk_todo.prnt_wrk_todo_idx IS '부모 업무 TODO PK';
COMMENT ON COLUMN tb_wrk_todo.ttl IS 'TODO 제목';
COMMENT ON COLUMN tb_wrk_todo.dscr IS 'TODO 설명';
COMMENT ON COLUMN tb_wrk_todo.sts IS 'TODO 상태';
COMMENT ON COLUMN tb_wrk_todo.prgs_rt IS '진행률';
COMMENT ON COLUMN tb_wrk_todo.strt_dt IS '시작일';
COMMENT ON COLUMN tb_wrk_todo.due_dt IS '마감일';
COMMENT ON COLUMN tb_wrk_todo.cmpl_at IS '최하위 TODO 직접 완료 시각, 기존 완료일 미상은 NULL';
COMMENT ON COLUMN tb_wrk_todo.sort_ord IS '정렬 순서';
COMMENT ON COLUMN tb_wrk_todo.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_todo.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_wrk_todo.created_at IS '생성 일시';
COMMENT ON COLUMN tb_wrk_todo.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_wrk_todo.del_at IS '삭제 일시';

-- 보관 스키마: 일일보고·업무 연결은 앱 기능 종료 후에도 데이터를 유지한다. 물리 삭제는 별도 작업이다.
CREATE TABLE tb_wrk_dly_rpt (
    wrk_dly_rpt_idx   BIGSERIAL    PRIMARY KEY,
    wrk_dly_rpt_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx      BIGINT       NOT NULL,
    rpt_dt            DATE         NOT NULL,
    cntn              TEXT,
    spcl_note         TEXT,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at            TIMESTAMPTZ,
    CONSTRAINT uq_wrk_dly_rpt_owner_date UNIQUE (mbr_acct_idx, rpt_dt)
);

CREATE UNIQUE INDEX ux_wrk_dly_rpt_idx_owner ON tb_wrk_dly_rpt (wrk_dly_rpt_idx, mbr_acct_idx);
CREATE INDEX idx_wrk_dly_rpt_owner_date ON tb_wrk_dly_rpt (mbr_acct_idx, rpt_dt DESC);
CREATE INDEX idx_wrk_dly_rpt_date ON tb_wrk_dly_rpt (rpt_dt DESC);

COMMENT ON TABLE tb_wrk_dly_rpt IS '일일보고';
COMMENT ON COLUMN tb_wrk_dly_rpt.wrk_dly_rpt_idx IS '일일보고 PK';
COMMENT ON COLUMN tb_wrk_dly_rpt.wrk_dly_rpt_uuid IS '일일보고 UUID';
COMMENT ON COLUMN tb_wrk_dly_rpt.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_wrk_dly_rpt.rpt_dt IS '보고 일자';
COMMENT ON COLUMN tb_wrk_dly_rpt.cntn IS '보고 본문';
COMMENT ON COLUMN tb_wrk_dly_rpt.spcl_note IS '특이사항';
COMMENT ON COLUMN tb_wrk_dly_rpt.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_dly_rpt.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_wrk_dly_rpt.created_at IS '생성 일시';
COMMENT ON COLUMN tb_wrk_dly_rpt.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_wrk_dly_rpt.del_at IS '삭제 일시';

CREATE TABLE tb_wrk_dly_rpt_wrk_unit (
    wrk_dly_rpt_wrk_unit_idx  BIGSERIAL    PRIMARY KEY,
    wrk_dly_rpt_idx           BIGINT       NOT NULL,
    mbr_acct_idx              BIGINT       NOT NULL,
    work_unit_idx             BIGINT       NOT NULL,
    created_by                VARCHAR(100) NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_wrk_dly_rpt_wrk_unit UNIQUE (wrk_dly_rpt_idx, work_unit_idx)
);

CREATE INDEX idx_wrk_dly_rpt_wrk_unit_rpt ON tb_wrk_dly_rpt_wrk_unit (wrk_dly_rpt_idx);
CREATE INDEX idx_wrk_dly_rpt_wrk_unit_owner ON tb_wrk_dly_rpt_wrk_unit (mbr_acct_idx);
CREATE INDEX idx_wrk_dly_rpt_wrk_unit_unit ON tb_wrk_dly_rpt_wrk_unit (work_unit_idx);
CREATE UNIQUE INDEX ux_wrk_dly_rpt_wrk_unit_report_work_unit ON tb_wrk_dly_rpt_wrk_unit (wrk_dly_rpt_idx, work_unit_idx);

COMMENT ON TABLE tb_wrk_dly_rpt_wrk_unit IS '일일보고-업무 연결';
COMMENT ON COLUMN tb_wrk_dly_rpt_wrk_unit.wrk_dly_rpt_wrk_unit_idx IS '일일보고 업무 연결 PK';
COMMENT ON COLUMN tb_wrk_dly_rpt_wrk_unit.wrk_dly_rpt_idx IS '일일보고 PK';
COMMENT ON COLUMN tb_wrk_dly_rpt_wrk_unit.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_wrk_dly_rpt_wrk_unit.work_unit_idx IS '업무 PK';
COMMENT ON COLUMN tb_wrk_dly_rpt_wrk_unit.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_dly_rpt_wrk_unit.created_at IS '생성 일시';

-- 보관 스키마: 주간보고는 앱 기능 종료 후에도 데이터를 유지한다. 물리 삭제는 별도 작업이다.
CREATE TABLE tb_wrk_wkl_rpt (
    wrk_wkl_rpt_idx   BIGSERIAL    PRIMARY KEY,
    wrk_wkl_rpt_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx      BIGINT       NOT NULL,
    wk_strt_dt        DATE         NOT NULL,
    wk_end_dt         DATE         NOT NULL,
    ttl               VARCHAR(200) NOT NULL,
    cntn              TEXT         NOT NULL,
    opn_yn            CHAR(1)      NOT NULL DEFAULT 'N',
    pbls_at           TIMESTAMPTZ,
    gen_type          VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at            TIMESTAMPTZ,
    CONSTRAINT chk_wrk_wkl_rpt_opn_yn CHECK (opn_yn IN ('Y', 'N')),
    CONSTRAINT chk_wrk_wkl_rpt_gen_type CHECK (gen_type IN ('MANUAL', 'OPENAI', 'RULE_BASED'))
);

CREATE INDEX idx_wrk_wkl_rpt_owner_week ON tb_wrk_wkl_rpt (mbr_acct_idx, wk_strt_dt DESC, wk_end_dt DESC);
CREATE INDEX idx_wrk_wkl_rpt_open ON tb_wrk_wkl_rpt (opn_yn, pbls_at DESC);

COMMENT ON TABLE tb_wrk_wkl_rpt IS '주간보고';
COMMENT ON COLUMN tb_wrk_wkl_rpt.wrk_wkl_rpt_idx IS '주간보고 PK';
COMMENT ON COLUMN tb_wrk_wkl_rpt.wrk_wkl_rpt_uuid IS '주간보고 UUID';
COMMENT ON COLUMN tb_wrk_wkl_rpt.mbr_acct_idx IS '회원 계정 PK';
COMMENT ON COLUMN tb_wrk_wkl_rpt.wk_strt_dt IS '주 시작일';
COMMENT ON COLUMN tb_wrk_wkl_rpt.wk_end_dt IS '주 종료일';
COMMENT ON COLUMN tb_wrk_wkl_rpt.ttl IS '보고 제목';
COMMENT ON COLUMN tb_wrk_wkl_rpt.cntn IS '보고 본문';
COMMENT ON COLUMN tb_wrk_wkl_rpt.opn_yn IS '공개 여부';
COMMENT ON COLUMN tb_wrk_wkl_rpt.pbls_at IS '공개 일시';
COMMENT ON COLUMN tb_wrk_wkl_rpt.gen_type IS '생성 방식';
COMMENT ON COLUMN tb_wrk_wkl_rpt.created_by IS '생성자 로그인 ID';
COMMENT ON COLUMN tb_wrk_wkl_rpt.updated_by IS '수정자 로그인 ID';
COMMENT ON COLUMN tb_wrk_wkl_rpt.created_at IS '생성 일시';
COMMENT ON COLUMN tb_wrk_wkl_rpt.updated_at IS '수정 일시';
COMMENT ON COLUMN tb_wrk_wkl_rpt.del_at IS '삭제 일시';

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

COMMIT;
