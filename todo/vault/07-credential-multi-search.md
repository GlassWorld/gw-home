# Todo: Vault 자격증명 복수 검색 지원

> 연관 문서: [review/vault/06-credential-multi-search.md](../../review/vault/06-credential-multi-search.md)
> 작업 분류: **NORMAL**
> 도메인: vault

---

## 작업 범위

- Vault 목록 검색 입력 1개를 유지하면서 복수 검색어를 지원한다.
- 공백 기준으로 검색어를 분리하고, 각 토큰을 AND 조건으로 적용한다.
- 검색 대상은 `title`, `loginId`, `memo` 범위를 우선 유지한다.
- DB 스키마나 API 경로 변경 없이 기존 목록 조회 계약 안에서 처리한다.

### 범위 제외

- OR 검색 옵션 추가
- 검색 연산자 문법 도입
- 전문검색 인덱스 추가
- 카테고리 필터 구조 변경

---

## 구현 체크리스트

### 1. 검색 규칙 확정

- [x] 입력값 trim 규칙 확정
- [x] 연속 공백 제거 및 토큰 분리 규칙 확정
- [x] 빈 토큰 제거 후 0개일 때는 전체 조회로 처리하는지 확정
- [x] 기본 검색 의미를 AND 로 고정

### 2. Backend — Service / Mapper

- [x] `VaultService` 에서 keyword 토큰 분리 책임 위치 확인
- [x] Mapper 에 전달할 검색 파라미터 구조 정리
- [x] `VaultMapper.xml` 에서 토큰별 AND 조건 동적 SQL 작성
- [x] 각 토큰이 `title`, `login_id`, `memo` 중 하나에 매칭되도록 조건 구성

### 3. Frontend — 검색 UX

- [x] `/vault` 검색 입력값 trim 및 중복 공백 정리 여부 확인
- [x] 기존 debounce 또는 검색 트리거 흐름 유지
- [x] placeholder 또는 helper text 에 복수 검색 가능성 반영 여부 검토
- [x] 검색어 초기화 시 전체 목록 복귀 흐름 확인

### 4. 검증

- [x] `aws prod` 처럼 2개 키워드 입력 시 의도한 결과만 조회되는지 확인
- [x] `admin otp personal` 처럼 3개 이상 토큰도 정상 동작하는지 확인
- [x] 공백만 입력한 경우 전체 조회로 동작하는지 확인
- [x] 기존 단일 키워드 검색이 회귀하지 않는지 확인

---

## 작업 순서

1. 검색어 토큰 규칙을 먼저 고정한다.
2. Backend 동적 SQL 을 복수 토큰 AND 검색으로 확장한다.
3. Frontend 검색 입력의 trim / 안내 문구를 정리한다.
4. 단일 검색과 복수 검색을 함께 회귀 확인한다.

---

## 완료 기준

- 사용자가 검색창 하나로 복수 검색어를 입력할 수 있다.
- 각 검색어는 AND 조건으로 적용되어 결과가 적절히 좁혀진다.
- 기존 단일 검색 UX는 유지되고, 공백 입력도 안정적으로 처리된다.
