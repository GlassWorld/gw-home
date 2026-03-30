# Review: Vault 자격증명 복수 검색 지원

> **연관 문서**: [review/vault/01-credential-manager.md](./01-credential-manager.md) / [review/vault/04-vault-ui-improvements.md](./04-vault-ui-improvements.md) / work 미생성

---

## 작업 개요

Vault 자격증명 목록 검색에서 단일 키워드만 부분 일치시키는 현재 방식 대신,
복수 검색어를 함께 입력해 원하는 자격증명을 더 빠르게 찾을 수 있도록 검색 UX와 조회 규칙을 확장한다.

예시:
- `aws prod`
- `admin otp`
- `github personal token`

---

## 변경 목적

- 제목 하나만 기억나지 않아도 여러 단서를 함께 입력해 검색할 수 있게 한다.
- 자격증명 수가 늘어날수록 단일 키워드 검색의 한계를 줄인다.
- UI는 단순하게 유지하면서 실제 탐색 성공률을 높인다.

---

## 현재 구조와 한계

현재 Vault 목록 조회는 `keyword` 1개를 받아 제목/메모 기준 `ILIKE` 검색하는 구조에 가깝다.
이 방식은 `aws prod`, `github admin`, `otp personal` 처럼 여러 단어를 조합해 찾고 싶은 경우 원하는 결과를 놓치기 쉽다.

특히 메모 중심 사용이 많은 Vault 특성상, 사용자는 제목보다 여러 단서를 섞어 입력하는 경우가 자연스럽다.

---

## 권장 방향

### 1. 입력은 하나, 내부적으로는 토큰 분리

- UI는 기존 검색 입력 1개를 유지한다.
- 입력값을 공백 기준으로 분리해 복수 토큰으로 해석한다.
- 빈 토큰은 제거한다.

예:
- `"aws   prod"` → `["aws", "prod"]`

### 2. 검색 의미는 기본적으로 AND

- 각 토큰이 제목 또는 메모 중 어디엔가 포함되면 매칭으로 본다.
- 전체 결과는 "모든 토큰을 만족"하는 AND 조건이 기본값이 적절하다.

이유:
- Vault 검색은 후보를 넓히는 것보다 빠르게 좁히는 쪽이 실제 사용성과 맞다.

### 3. 검색 대상 필드는 기존 범위를 유지

- 1차는 `title`, `loginId`, `memo` 정도를 우선 검색 대상으로 둔다.
- 과거 문서에서 제거된 `description`, `url` 은 대상에 다시 넣지 않는다.

### 4. 프론트 debounce 와 검색어 trim 정리

- 입력 중 과도한 호출을 줄이기 위해 기존 debounce 흐름이 있다면 유지한다.
- 앞뒤 공백과 중복 공백은 프론트에서 1차 정리해 전송한다.

---

## 예상 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| Backend - api | `GET /api/v1/vault/credentials` 검색 파라미터 해석 규칙 보완 |
| Backend - service | 검색어 토큰 분리 및 Mapper 전달 구조 변경 가능 |
| Backend - infra-db | `VaultMapper.xml` 동적 SQL을 복수 토큰 AND 검색으로 확장 |
| Frontend - vault | `/vault` 검색 입력 placeholder, trim, debounce 동작 점검 |

---

## 리스크 분석

| 리스크 | 대응 |
|--------|------|
| 토큰마다 `ILIKE` 조건이 늘어 SQL이 복잡해질 수 있음 | 1차는 토큰 수를 합리적 범위로 두고 동적 SQL로 처리 |
| OR 검색을 기대한 사용자가 결과가 적다고 느낄 수 있음 | placeholder 또는 도움말에 복수 단어 검색 의미를 가볍게 안내 |
| 공백만 입력한 경우 전체 조회와 충돌 가능 | trim 후 빈 문자열이면 검색 미적용으로 처리 |

---

## 작업 분류

**NORMAL**

사유:
- DB 스키마 변경 없음
- vault 도메인 내부의 검색 로직/화면 문구 조정 중심
- Backend + Frontend 동시 수정 가능성은 있으나 API 스펙 구조 자체 변경은 아님

---

## 예상 구현 파일 범위

- `gw-home-api/src/main/java/com/gw/api/service/vault/VaultService.java`
- `gw-home-infra-db/src/main/resources/mapper/vault/VaultMapper.xml`
- `gw-home-ui/composables/useVaultApi.ts`
- `gw-home-ui/pages/vault/index.vue`

---

## 권장 다음 단계

1. 기존 검색 파이프라인이 제목/로그인ID/메모 중 어디를 대상으로 하는지 먼저 확인
2. 토큰 분리 규칙과 AND 검색 정책을 todo 문서로 고정
3. Backend SQL과 Frontend 검색 UX를 함께 맞춘다
