# Review: 계정관리 기능

> 작업 분류: **HEAVY**
> DB 스키마 변경 / 공통 VO 수정 / 인증 흐름 영향 / Full-stack

---

## 작업 개요

ADMIN 권한을 가진 사용자만 접근 가능한 계정관리 기능.
- 권한: ADMIN / USER / GUEST 3단계 관리
- ADMIN: 계정 생성·수정·삭제·권한변경·상태변경
- 일반 로그인/인증 구조와 연결 가능한 확장성 설계

---

## 변경 목적

- 사용자 계정에 대한 중앙 관리 기능 제공
- role 기반 접근 제어 강화 (ADMIN / USER / GUEST 명시적 분리)
- 계정 상태(ACTIVE / INACTIVE)를 시스템 잠금(`lck_yn`)과 분리하여 관리

---

## 설계 결정 사항

### 1. acct_stat 컬럼 추가 (신규)

`lck_yn`은 로그인 실패로 인한 시스템 잠금 용도이므로 관리자 상태 제어와 분리.

```
acct_stat VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
값: ACTIVE (정상) / INACTIVE (관리자 비활성화)
```

### 2. role 값 범위

DB 컬럼은 이미 VARCHAR(20). 값 제약은 애플리케이션 레벨에서 검증.
```
ADMIN / USER / GUEST
```

### 3. Admin API 경로

`/api/v1/admin/accounts` — SecurityConfig에서 이미 `hasRole("ADMIN")` 적용 중.

### 4. 로그인 시 INACTIVE 계정 차단

`AuthService.login()` 에서 `acct_stat == 'INACTIVE'` 체크 추가.
→ `auth` → `account` 참조 방향이므로 도메인 경계 규칙 준수.

### 5. 관리자의 계정 삭제

소프트 삭제(`del_at`) 사용. 기존 `deleteAccount` 로직 재사용.

### 6. 프론트 Admin 페이지

`/admin/accounts` 페이지. ADMIN role 미소유 시 접근 차단 (middleware).

### 7. 헤더 네비게이션 — 관리자 메뉴

`AppHeader.vue` nav에 `관리자` 링크 추가.
`authStore.currentUser?.role === 'ADMIN'` 조건부 렌더링 → ADMIN에게만 노출.

---

## 영향 범위

### DB 변경

| 항목 | 내용 |
|------|------|
| 테이블 | `tb_mbr_acct` |
| 변경 | `acct_stat VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` 컬럼 추가 |
| 기존 데이터 | DEFAULT 'ACTIVE' 로 일괄 적용 — 영향 없음 |
| 인덱스 | 추가 불필요 (관리자 화면 조회는 풀스캔 허용 범위) |
| 롤백 | ALTER TABLE ... DROP COLUMN acct_stat |

### Backend 변경

| 레이어 | 파일 | 변경 내용 |
|--------|------|-----------|
| VO (share) | `AcctVo.java` | `acctStat` 필드 추가 |
| Mapper IF | `AccountMapper.java` | `selectAllAccounts`, `updateRole`, `updateStatus`, `unlockAccount` 추가 |
| Mapper XML | `AccountMapper.xml` | 위 SQL 추가, 기존 SELECT에 `acct_stat` 컬럼 추가 |
| Service (신규) | `AdminAccountService.java` | 관리자 계정 CRUD 로직 |
| Controller (신규) | `AdminAccountController.java` | `/api/v1/admin/accounts` |
| DTO (신규 4개) | `AdminAccountListResponse`, `AdminAccountDetailResponse`, `AdminCreateAccountRequest`, `UpdateRoleRequest`, `UpdateStatusRequest` | |
| Service (기존) | `AuthService.java` | login 시 acct_stat INACTIVE 체크 추가 |
| DDL | `tb_mbr_acct_add_acct_stat.sql` | ALTER TABLE |
| DDL | `tb_mbr_acct_add_acct_stat_rollback.sql` | 롤백 SQL |

### Frontend 변경

| 파일 | 변경 내용 |
|------|-----------|
| `pages/admin/accounts/index.vue` (신규) | 계정 목록 + 필터 + 액션 |
| `composables/useAdminAccountApi.ts` (신규) | 관리자 계정 API 연동 |
| `types/admin.ts` (신규) | Admin 관련 TypeScript 타입 |
| `middleware/admin.ts` (신규) | ADMIN role 체크 middleware |

---

## 리스크 분석

| 리스크 | 대응 |
|--------|------|
| 기존 로그인 데이터에 acct_stat NULL 없음 | DEFAULT 'ACTIVE' 지정으로 안전 |
| ADMIN이 자기 자신을 비활성화/삭제 | 서비스단에서 자기 자신 변경 금지 검증 |
| GUEST role 부여 시 기존 인증된 JWT는 여전히 유효 | role 변경 후 기존 토큰은 만료까지 유효 — 허용 가능 범위 |
| INACTIVE 계정이 기존 토큰으로 API 접근 | stateless JWT 구조상 token 만료 전까지 접근 가능 — 현재 범위 외, 별도 작업으로 분리 가능 |
| 대량 계정 목록 성능 | 페이지네이션 필수 |

---

## 작업 순서

1. **DB** — DDL 작성 및 마이그레이션
2. **VO** — `AcctVo` 에 `acctStat` 추가
3. **Mapper** — `AccountMapper` 추가 메서드 + XML
4. **AdminAccountService** — 관리자 서비스 로직
5. **AdminAccountController** — Admin API 엔드포인트
6. **AuthService** — INACTIVE 계정 로그인 차단
7. **Frontend** — types → composable → page → middleware

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

## 전체 파일 목록

### 신규 (Backend — 7개)
```
gw-home-api/.../controller/account/AdminAccountController.java
gw-home-api/.../service/account/AdminAccountService.java
gw-home-api/.../dto/account/AdminAccountListResponse.java
gw-home-api/.../dto/account/AdminAccountDetailResponse.java
gw-home-api/.../dto/account/AdminCreateAccountRequest.java
gw-home-api/.../dto/account/UpdateRoleRequest.java
gw-home-api/.../dto/account/UpdateStatusRequest.java
gw-home-infra-db/.../sql/ddl/account/tb_mbr_acct_add_acct_stat.sql
gw-home-infra-db/.../sql/ddl/account/tb_mbr_acct_add_acct_stat_rollback.sql
```

### 신규 (Frontend — 4개)
```
gw-home-ui/pages/admin/accounts/index.vue
gw-home-ui/composables/useAdminAccountApi.ts
gw-home-ui/types/admin.ts
gw-home-ui/middleware/admin.ts
```

### 변경 (Backend — 4개)
```
gw-home-share/.../vo/account/AcctVo.java             acctStat 필드 추가
gw-home-infra-db/.../mapper/account/AccountMapper.java   admin 쿼리 추가
gw-home-infra-db/.../mapper/account/AccountMapper.xml    admin SQL + acct_stat 컬럼 추가
gw-home-api/.../service/auth/AuthService.java            INACTIVE 계정 차단
docs/all-ddl.sql                                         acct_stat 반영
```

### 변경 (Frontend — 1개)
```
gw-home-ui/components/common/AppHeader.vue    ADMIN role 조건부 관리자 메뉴 추가
```

---

## 관련 문서

- `review/account/01-account-settings-page.md` — 계정 설정 페이지
- `gw-home-api/.../config/SecurityConfig.java` — admin 경로 보안 설정
- `gw-home-api/.../jwt/JwtAuthenticationFilter.java` — role 기반 인증
