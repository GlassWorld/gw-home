# 관리자 초기 계정 및 로그인-대시보드 흐름 검증

**분류**: HEAVY (DB 초기 데이터 + 인증·보안 영향)
**의존**: 04-domain-account, 05-domain-auth, 12-domain-admin, 13-ui-module
**참조 review**: [review/auth/01-init-admin.md](../../review/auth/01-init-admin.md)

---

## 목표

DB 초기화 시 기본 관리자 계정이 자동 생성되도록 init SQL을 작성하고,
해당 계정으로 로그인 → JWT 발급 → 메인 대시보드 진입이 끊김 없이 동작하도록 검증한다.

---

## 현황 파악 (탐색 결과)

| 항목 | 현황 |
|------|------|
| `tb_mbr_acct` 구조 | `role VARCHAR(20) DEFAULT 'USER'` — ADMIN 삽입 가능 |
| 비밀번호 암호화 | `BCryptPasswordEncoder` (rounds 10) |
| JWT 발급 | `JwtProvider.generateAccessToken(loginId, role)` — role 포함 |
| JWT 필터 | `JwtAuthenticationFilter` — `ROLE_` 접두사 자동 변환 |
| SecurityConfig | `/api/v1/admin/**` → `hasRole("ADMIN")` 이미 설정됨 |
| 로그인 API | `POST /api/v1/auth/login` 구현 완료 |
| 대시보드 페이지 | `pages/dashboard/index.vue` — `middleware: 'auth'` 적용 완료 |
| 로그인 성공 후 리다이렉트 | `pages/login.vue` → `/dashboard` 이동 구현 완료 |
| 초기 데이터 SQL | **없음 → 신규 작성 필요** |
| 프로필 테이블 | `tb_mbr_prfl` — 회원가입 시 함께 생성 → init SQL에도 포함 필요 |

---

## 작업 목록

### DB — 초기 데이터 SQL

- [x] `gw-home-infra-db/src/main/resources/sql/init/init-data.sql` 생성
  - `tb_mbr_acct` INSERT (role = 'ADMIN', BCrypt 암호화 비밀번호)
  - `tb_mbr_prfl` INSERT (관리자 기본 프로필, 위 계정 idx 참조)
  - 중복 실행 방어: `ON CONFLICT (lgn_id) DO NOTHING`
- [x] 기본 관리자 계정 정보 확정
  ```
  lgn_id : admin
  password : admin!@34   (BCrypt hash 사전 생성하여 SQL에 삽입)
  email  : admin@gw-home.local
  role   : ADMIN
  ```
- [x] BCrypt 해시 생성 방법 문서화 (개발자 가이드 주석으로 SQL 상단에 기재)
- [x] 운영 환경 분리 전략: `application-prod.yml`에 init SQL 비활성화 또는 환경변수로 대체 가능 여부 검토

#### DB 변경 체크리스트

- [x] DDL 변경 SQL 작성 (INSERT only — DDL 구조 변경 없음)
- [x] 기존 데이터 영향 검토 (신규 삽입이므로 영향 없음)
- [x] NULL / DEFAULT 정책 확인 (`created_by` NOT NULL → 'system' 지정)
- [x] 인덱스 영향 확인 (`lgn_id UNIQUE`, `email UNIQUE` 충돌 방지)
- [x] 롤백 SQL 작성 (해당 lgn_id DELETE 문)
- [x] 운영 반영 순서 정의 (DDL → init SQL 순)
- [x] 데이터 마이그레이션 필요 여부 확인 (신규 데이터 삽입이므로 해당 없음)

---

### Backend — 검증

- [x] `AuthService.login()` — role='ADMIN' 계정으로 토큰 정상 발급 확인
  - `generateAccessToken(loginId, "ADMIN")` 호출 경로 확인
- [x] `JwtAuthenticationFilter` — ADMIN role이 `ROLE_ADMIN`으로 변환되어 SecurityContext에 등록되는지 확인
- [x] `SecurityConfig` — `/api/v1/admin/**` hasRole("ADMIN") 설정 재확인
- [x] MyBatis `home` 스키마 커넥션 확인 (`application-local.yml` + DataSource 설정 수정)

---

### Frontend — 검증

- [x] `pages/dashboard/index.vue` — `middleware: 'auth'` 적용 여부 확인
- [x] `pages/login.vue` — 로그인 성공 후 `/dashboard` 리다이렉트 동작 확인
- [x] `use-auth.ts` — 로그인 응답 후 `accessToken` store 저장 + `refreshToken` 쿠키 저장 확인
- [ ] 대시보드에서 인증 헤더로 API 호출 시 401 없이 응답되는지 확인

---

## 완료 기준

- [x] DB 세팅(`init-data.sql` 실행) 후 `SELECT * FROM tb_mbr_acct WHERE lgn_id = 'admin'` 행 존재
- [ ] `POST /api/v1/auth/login` `{ loginId: "admin", password: "admin!@34" }` → 200 + JWT 응답
- [x] 발급된 AccessToken으로 `GET /api/v1/admin/summary` 호출 → 200 (ADMIN 인가 통과)
- [ ] 프론트 `/login` 에서 admin 계정 로그인 성공 → `/dashboard` 이동
- [ ] 대시보드 진입 후 권한 오류(401/403) 없이 페이지 정상 표시

---

## 리스크 및 주의사항

| 리스크 | 대응 |
|--------|------|
| 운영 환경에 개발 기본 계정 노출 | `application-prod.yml`에서 init SQL 제외 또는 ENV 기반 비밀번호 처리 |
| BCrypt 해시 생성 방법 혼동 | SQL 상단에 생성 명령어 주석 기재 (`PasswordUtil.encodeWithBcrypt("admin!@34")`) |
| `tb_mbr_prfl` FK 누락 | account INSERT 선행 후 idx 참조하여 profile INSERT |
| 중복 실행 시 UNIQUE 오류 | `ON CONFLICT DO NOTHING` 처리 |
