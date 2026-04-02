# 페이지 안내

## 이 문서의 목적

이 문서는 현재 프론트엔드 라우트와 각 페이지의 역할을 이해하기 위한 안내서다.

## 주요 라우트

| 경로 | 파일 | 설명 | 인증 필요 |
|------|------|------|-----------|
| `/` | `pages/index.vue` | 루트 진입 시 기본 페이지로 이동 | X |
| `/login` | `pages/login.vue` | 로그인 화면 | X |
| `/dashboard` | `pages/dashboard/index.vue` | 대시보드 | O |
| `/notices` | `pages/notices/index.vue` | 공지사항 목록 | O |
| `/notices/[noticeUuid]` | `pages/notices/[noticeUuid].vue` | 공지사항 상세 | O |
| `/board` | `pages/board/index.vue` | 게시글 목록 | O |
| `/board/[boardUuid]` | `pages/board/[boardUuid].vue` | 게시글 상세 | O |
| `/board/create` | `pages/board/create.vue` | 게시글 작성 | O |
| `/work` | `pages/work/index.vue` | 업무 등록과 조회 | O |
| `/work/daily-reports` | `pages/work/daily-reports/index.vue` | 일일보고 작성과 조회 | O |
| `/work/daily-reports/create` | `pages/work/daily-reports/create.vue` | 일일보고 작성 | O |
| `/work/daily-reports/[dailyReportUuid]/edit` | `pages/work/daily-reports/[dailyReportUuid]/edit.vue` | 일일보고 수정 | O |
| `/work/weekly-reports` | `pages/work/weekly-reports/index.vue` | 주간보고 작성과 조회 | O |
| `/work/git-accounts` | `pages/work/git-accounts/index.vue` | Git 계정 및 저장소 연동 관리 | O |
| `/vault` | `pages/vault/index.vue` | 자격증명 보관함 | O |
| `/settings` | `pages/settings/index.vue` | 계정 설정 | O |
| `/security` | `pages/security/index.vue` | OTP와 보안 설정 | O |
| `/admin/accounts` | `pages/admin/accounts/index.vue` | 관리자 계정 관리 | O |
| `/admin/notices` | `pages/admin/notices/index.vue` | 관리자 공지 관리 | O |
| `/admin/daily-reports` | `pages/admin/daily-reports/index.vue` | 관리자 일일보고 관리 | O |
| `/admin/vault-categories` | `pages/admin/vault-categories/index.vue` | 관리자 금고 카테고리 관리 | O |

## 초기 진입 흐름

```text
사용자 접속
  -> /
  -> 기본 페이지로 이동
  -> 인증 필요 페이지면 auth 미들웨어 검사
  -> 비인증 상태면 /login 으로 이동
```

## 페이지별 역할

### 로그인

- 로그인 ID와 비밀번호 입력
- 이미 로그인한 사용자는 대시보드로 이동

### 대시보드

- 로그인 사용자 요약 정보
- 최근 공지와 최근 게시글 표시
- 주요 화면으로 이동하는 빠른 링크 제공

### 공지사항

- 공지 목록 조회
- 공지 상세 확인

### 게시글

- 목록, 검색, 상세 조회
- 작성자 본인의 수정과 삭제
- 새 게시글 작성

### 설정과 보안

- 계정 정보 수정
- OTP 설정과 보안 설정 관리

### 업무와 관리자 화면

- 업무, 일일보고, 주간보고 작성과 조회
- Git 계정과 저장소 연동 관리
- 관리자 전용 운영 기능 제공

## 미들웨어

```text
middleware/
├── auth.ts   인증 필요 페이지 접근 제어
├── guest.ts  로그인 페이지 같은 비인증 전용 화면 제어
└── admin.ts  관리자 전용 화면 접근 제어
```
