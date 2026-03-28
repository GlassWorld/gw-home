# Todo: 계정관리 기능

> 작업 분류: **HEAVY**
> 관련 리뷰: `review/account/02-account-management.md`

---

## Step 1 — DB: acct_stat 컬럼 추가

- [ ] `tb_mbr_acct_add_acct_stat.sql` 작성 (ALTER TABLE)
- [ ] `tb_mbr_acct_add_acct_stat_rollback.sql` 작성
- [ ] `docs/all-ddl.sql` 에 `acct_stat` 컬럼 반영

---

## Step 2 — VO: AcctVo 수정

- [ ] `AcctVo.java` — `acctStat` 필드 추가

---

## Step 3 — Mapper: AccountMapper 확장

- [ ] `AccountMapper.java` — 다음 메서드 추가
  - `selectAllAccounts(String loginId, String role, String acctStat, int offset, int limit)`
  - `countAllAccounts(String loginId, String role, String acctStat)`
  - `updateRole(String uuid, String role, String updatedBy)`
  - `updateStatus(String uuid, String acctStat, String updatedBy)`
- [ ] `AccountMapper.xml` — 위 SQL 추가 + 기존 SELECT에 `acct_stat` 컬럼 추가

---

## Step 4 — Backend: AdminAccountService 생성

- [ ] `AdminAccountService.java` 생성 — 다음 메서드 구현
  - `getAccounts(...)` — 목록 조회 (페이지네이션)
  - `getAccount(uuid)` — 단건 조회
  - `createAccount(request, adminLoginId)` — 계정 생성
  - `updateRole(uuid, request, adminLoginId)` — 권한 변경 (자기 자신 변경 금지)
  - `updateStatus(uuid, request, adminLoginId)` — 상태 변경 (자기 자신 변경 금지)
  - `deleteAccount(uuid, adminLoginId)` — 소프트 삭제 (자기 자신 삭제 금지)

---

## Step 5 — Backend: DTO 생성 (5개)

- [ ] `AdminAccountListResponse.java` — `(uuid, loginId, email, role, acctStat, lckYn, createdAt)`
- [ ] `AdminAccountDetailResponse.java` — `(uuid, loginId, email, role, acctStat, lckYn, lckAt, createdAt, updatedAt)`
- [ ] `AdminCreateAccountRequest.java` — `(loginId, email, password, role)`
- [ ] `UpdateRoleRequest.java` — `(role)` ADMIN/USER/GUEST 검증
- [ ] `UpdateStatusRequest.java` — `(status)` ACTIVE/INACTIVE 검증

---

## Step 6 — Backend: AdminAccountController 생성

- [ ] `AdminAccountController.java` 생성 — `/api/v1/admin/accounts`
  - `GET /` — 목록 조회 (query: loginId, role, status, page, size)
  - `GET /{uuid}` — 단건 조회
  - `POST /` — 계정 생성
  - `PUT /{uuid}/role` — 권한 변경
  - `PUT /{uuid}/status` — 상태 변경
  - `DELETE /{uuid}` — 삭제

---

## Step 7 — Backend: AuthService INACTIVE 차단

- [ ] `AuthService.java` — `login()` 에서 `acct_stat == 'INACTIVE'` 체크 → `BusinessException(UNAUTHORIZED)` 추가

---

## Step 8 — Frontend: TypeScript 타입 정의

- [ ] `types/admin.ts` 생성
  - `AdminAccount` (목록용)
  - `AdminAccountDetail` (상세용)
  - `AdminCreateAccountForm`
  - `UpdateRoleForm`
  - `UpdateStatusForm`
  - `AccountRole` enum: `'ADMIN' | 'USER' | 'GUEST'`
  - `AccountStatus` enum: `'ACTIVE' | 'INACTIVE'`

---

## Step 9 — Frontend: useAdminAccountApi 생성

- [ ] `composables/useAdminAccountApi.ts` 생성
  - `fetchAccounts(params)` — GET `/api/v1/admin/accounts`
  - `fetchAccount(uuid)` — GET `/api/v1/admin/accounts/{uuid}`
  - `createAccount(form)` — POST `/api/v1/admin/accounts`
  - `updateRole(uuid, form)` — PUT `/api/v1/admin/accounts/{uuid}/role`
  - `updateStatus(uuid, form)` — PUT `/api/v1/admin/accounts/{uuid}/status`
  - `deleteAccount(uuid)` — DELETE `/api/v1/admin/accounts/{uuid}`

---

## Step 10 — Frontend: admin middleware 생성

- [ ] `middleware/admin.ts` 생성 — ADMIN role 미소유 시 `/dashboard` 리다이렉트

---

## Step 11 — Frontend: 계정관리 페이지 생성

- [ ] `pages/admin/accounts/index.vue` 생성
  - `definePageMeta({ middleware: 'admin' })`
  - 계정 목록 테이블 (loginId, email, role, status, 잠금여부, 가입일)
  - 필터: role / status / loginId 검색
  - 페이지네이션
  - 액션: 계정 생성 / 권한 변경 / 상태 변경 / 삭제

---

## Step 12 — Frontend: AppHeader 관리자 메뉴 추가

- [ ] `components/common/AppHeader.vue` — ADMIN role 조건부 `관리자` nav 링크 추가
  - `authStore.currentUser?.role === 'ADMIN'` 일 때만 렌더링
  - 링크 대상: `/admin/accounts`

---

## DB 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성
- [ ] 기존 데이터 영향 검토 → DEFAULT 'ACTIVE' 로 안전
- [ ] NULL / DEFAULT 정책 확인 → NOT NULL + DEFAULT 'ACTIVE'
- [ ] 인덱스 영향 확인 → 추가 불필요
- [ ] 롤백 SQL 작성
- [ ] 운영 반영 순서 정의 → DB ALTER 먼저, 앱 배포 후
- [ ] 데이터 마이그레이션 필요 여부 확인 → 불필요

---

## API 스펙 요약

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/admin/accounts` | 목록 조회 (loginId?, role?, status?, page, size) |
| GET | `/api/v1/admin/accounts/{uuid}` | 단건 조회 |
| POST | `/api/v1/admin/accounts` | 계정 생성 |
| PUT | `/api/v1/admin/accounts/{uuid}/role` | 권한 변경 |
| PUT | `/api/v1/admin/accounts/{uuid}/status` | 상태 변경 |
| DELETE | `/api/v1/admin/accounts/{uuid}` | 삭제 (소프트) |

---

## 관련 문서

- `review/account/02-account-management.md`
