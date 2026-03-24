# 계정 잠금 기능 (Part B — Full-stack) — Todo

> 리뷰 문서: [`review/auth/03-login-ux-features.md`](../../review/auth/03-login-ux-features.md)
> 연관 todo: [`todo/auth/03-login-ux-frontend.md`](03-login-ux-frontend.md) (Part A)
> 작업 분류: **HEAVY**

⚠️ HEAVY 작업 — 승인 후 진행

---

## 작업 범위

| 파일 | 변경 내용 |
|------|-----------|
| `tb_mbr_acct` (DDL) | `lgn_fail_cnt`, `lck_yn`, `lck_at` 컬럼 추가 |
| `AcctVo.java` | 잠금 관련 필드 추가 |
| `ErrorCode.java` | `ACCOUNT_LOCKED(423)` 추가 |
| `AccountMapper.java` | 카운트/잠금/초기화 메서드 추가 |
| `AccountMapper.xml` | 위 메서드 SQL 추가 |
| `AuthService.java` | 잠금 확인 → 카운트 → 잠금 → 초기화 로직 추가 |
| `LoginForm.vue` | 423 전용 잠금 메시지 표시 |

---

## DB 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성
  ```sql
  ALTER TABLE tb_mbr_acct
    ADD COLUMN lgn_fail_cnt INT NOT NULL DEFAULT 0,
    ADD COLUMN lck_yn BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN lck_at TIMESTAMPTZ;
  ```
- [ ] 기존 데이터 영향 검토 (DEFAULT 값으로 무영향 확인)
- [ ] NULL / DEFAULT 정책 확인 (`lgn_fail_cnt DEFAULT 0`, `lck_yn DEFAULT false`, `lck_at NULL 허용`)
- [ ] 인덱스 영향 확인 (추가 인덱스 불필요)
- [ ] 롤백 SQL 작성
  ```sql
  ALTER TABLE tb_mbr_acct
    DROP COLUMN lgn_fail_cnt,
    DROP COLUMN lck_yn,
    DROP COLUMN lck_at;
  ```
- [ ] 운영 반영 순서 정의 (DB 먼저 → Backend 배포)
- [ ] 데이터 마이그레이션 필요 여부 확인

---

## Todo

### 1. DB — `tb_mbr_acct` 컬럼 추가
- [ ] DDL ALTER 스크립트 작성 및 적용

### 2. share — `AcctVo.java` 필드 추가
- [ ] `lgnFailCnt` (int), `lckYn` (boolean), `lckAt` (OffsetDateTime) 필드 추가

### 3. share — `ErrorCode.java` 추가
- [ ] `ACCOUNT_LOCKED(423, "계정이 잠금되었습니다")` 추가

### 4. infra-db — `AccountMapper.java` 메서드 추가
- [ ] `incrementLoginFailCount(@Param("idx") Long idx)` — 실패 카운트 +1
- [ ] `lockAccount(@Param("idx") Long idx)` — `lck_yn = true`, `lck_at = now()`
- [ ] `resetLoginFailCount(@Param("idx") Long idx)` — `lgn_fail_cnt = 0`

### 5. infra-db — `AccountMapper.xml` SQL 추가
- [ ] `incrementLoginFailCount` UPDATE SQL
- [ ] `lockAccount` UPDATE SQL
- [ ] `resetLoginFailCount` UPDATE SQL

### 6. api — `AuthService.java` 로직 수정
- [ ] 계정 조회 후 `lckYn == true` 이면 `ACCOUNT_LOCKED` throw
- [ ] 비밀번호 불일치 시:
  - [ ] `incrementLoginFailCount` 호출
  - [ ] 실패 횟수 >= 5 이면 `lockAccount` 호출
  - [ ] 남은 횟수 포함 메시지 반환 (`"로그인 정보가 올바르지 않습니다. (잔여 {n}회)"`)
- [ ] 비밀번호 일치 시 `resetLoginFailCount` 호출

### 7. Frontend — `LoginForm.vue` 잠금 메시지 처리
- [ ] HTTP 423 응답 시 전용 메시지 표시
  - "계정이 잠금되었습니다. 관리자에게 문의하세요."
