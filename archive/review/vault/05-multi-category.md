# Review: Vault 자격증명 복수 카테고리 지원

> **연관 문서**: [review/vault/01-credential-manager.md](./01-credential-manager.md) / [review/vault/02-category.md](./02-category.md) / work 미생성

---

## 작업 개요

현재 개인 자격증명(`vault credential`)은 카테고리 1개만 연결할 수 있다.
이를 카테고리 다중 선택 구조로 변경하여, 하나의 자격증명이 여러 카테고리에 동시에 속할 수 있도록 한다.

---

## 변경 목적

- 하나의 자격증명을 업무/개인/공용 등 여러 기준으로 동시에 분류할 수 있게 한다.
- 단일 카테고리 제약으로 인해 생기는 중복 등록 또는 분류 타협을 줄인다.
- Vault 목록 필터링을 유지하면서도 자격증명 분류 유연성을 높인다.

---

## 현황 분석

현재 구조는 단일 카테고리 전제에 강하게 묶여 있다.

| 항목 | 현재 상태 |
|------|-----------|
| DB | `tb_vlt_crd.vlt_cat_idx` 단일 FK |
| Backend 요청 | `SaveCredentialRequest.categoryUuid` 단일 값 |
| Backend 응답 | `CredentialResponse.categoryUuid`, `categoryName`, `categoryColor` 단일 값 |
| Frontend 타입 | `Credential.categoryUuid`, `categoryName`, `categoryColor` 단일 값 |
| Frontend 입력 UI | `CredentialFormModal` 단일 선택 |
| 목록 필터 | `/vault` 에서 `categoryUuid` 1개 기준 필터 |

즉, 다중 카테고리 지원은 단순 UI 수정이 아니라 DB 모델과 API 계약 자체를 바꾸는 작업이다.

---

## 권장 구조

### 1. 조인 테이블 도입

기존 `tb_vlt_crd.vlt_cat_idx` 단일 FK 대신, 자격증명-카테고리 매핑 테이블을 둔다.

예상 구조:

| 테이블 | 설명 |
|--------|------|
| `tb_vlt_crd` | 자격증명 본문 |
| `tb_vlt_cat` | 카테고리 마스터 |
| `tb_vlt_crd_cat` | 자격증명-카테고리 다대다 매핑 |

예상 컬럼:

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `vlt_crd_cat_idx` | BIGSERIAL PK | 내부 PK |
| `tb_vlt_crd_idx` | BIGINT NOT NULL | 자격증명 PK |
| `tb_vlt_cat_idx` | BIGINT NOT NULL | 카테고리 PK |
| `created_at` | TIMESTAMPTZ | 생성 일시 |
| UNIQUE (`tb_vlt_crd_idx`, `tb_vlt_cat_idx`) | | 중복 매핑 방지 |

### 2. 기존 컬럼 처리

`tb_vlt_crd.vlt_cat_idx` 는 최종적으로 제거 대상이다.

다만 구현 순서는 아래 두 안 중 하나를 선택해야 한다.

| 안 | 방식 | 장점 | 단점 |
|----|------|------|------|
| A | 조인 테이블 추가 후 기존 `vlt_cat_idx` 유지, 앱 전환 후 제거 | 롤백과 점진 배포가 쉬움 | 이행 단계가 길어짐 |
| B | 한 번에 조인 테이블 전환 + `vlt_cat_idx` 제거 | 구조가 깔끔함 | 배포 실패 시 복구 부담이 큼 |

기본 권장안: **A**

---

## API 변경 예상

### Vault Credential API

| Method | Path | 변경 내용 |
|--------|------|-----------|
| GET | `/api/v1/vault/credentials` | 응답에 카테고리 목록 포함 필요 |
| GET | `/api/v1/vault/credentials/{uuid}` | 응답에 카테고리 목록 포함 필요 |
| POST | `/api/v1/vault/credentials` | `categoryUuid` → `categoryUuids` 로 변경 |
| PUT | `/api/v1/vault/credentials/{uuid}` | `categoryUuid` → `categoryUuids` 로 변경 |

### 응답 구조

현재 단일 필드:
- `categoryUuid`
- `categoryName`
- `categoryColor`

변경 권장:
- `categories: VaultCategorySummary[]`

예상 응답 요소:

| 필드 | 설명 |
|------|------|
| `categoryUuid` | 카테고리 UUID |
| `name` | 카테고리명 |
| `color` | 카테고리 색상 |

---

## 프론트엔드 변경 예상

### 입력 UI

- `CredentialFormModal.vue`
  - 단일 선택 드롭다운 → 복수 선택 UI
  - 검색 가능 멀티 셀렉트 또는 체크리스트 UI 필요

