# 로그인 UX 기능 추가 — 리뷰

> 관련 todo 예정: `todo/auth/03-login-ux-features.md`

---

## 요청 기능 목록

1. 로그인 아이디 저장 (체크박스)
2. 비밀번호 입력 시 Caps Lock 감지 알림
3. 로그인 실패 5회 이상 → 계정 잠금
4. 계정 입력 오류 메시지 출력

---

## 작업 분류: **A / B 분리**

| 구분 | 기능 | 분류 |
|------|------|------|
| A | 아이디 저장 체크박스 | NORMAL — 프론트 단독 |
| A | Caps Lock 감지 | NORMAL — 프론트 단독 |
| A | 오류 메시지 상세화 | NORMAL — 프론트 단독 |
| B | **계정 잠금 (5회 실패)** | **HEAVY** — DB + Backend + Frontend 전체 |

> A와 B는 별도 작업으로 분리. B는 HEAVY 절차(리뷰 → 승인 → 작업) 적용.

---

## Part A — NORMAL (프론트 단독)

### A-1. 로그인 아이디 저장

- `LoginForm.vue` 에 체크박스 추가
- 체크 시 `localStorage` 에 `loginId` 저장
- 페이지 진입 시 저장 값 자동 채움

**변경 파일**
- `gw-home-ui/components/auth/LoginForm.vue`

---

### A-2. Caps Lock 감지

- 비밀번호 input에 `@keyup` 이벤트로 `event.getModifierState('CapsLock')` 감지
- Caps Lock ON 상태일 때 input 하단에 경고 문구 표시
  - 예: "Caps Lock이 켜져 있습니다"

**변경 파일**
- `gw-home-ui/components/auth/LoginForm.vue`

---

### A-3. 오류 메시지 상세화

현재 백엔드 응답 메시지를 그대로 출력 중 (`error.data?.message`).
추가 처리 없이 현재 구조를 유지하되,
계정 잠금(Part B) 완료 후 잠금 메시지를 자연스럽게 연결.

**변경 파일**
- 없음 (현재 구조로 충분, B 완료 후 자연 연결)

---

## Part B — HEAVY (계정 잠금)

### 개요

로그인 실패 5회 이상 시 해당 계정을 잠금 처리.
이후 로그인 시도 시 잠금 메시지 반환.

### 영향 범위

| 레이어 | 파일 | 변경 내용 |
|--------|------|-----------|
| **DB** | `tb_mbr_acct` | `lgn_fail_cnt` (INT), `lck_yn` (BOOLEAN), `lck_at` (TIMESTAMPTZ) 컬럼 추가 |
| **share — VO** | `AcctVo.java` | `lgnFailCnt`, `lckYn`, `lckAt` 필드 추가 |
| **share — ErrorCode** | `ErrorCode.java` | `ACCOUNT_LOCKED(423, "계정이 잠금되었습니다")` 추가 |
| **infra-db — Mapper** | `AccountMapper.java` | `incrementLoginFailCount`, `lockAccount`, `resetLoginFailCount` 메서드 추가 |
| **infra-db — XML** | `AccountMapper.xml` | 위 메서드 SQL 추가 |
| **api — Service** | `AuthService.java` | 잠금 여부 확인 → 실패 시 카운트 증가 → 5회 도달 시 잠금 처리 → 성공 시 카운트 초기화 |
| **Frontend** | `LoginForm.vue` / `login.vue` | 잠금 에러코드(423) 전용 메시지 표시 |

### 처리 흐름 (Backend)

```
login 요청
  ↓
계정 조회 → 없으면 UNAUTHORIZED
  ↓
lck_yn = true → ACCOUNT_LOCKED (423)
  ↓
비밀번호 불일치
  → lgn_fail_cnt + 1
  → fail_cnt >= 5 → lck_yn = true, lck_at = now()
  → UNAUTHORIZED (남은 횟수 메시지 포함)
  ↓
비밀번호 일치
  → lgn_fail_cnt = 0 리셋
  → 토큰 발급
```

### DB 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성 (`ALTER TABLE tb_mbr_acct ADD COLUMN ...`)
- [ ] 기존 데이터 영향 검토 (기존 계정 `lgn_fail_cnt = 0`, `lck_yn = false` DEFAULT)
- [ ] NULL / DEFAULT 정책 확인 (`lgn_fail_cnt DEFAULT 0 NOT NULL`, `lck_yn DEFAULT false NOT NULL`)
- [ ] 인덱스 영향 확인 (잠금 컬럼 인덱스 불필요)
- [ ] 롤백 SQL 작성
- [ ] 운영 반영 순서 정의 (DB 먼저 → Backend 배포)
- [ ] 데이터 마이그레이션 필요 여부 확인 (기존 데이터 무영향)

### 리스크

| 리스크 | 수준 | 대응 |
|--------|------|------|
| `AcctVo` 수정 → share 모듈 영향 | 중간 | 필드 추가만, 기존 사용처 영향 없음 |
| `ErrorCode` 추가 → share 모듈 영향 | 낮음 | 추가만, 기존 코드 무영향 |
| 계정 잠금 후 해제 수단 없음 | 중간 | 어드민 기능 없는 현 시점: `lck_yn` 수동 초기화 또는 별도 잠금 해제 API 향후 추가 예정 명시 |
| 분산 환경에서 카운트 동시성 | 낮음 | 단일 서버 운영 가정, DB 레벨 UPDATE로 처리 |

---

## 정리

- **Part A (NORMAL)**: `LoginForm.vue` 단독 수정 — 즉시 진행 가능
- **Part B (HEAVY)**: DB 스키마 + share 모듈 + Backend + Frontend — 승인 후 진행
- A와 B는 **독립적으로 분리 가능** (B 없이 A만 먼저 진행 가능)
