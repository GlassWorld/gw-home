# Todo: 문서와 집계 DDL 정합성 정리

> 연관 문서: [review/work/03-overall-refactor-check.md](../../review/work/03-overall-refactor-check.md)
> 작업 레벨: **HEAVY**
> 레이어: **Docs + Backend + Frontend**

---

## 작업 범위

- 문서와 실제 구현이 어긋난 항목을 현재 코드 기준으로 재정렬
- `docs/all-ddl.sql`에 누락된 `work` 도메인 DDL 반영 여부 정리
- 프론트 페이지 구조, API 계약 문서, DB 문서를 실제 구현 기준으로 갱신

### 현재 요청 기준 우선 범위

- `docs/backend/database.md`
- `docs/all-ddl.sql`
- `docs/frontend/pages.md`
- `docs/common/api-contract.md`
- `docs/frontend/auth-flow.md`

### 현재 요청 기준 범위 제외

- 인증 방식 자체 변경
- 백엔드 API 스펙 변경
- 프론트 UX 변경
- DB 스키마 신규 변경

---

## 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| Docs - backend | 집계 DDL 설명과 실제 스키마 정합성 정리 |
| Docs - common | `ApiResponse`, 페이지 번호 정책 문서 정리 |
| Docs - frontend | 페이지 구조, 인증 흐름 문서 정리 |
| DDL docs | `work` 도메인 누락분 반영 여부 확정 |

---

## 설계 방향

### 1. 문서는 구현과 다르면 구현 기준으로 먼저 맞춘다

- 현재 작업은 기능 변경이 아니라 기준 문서 정합성 회복이 목적이다.
- 문서가 미래 설계를 설명하는 경우라도, 현재 구현과 다른 내용은 별도 메모로 분리한다.

### 2. `all-ddl.sql`의 역할을 먼저 확정한다

- 전체 재생성 스크립트로 유지할지
- 참고용 집계 파일로 낮출지

역할을 확정한 뒤 문서와 파일을 함께 맞춘다.

### 3. 프론트 계약 문서는 실제 타입과 응답 정책에 맞춘다

- `ApiResponse<T>`의 `data` nullable 여부
- 페이지 시작 번호
- 초기 진입 라우팅

위 항목은 프론트 타입과 백엔드 응답 기준을 함께 확인해 맞춘다.

---

## 구현 체크리스트

### 1. DB 문서 정리

- [x] `docs/backend/database.md`의 `all-ddl.sql` 설명 검토
- [x] `docs/all-ddl.sql`에 `work` 도메인 누락 범위 확인
- [x] `all-ddl.sql` 유지 정책 확정
- [x] 정책에 맞게 문서와 파일 정리

### 2. 프론트 문서 정리

- [x] `docs/frontend/pages.md`와 실제 페이지 구조 대조
- [x] 루트 진입 경로 설명 정리
- [x] 존재하지 않는 페이지 경로 제거 또는 보정

### 3. API 계약 문서 정리

- [x] `ApiResponse<T>`의 `data` nullable 여부 확인
- [x] 페이지 번호 시작 기준 확인
- [x] 문서 예시 응답을 실제 규약 기준으로 보정

### 4. 인증 문서 정리

- [x] 현재 refresh token 처리 방식 반영 여부 검토
- [x] 문서에 남은 `HttpOnly` 쿠키 설명 정리
- [x] 현재 보안 모델 설명을 실제 구현 기준으로 맞춤

### 5. 검증

- [x] 문서 간 상호 참조 링크 확인
- [x] 실제 코드와 문서 샘플 불일치 재점검
- [x] 후속 기능 변경이 필요한 항목은 별도 todo로 분리

---

## 진행 결과

- `backend-rules`, `architecture` 문서에 mapper alias 기준과 `convert`/`share.util` 리팩토링 패턴을 반영했다.
- `pages`, `auth-flow`, `api-contract`, `database` 문서를 실제 라우트/응답/인증 흐름 기준으로 정리했다.
- `docs/all-ddl.sql`에 누락된 `work` 도메인 집계 DDL을 반영했다.

---

## 후보 대상 파일 메모

- `docs/backend/database.md`
- `docs/all-ddl.sql`
- `docs/frontend/pages.md`
- `docs/common/api-contract.md`
- `docs/frontend/auth-flow.md`
- `gw-home-ui/types/api/common.ts`
- `gw-home-api/src/main/java/com/gw/infra/db/support/PageSortSupport.java`
