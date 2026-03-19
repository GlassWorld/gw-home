# Domain

## 도메인 목록

| 도메인 | 책임 |
|--------|------|
| `account` | 회원 가입, 탈퇴, 계정 정보 관리 |
| `auth` | 로그인, 로그아웃, 토큰 발급/갱신 |
| `profile` | 프로필 조회/수정 (닉네임, 소개, 프로필 이미지) |
| `board` | 게시글 CRUD, 목록/검색, 카테고리 |
| `comment` | 댓글/대댓글 CRUD |
| `file` | 파일/이미지 업로드 및 저장 관리 (독립 도메인) |
| `tag` | 태그 생성/조회, 게시글-태그 매핑 |
| `favorite` | 게시글/댓글 좋아요 토글 |
| `admin` | 회원 관리, 게시글 제재, 통계 |

## 도메인 경계 원칙

- `account` / `auth` / `profile` 분리 — 역방향 참조 금지
  - `auth`는 `account` 참조 가능, 역방향 금지
- `board` / `admin` 분리 — admin은 board를 참조하되 역방향 금지
- `file` 도메인은 독립 — 다른 도메인에서 `file`을 직접 참조하지 않고 URL만 저장
- 게시글 본문 이미지: `board` 테이블의 `img_url` 컬럼에 URL 문자열로 저장
- 도메인 간 직접 호출 최소화 — 필요 시 `share`의 공통 인터페이스 경유

## 도메인별 테이블 축약

| 도메인 | 축약 | 예시 테이블 |
|--------|------|-------------|
| `account` | `mbr_acct` | `tb_mbr_acct` |
| `auth` | `auth` | `tb_auth_tkn` |
| `profile` | `mbr_pfl` | `tb_mbr_pfl` |
| `board` | `brd` | `tb_brd_pst`, `tb_brd_ctgr` |
| `comment` | `brd_cmt` | `tb_brd_cmt` |
| `file` | `file` | `tb_file` |
| `tag` | `tag` | `tb_tag`, `tb_brd_tag` |
| `favorite` | `fvt` | `tb_fvt` |
| `admin` | `adm` | `tb_adm_log` |