### 목록/상세 UI

- `CredentialCard.vue`
  - 단일 badge → 다중 badge 렌더링
- `CredentialDetailModal.vue`
  - 다중 카테고리 badge 렌더링
- `pages/vault/index.vue`
  - 필터는 기존처럼 단일 카테고리 기준 유지 가능
  - 단, 목록 응답은 다중 카테고리 표시를 지원해야 함

### 타입 변경

- `types/vault.ts`
  - `Credential.categoryUuid/categoryName/categoryColor` 제거 또는 deprecated 처리
  - `categories: VaultCategory[]` 또는 요약 타입 배열로 전환
  - `SaveCredentialPayload.categoryUuid` → `categoryUuids?: string[]`

---

## 영향 범위 분석

| 항목 | 영향 여부 | 내용 |
|------|-----------|------|
| DB 스키마 | ✅ | 신규 조인 테이블 + 기존 `tb_vlt_crd` 관계 구조 변경 |
| vault 도메인 Backend | ✅ | Mapper/Service/DTO/응답 구조 전면 수정 |
| vault 프론트 | ✅ | 타입/폼/카드/상세/목록 렌더 수정 |
| admin 도메인 | △ | 카테고리 관리 자체는 유지, 삭제 시 연결 처리 규칙 재검토 필요 |
| share 모듈 | ✅ | `VO/JVO/DTO` 구조 영향 가능 |
| API 스펙 | ✅ | 요청/응답 계약 변경 |

---

## 주요 리스크

| 리스크 | 수준 | 대응 |
|--------|------|------|
| 기존 데이터 마이그레이션 누락 | 높음 | `vlt_cat_idx` 값을 `tb_vlt_crd_cat` 로 이관하는 SQL 필요 |
| 단일 응답 필드 사용 프론트 회귀 | 높음 | 타입과 화면을 같은 배포 단위로 전환 |
| 카테고리 삭제 시 매핑 정리 규칙 모호 | 중 | FK cascade 또는 소프트 삭제 기준 명확화 필요 |
| 목록 조회 중복 행 발생 | 중 | 조인 시 집계 방식(JSON/array_agg 또는 2단계 조회) 설계 필요 |
| 검색/필터 성능 저하 | 중 | 매핑 테이블 인덱스 설계 필요 |

---

## 대안 검토

| 방안 | 설명 | 결론 |
|------|------|------|
| `tb_vlt_crd` 에 category 컬럼 여러 개 추가 | 고정 개수만 가능, 확장성 낮음 | 부적합 |
| 문자열/배열 컬럼에 UUID 목록 저장 | 정합성/FK 관리 어려움 | 부적합 |
| 조인 테이블 도입 | 정규화, 확장성, 무결성 확보 | **채택 권장** |

---

## 예상 구현 파일 범위

### DB
- `gw-home-infra-db/src/main/resources/sql/ddl/vault/*`
- `docs/all-ddl.sql`

### Backend
- `gw-home-api/src/main/java/com/gw/api/dto/vault/*`
- `gw-home-api/src/main/java/com/gw/api/service/vault/VaultService.java`
- `gw-home-infra-db/src/main/resources/mapper/vault/VaultMapper.xml`
- `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/vault/*`
- `gw-home-share/src/main/java/com/gw/share/vo/vault/*`
- 필요 시 `JVO` 신규 추가

### Frontend
- `gw-home-ui/types/vault.ts`
- `gw-home-ui/composables/useVaultApi.ts`
- `gw-home-ui/components/vault/CredentialFormModal.vue`
- `gw-home-ui/components/vault/CredentialCard.vue`
- `gw-home-ui/components/vault/CredentialDetailModal.vue`
- `gw-home-ui/pages/vault/index.vue`

---

## 작업 분류

**HEAVY**

사유:
- DB 스키마 변경
- 기존 데이터 이관 필요
- API 계약 변경
- Backend + Frontend 동시 수정

---

## 선결 결정 사항

1. 기존 `categoryUuid` 필드는 즉시 제거할지, 이행 기간 동안 병행할지
2. Vault 목록 필터는 계속 단일 카테고리 선택으로 유지할지
3. 응답 구조를 `categories[]` 로 완전 전환할지, 단일 대표 카테고리 필드를 병행 제공할지
4. 카테고리 삭제 시 매핑 레코드 처리 규칙을 어떻게 가져갈지

---

## 권장 다음 단계

- review 승인 후 todo 문서 생성
- DDL/마이그레이션 전략 먼저 확정
- 이후 Backend → Frontend 순으로 작업 계획 수립
