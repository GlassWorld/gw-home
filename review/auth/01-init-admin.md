# Review: 관리자 초기 계정 및 로그인-대시보드 흐름

**분류**: HEAVY
**참조 todo**: [todo/14-init-admin.md](../../todo/14-init-admin.md)
**작성일**: 2026-03-22

---

## 작업 개요

DB 초기화 시 기본 관리자 계정을 자동 생성하는 init SQL을 추가하고,
해당 계정으로 로그인 → JWT 발급 → 대시보드 진입 흐름이 정상 동작하는지 검증한다.

---

## 변경 목적

- 개발/테스트 환경에서 매번 수동으로 계정을 생성하지 않아도 되도록 init 데이터 제공
- 관리자 기능(`/api/v1/admin/**`) 동작 검증을 위한 ADMIN 권한 계정 필요
- 로그인 → JWT → 대시보드 전체 흐름의 E2E 정상 동작 확인

---

## 현황 분석

### 이미 구현된 항목

| 항목 | 파일 | 비고 |
|------|------|------|
| JWT 발급 | `JwtProvider.java` | `generateAccessToken(loginId, role)` |
| JWT 필터 | `JwtAuthenticationFilter.java` | `ROLE_` 접두사 자동 변환 |
| Security 설정 | `SecurityConfig.java` | `/api/v1/admin/**` → `hasRole("ADMIN")` |
| 로그인 API | `POST /api/v1/auth/login` | loginId + password → AccessToken + RefreshToken |
| 로그인 페이지 | `pages/login.vue` | 성공 시 `/dashboard` 리다이렉트 |
| 대시보드 페이지 | `pages/dashboard/index.vue` | `middleware: 'auth'` 적용 |
| auth middleware | `middleware/auth.ts` | 미인증 → `/login` 리다이렉트 |
| 토큰 저장 | `use-auth.ts` / `auth.store.ts` | AccessToken: 메모리, RefreshToken: HttpOnly 쿠키 |

### 누락된 항목

| 항목 | 위치 | 상태 |
|------|------|------|
| init-data.sql | `sql/init/init-data.sql` | **없음 → 신규 생성 필요** |
| admin 계정 INSERT | `tb_mbr_acct` | **없음** |
| admin 프로필 INSERT | `tb_mbr_prfl` | **없음** |

---

## 예상 영향 범위

### 변경 레이어

| 레이어 | 변경 내용 | 영향 |
|--------|-----------|------|
| DB (데이터) | `tb_mbr_acct`, `tb_mbr_prfl` INSERT | 신규 행 추가, 구조 변경 없음 |
| Backend | 변경 없음 | — |
| Frontend | 변경 없음 (기존 구현 사용) | — |

### 영향 받는 API

| API | 영향 |
|-----|------|
| `POST /api/v1/auth/login` | admin 계정으로 정상 응답 여부 확인 필요 |
| `GET /api/v1/admin/**` | ADMIN role 인가 통과 여부 확인 필요 |

### 프론트 영향

없음. 기존 로그인 / 대시보드 구현 그대로 사용.

### 데이터 영향

- 기존 데이터 변경 없음
- `tb_mbr_acct.lgn_id UNIQUE`, `email UNIQUE`, `tb_mbr_prfl.nick_nm UNIQUE` 충돌 가능성
  → `ON CONFLICT DO NOTHING` 처리 필수

---

## 구현 계획

### 1. BCrypt 해시 생성

init SQL에 삽입할 비밀번호 해시를 사전 생성한다.

```java
// Spring 코드 또는 온라인 BCrypt 도구로 생성
new BCryptPasswordEncoder().encode("Admin1234!")
// 예시 결과: $2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

기본 계정 정보 (개발용):

| 항목 | 값 |
|------|----|
| lgn_id | `admin` |
| password | `Admin1234!` |
| email | `admin@gw-home.local` |
| role | `ADMIN` |
| nick_nm | `관리자` |

### 2. init-data.sql 구조

```sql
-- [개발용 초기 데이터] 운영 환경 반영 전 반드시 변경
-- BCrypt 해시 생성: new BCryptPasswordEncoder().encode("Admin1234!")

INSERT INTO tb_mbr_acct (lgn_id, pwd, email, role, created_by)
VALUES ('admin', '{BCrypt_HASH}', 'admin@gw-home.local', 'ADMIN', 'system')
ON CONFLICT (lgn_id) DO NOTHING;

INSERT INTO tb_mbr_prfl (mbr_acct_idx, nick_nm, created_by)
SELECT mbr_acct_idx, '관리자', 'system'
FROM tb_mbr_acct
WHERE lgn_id = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM tb_mbr_prfl p WHERE p.mbr_acct_idx = tb_mbr_acct.mbr_acct_idx
  );
```

### 3. 적용 방법

- 개발 환경: DDL 실행 후 `init-data.sql` 수동 실행
- 운영 환경: init SQL 적용 제외 또는 ENV 기반 별도 관리

---

## 리스크 분석

| 리스크 | 심각도 | 대응 방안 |
|--------|--------|-----------|
| 운영 환경 기본 계정 노출 | **높음** | `application-prod.yml`에서 init SQL 실행 제외. 운영 배포 전 계정 비밀번호 변경 필수 |
| UNIQUE 제약 중복 실행 오류 | 낮음 | `ON CONFLICT DO NOTHING` 처리 |
| `tb_mbr_prfl` FK 순서 오류 | 낮음 | account INSERT → profile INSERT 순서 보장 |
| BCrypt rounds 불일치 | 낮음 | `SecurityConfig` 의 `BCryptPasswordEncoder()` 기본값(rounds=10) 동일 사용 |
| RefreshToken 쿠키 미전달 | 낮음 | 프론트 쿠키 도메인/SameSite 설정 확인 |

---

## 대안 검토

| 방안 | 장점 | 단점 | 결론 |
|------|------|------|------|
| init-data.sql 파일 | 명시적, 버전 관리 가능 | 운영 노출 위험 | **채택** |
| Spring `CommandLineRunner` | 프로파일별 제어 용이 | 백엔드 코드 변경 필요 | 운영 분리가 필요해지면 고려 |
| Flyway migration | 마이그레이션 이력 관리 | 현재 Flyway 미도입 | 해당 없음 |

---

## 검증 항목

- [ ] `init-data.sql` 실행 후 admin 계정 조회 성공
- [ ] `POST /api/v1/auth/login` admin 계정 → 200 + JWT 응답
- [ ] 발급 AccessToken으로 `GET /api/v1/admin/summary` → 200 (ADMIN 인가)
- [ ] 프론트 로그인 → `/dashboard` 이동 확인
- [ ] 대시보드 진입 시 401/403 없음
- [ ] 중복 실행 시 오류 없음 확인

---

## 승인 후 작업 순서

1. BCrypt 해시 생성
2. `sql/init/init-data.sql` 작성
3. 로컬 DB에 적용 후 검증 항목 확인
4. 운영 환경 분리 전략 메모 추가
