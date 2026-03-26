# Review: 계정관리 메뉴 생성

## 작업 개요

Admin 전용 계정관리 페이지 신규 생성.
신규 계정 등록(가입) 및 기존 회원 권한(role) 변경 기능 제공.

## 변경 목적

- 관리자가 UI에서 직접 계정을 생성하고 권한을 부여할 수 있도록 함
- 현재는 가입(POST /api/v1/accounts) API는 있지만, role 변경 API가 없음
- 회원 목록 조회/삭제는 `/api/v1/admin/members` 로 이미 구현되어 있음

## 예상 영향 범위

### Backend
| 레이어 | 파일 | 변경 내용 |
|--------|------|-----------|
| Controller | `AdminController.java` | PATCH /api/v1/admin/members/{uuid}/role 엔드포인트 추가 |
| DTO | `AdminUpdateRoleRequest.java` (신규) | `{ "role": "ADMIN" \| "USER" }` |
| Service | `AdminService.java` | `updateMemberRole()` 메서드 추가 |
| Mapper | `AdminMapper.java` | `updateMemberRole()` 인터페이스 추가 |
| XML | `AdminMapper.xml` | updateMemberRole SQL 추가 |

> 기존 `AccountMapper` / `AcctVo` 는 건드리지 않고 `AdminMapper` 에서 직접 UPDATE

### Frontend
| 파일 | 변경 내용 |
|------|-----------|
| `pages/admin/accounts/index.vue` (신규) | 회원 목록 + 권한 변경 + 신규 등록 폼 |
| `components/common/AppHeader.vue` | Admin 네비게이션 링크 추가 |
| `composables/useAdminApi.ts` (신규) | admin members API 연동 |
| `types/api/admin.ts` (신규) | AdminMember 타입 |

### 인증/보안
- `admin` 미들웨어 재사용 (기존 `middleware/admin.ts`)
- 백엔드는 `@PreAuthorize("hasRole('ADMIN')")` 이미 적용된 `AdminController` 에 추가

## 기능 상세

### 신규 계정 등록
- 기존 `POST /api/v1/accounts` (SignUp API) 그대로 활용
- 폼: 로그인ID, 비밀번호, 이메일
- 생성 후 권한은 기본 USER → 즉시 role 변경 가능

### 권한 변경
- API: `PATCH /api/v1/admin/members/{memberAccountUuid}/role`
- body: `{ "role": "ADMIN" | "USER" }`
- 자신의 계정 권한 강등 방지 (본인 UUID 비교)

### 회원 목록
- 기존 `GET /api/v1/admin/members` 활용 (페이지네이션, 검색 포함)
- 상태: 활성/삭제 표시
- 각 행: 권한 변경 버튼, 삭제 버튼

## 리스크 분석

| 리스크 | 대응 |
|--------|------|
| admin이 자신의 role을 USER로 변경 | 백엔드에서 Principal.getName() 비교 후 차단 |
| 신규 등록 시 비밀번호 노출 | HTTPS 환경 가정, 관리자 전용 기능이므로 허용 |
| 기존 AccountController 영향 | 없음 — AdminController에만 추가 |

## 작업 분류

**HEAVY** — 신규 Backend API(role 변경) + 신규 Frontend 페이지 + Mapper XML 변경 + 공통 헤더 수정

## 관련 파일

- `review/admin/01-account-management.md` (이 파일)
- API: `gw-home-api/.../controller/admin/AdminController.java`
- 기존 회원 API: `gw-home-api/.../dto/admin/AdminMemberResponse.java`
- 기존 가입 API: `gw-home-api/.../controller/account/AccountController.java`
- 기존 admin 미들웨어: `gw-home-ui/middleware/admin.ts`
