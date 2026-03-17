# Todo Overview

## 작업 목적
이 디렉토리의 문서를 순서대로 실행하여 프로젝트 전체 소스를 생성한다.

## 실행 전 필독
- `.ai/CORE_RULES.md` — 절대 규칙
- `.ai/AGENTS.md` — 진입 절차
- `docs/rules.md` — 네이밍/패키지 규칙

## 작업 순서

| 순서 | 파일 | 내용 |
|------|------|------|
| 1 | `01-project-setup.md` | Gradle 멀티모듈 + 공통 설정 |
| 2 | `02-share-module.md` | 공통 모듈 (DTO, 예외, 유틸) |
| 3 | `03-infra-db-module.md` | DB 설정, MyBatis 설정 |
| 4 | `04-domain-account.md` | 회원 가입/탈퇴/조회 |
| 5 | `05-domain-auth.md` | 로그인/토큰 |
| 6 | `06-domain-profile.md` | 프로필 조회/수정 |
| 7 | `07-domain-board.md` | 게시글 CRUD |
| 8 | `08-domain-comment.md` | 댓글/대댓글 |
| 9 | `09-domain-file.md` | 파일 업로드 |
| 10 | `10-domain-tag.md` | 태그 |
| 11 | `11-domain-favorite.md` | 좋아요 |
| 12 | `12-domain-admin.md` | 관리자 |

## 의존 관계

```
01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09 → 10 → 11 → 12
```
- 각 단계는 이전 단계 완료 후 진행
- 04~12는 독립적으로 병렬 작업 가능 (단, 02/03 완료 후)

## 완료 기준

- [x] 01 project-setup
- [x] 02 share-module
- [x] 03 infra-db-module
- [x] 04 domain-account
- [x] 05 domain-auth
- [x] 06 domain-profile
- [x] 07 domain-board
- [x] 08 domain-comment
- [x] 09 domain-file
- [x] 10 domain-tag
- [x] 11 domain-favorite
- [x] 12 domain-admin
