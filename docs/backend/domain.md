# 도메인 정의

## 이 문서의 목적

이 문서는 각 도메인이 어떤 책임을 가지는지와 도메인 경계를 어떻게 나누는지를 설명한다.

## 도메인 목록

| 도메인 | 책임 |
|--------|------|
| `account` | 회원 가입, 탈퇴, 계정 정보 관리 |
| `auth` | 로그인, 로그아웃, 토큰 발급과 갱신 |
| `profile` | 프로필 조회와 수정 |
| `board` | 게시글 CRUD, 목록, 검색, 카테고리 |
| `comment` | 댓글과 대댓글 CRUD |
| `file` | 파일과 이미지 업로드, 저장 관리 |
| `tag` | 태그 생성, 조회, 게시글-태그 매핑 |
| `favorite` | 좋아요 토글 |
| `admin` | 회원 관리, 제재, 운영 기능 |

## 도메인 경계 원칙

- `account`, `auth`, `profile`은 서로 다른 책임으로 분리한다
- `auth`는 `account`를 참조할 수 있지만 역방향 참조는 허용하지 않는다
- `board`와 `admin`은 분리한다
- `admin`은 운영 목적으로 `board`를 참조할 수 있지만 역방향 참조는 허용하지 않는다
- `file`은 독립 도메인으로 유지하고 다른 도메인은 URL만 저장한다
- 도메인 간 직접 호출은 최소화한다

## 도메인별 테이블 축약

| 도메인 | 축약 | 예시 |
|--------|------|------|
| `account` | `mbr_acct` | `tb_mbr_acct` |
| `auth` | `auth` | `tb_auth_tkn` |
| `profile` | `mbr_pfl` | `tb_mbr_pfl` |
| `board` | `brd` | `tb_brd_pst`, `tb_brd_ctgr` |
| `comment` | `brd_cmt` | `tb_brd_cmt` |
| `file` | `file` | `tb_file` |
| `tag` | `tag` | `tb_tag`, `tb_brd_tag` |
| `favorite` | `fvt` | `tb_fvt` |
| `admin` | `adm` | `tb_adm_log` |

## 설계 시 참고 포인트

- 새로운 기능이 기존 도메인 책임에 포함되는지 먼저 판단한다
- 관리 기능이라고 해서 무조건 `admin`에 넣지 말고 운영 목적 여부를 확인한다
- 파일 저장이 필요하면 파일 자체는 `file`, 참조만 각 도메인에 둔다
