# Review: Vault 자격증명 필드 통합 (dsc + url + memo → memo)

> **연관 문서**: [review/vault/01-credential-manager.md](./01-credential-manager.md) / todo 미생성 / work 미생성

---

## 작업 개요

현재 자격증명에 분리되어 있는 `dsc`(설명), `url`, `memo` 세 필드를 `memo` 단일 필드로 통합한다.
사용자는 하나의 텍스트 영역에 설명, URL, 메모를 자유롭게 입력한다.

---

## 변경 목적

- 세 필드의 역할 경계가 모호하여 사용자 혼란 유발
- 입력 폼 단순화 — 항목 수 감소로 UX 개선
- 데이터 모델 경량화

---

## 현재 → 변경 후

| 현재 필드 | 변경 후 | 비고 |
|-----------|---------|------|
| `dsc` (설명) | 제거 | |
| `url` (URL) | 제거 | |
| `memo` (메모) | **유지** — 통합 필드로 사용 | 카드 본문 표시용으로도 활용 |

> `memo` 를 통합 필드로 채택.
> `dsc`, `url` 컬럼은 DB에서 DROP.

---

## 이미 구현된 상태

`tb_vlt_crd` 테이블 및 관련 코드가 이미 반영되어 있으므로 **ALTER TABLE** 로 변경해야 한다.

---

## DB 변경

```sql
-- 변경
ALTER TABLE tb_vlt_crd DROP COLUMN dsc;
ALTER TABLE tb_vlt_crd DROP COLUMN url;

-- 롤백
ALTER TABLE tb_vlt_crd ADD COLUMN dsc TEXT;
ALTER TABLE tb_vlt_crd ADD COLUMN url TEXT;
```

> **기존 데이터 처리**: `dsc`, `url` 컬럼 DROP 시 해당 데이터 유실.
> 현재 운영 데이터가 없는(초기) 상태 기준으로 진행.

---

## 영향 범위 분석

| 파일 | 변경 유형 | 내용 |
|------|-----------|------|
| `tb_vlt_crd` (DB) | ALTER — 컬럼 DROP | `dsc`, `url` 제거 |
| `ddl/vault/tb_vlt_crd.sql` | 수정 | DDL에서 `dsc`, `url` 제거 |
| `CrdVo.java` | 수정 | `dsc`, `url` 필드 제거 |
| `VaultMapper.xml` | 수정 | INSERT/UPDATE/SELECT 쿼리에서 `dsc`, `url` 제거 |
| `CredentialResponse.java` | 수정 | `dsc`, `url` 필드 제거 |
| `SaveCredentialRequest.java` | 수정 | `dsc`, `url` 필드 제거 |
| `types/vault.ts` | 수정 | `Credential`, `SaveCredentialPayload`에서 `description`, `url` 제거 |
| `CredentialFormModal.vue` | 수정 | 설명/URL 입력 필드 제거, memo 입력란 유지 |
| `CredentialDetailModal.vue` | 수정 | 설명/URL 표시 제거, memo 표시 유지 |
| `CredentialCard.vue` | 수정 | 카드 본문을 `description` → `memo` 로 변경 |

---

## 카드 표시 변경

- 현재: 카드 본문 → `description`
- 변경 후: 카드 본문 → `memo` (2줄 말줄임 유지)

---

## 리스크 분석

| 리스크 | 수준 | 대응 |
|--------|------|------|
| `dsc`, `url` 컬럼 DROP 시 데이터 유실 | 낮 | 초기 상태 기준, 운영 데이터 없음 확인 후 진행 |
| `memo` 단일 필드로 URL 표현 시 클릭 불가 | 낮 | 요구사항상 URL은 단순 텍스트로 표기 — 별도 처리 없음 |

---

## 작업 분류

**HEAVY** — 기존 구현 테이블 스키마 변경 (컬럼 DROP) + 전 레이어 연쇄 수정

---

## 구현 예상 파일 목록

### Backend
- `gw-home-infra-db/src/main/resources/sql/ddl/vault/tb_vlt_crd.sql`
- `gw-home-share/.../CrdVo.java`
- `gw-home-infra-db/.../VaultMapper.xml`
- `gw-home-api/.../CredentialResponse.java`
- `gw-home-api/.../SaveCredentialRequest.java`

### Frontend
- `types/vault.ts`
- `components/vault/CredentialFormModal.vue`
- `components/vault/CredentialDetailModal.vue`
- `components/vault/CredentialCard.vue`
