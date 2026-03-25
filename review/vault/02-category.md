# Review: Vault 카테고리 기능 추가

> **연관 문서**: [review/vault/01-credential-manager.md](./01-credential-manager.md) / [todo/vault/02-category.md](../../todo/vault/02-category.md) / work 미생성

---

## 작업 개요

자격증명에 카테고리를 부여하여 분류 관리할 수 있도록 한다.
카테고리는 관리자 메뉴에서 등록/수정/삭제하고,
Vault 목록 페이지 상단에 카테고리 탭/버튼을 나열하여 클릭 시 해당 카테고리로 자동 필터링한다.

---

## 변경 목적

- 자격증명 수가 많아질수록 카테고리별 분류 없이는 탐색이 어려워짐
- 관리자가 카테고리를 중앙 관리하여 일관된 분류 체계 유지
- 사용자는 카테고리 탭 클릭만으로 즉시 필터링 — 검색창 입력 없이도 빠른 탐색

---

## 신규 테이블

### `tb_vlt_cat` (Vault Category)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `tb_vlt_cat_idx` | BIGSERIAL PK | 내부 PK |
| `tb_vlt_cat_uuid` | UUID | 외부 노출 식별자 |
| `nm` | VARCHAR(100) NOT NULL | 카테고리명 |
| `dsc` | TEXT | 설명 |
| `sort_ord` | INT DEFAULT 0 | 정렬 순서 |
| `created_by` | VARCHAR(100) | 생성자 |
| `created_at` | TIMESTAMPTZ | 생성일시 |
| `updated_by` | VARCHAR(100) | 수정자 |
| `updated_at` | TIMESTAMPTZ | 수정일시 |
| `del_at` | TIMESTAMPTZ | 소프트 삭제 |

---

## 기존 테이블 변경

### `tb_vlt_crd` — 컬럼 추가

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `vlt_cat_idx` | BIGINT NULL | FK → `tb_vlt_cat.tb_vlt_cat_idx` (선택 항목) |

> 카테고리는 필수가 아니며, NULL 허용 (미분류 상태 허용).

---

## API 목록

### 관리자 카테고리 관리 API (`/api/v1/admin/vault-categories`)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/admin/vault-categories` | 카테고리 목록 조회 |
| POST | `/api/v1/admin/vault-categories` | 카테고리 등록 |
| PUT | `/api/v1/admin/vault-categories/{uuid}` | 카테고리 수정 |
| DELETE | `/api/v1/admin/vault-categories/{uuid}` | 카테고리 삭제 |

> 기존 `AdminController` 또는 신규 `AdminVaultCategoryController`로 구현.
> `@PreAuthorize("hasRole('ADMIN')")` 적용.

### 사용자용 카테고리 조회 API
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/vault/categories` | 카테고리 목록 조회 (공개 — 인증 사용자용) |

> 카테고리 목록은 전체 공개 (사용자별 분리 없음).

### 기존 Vault Credential API 변경
| Method | Path | 변경 내용 |
|--------|------|-----------|
| GET | `/api/v1/vault/credentials` | query param `categoryUuid` 추가 |
| POST | `/api/v1/vault/credentials` | 요청 body `categoryUuid` 추가 (선택) |
| PUT | `/api/v1/vault/credentials/{uuid}` | 요청 body `categoryUuid` 추가 (선택) |

---

## 프론트엔드 화면 구성

### Vault 목록 페이지 (`/vault`) 변경
- 검색 폼 하단(또는 상단)에 카테고리 필터 탭 추가
  - "전체" 탭 + 카테고리별 탭 나열
  - 탭 클릭 시 해당 `categoryUuid`로 자동 필터링 (목록 즉시 갱신)
  - 현재 선택 카테고리 강조 표시
- 등록/수정 폼 모달에 카테고리 선택 드롭다운 추가

### 관리자 카테고리 관리 페이지 (신규)
- 경로: `/admin/vault-categories`
- 기능: 카테고리 목록 테이블 + 등록/수정/삭제
- admin role 체크 middleware 적용

---

## 영향 범위 분석

| 항목 | 영향 여부 | 내용 |
|------|-----------|------|
| 신규 DB 테이블 | ✅ | `tb_vlt_cat` 생성 |
| 기존 테이블 변경 | ✅ | `tb_vlt_crd`에 `vlt_cat_idx` 컬럼 추가 |
| vault 도메인 API | ✅ | 목록 조회 파라미터 + 등록/수정 body 변경 |
| admin 도메인 | ✅ | 카테고리 관리 API 추가 |
| 신규 프론트 페이지 | ✅ | `/admin/vault-categories` |
| 기존 vault 프론트 | ✅ | 카테고리 탭 + 폼 드롭다운 추가 |
| 공통 모듈 변경 | ❌ | 없음 |
| 인증/보안 구조 | ❌ | 기존 `hasRole('ADMIN')` 재사용 |

---

## 리스크 분석

| 리스크 | 수준 | 대응 |
|--------|------|------|
| `tb_vlt_crd` 기존 데이터 처리 | 낮 | `vlt_cat_idx` NULL 허용으로 기존 데이터 영향 없음 |
| 카테고리 삭제 시 연결된 자격증명 처리 | 중 | 삭제 시 `tb_vlt_crd.vlt_cat_idx` NULL 처리 (ON DELETE SET NULL) 또는 삭제 불가 처리 선택 필요 |
| 관리자 전용 페이지 접근 제어 | 중 | 프론트 admin role middleware + 백엔드 `@PreAuthorize` 이중 검증 |

> **결정 필요**: 카테고리 삭제 시 연결된 자격증명 처리 방식
> - 안 A: `ON DELETE SET NULL` — 카테고리 삭제 시 자동으로 미분류 처리
> - 안 B: 연결된 자격증명이 있으면 삭제 불가 (에러 반환)
> → 기본 안: **안 A** (ON DELETE SET NULL) 채택. 사용자 데이터 유실 없음.

---

## 마이그레이션 체크리스트

- [ ] DDL: `CREATE TABLE tb_vlt_cat` 작성
- [ ] DDL: `ALTER TABLE tb_vlt_crd ADD COLUMN vlt_cat_idx BIGINT REFERENCES tb_vlt_cat(tb_vlt_cat_idx) ON DELETE SET NULL`
- [ ] 기존 `tb_vlt_crd` 데이터 영향 검토 (NULL 허용 — 영향 없음)
- [ ] 인덱스 추가: `tb_vlt_crd.vlt_cat_idx`
- [ ] 롤백 SQL 작성
- [ ] 운영 반영 순서: `tb_vlt_cat` 생성 → `tb_vlt_crd` 컬럼 추가 → 앱 배포

---

## 작업 분류

**HEAVY** — 기존 테이블 스키마 변경 + 신규 테이블 + 복수 도메인(vault + admin) 동시 영향

---

## 구현 예상 파일 목록

### Backend
- `AdminVaultCategoryController.java` (또는 AdminController 확장)
- `VaultCategoryService.java`
- `VaultCategoryMapper.java`
- `VaultCategoryMapper.xml`
- `CatVo.java` (share)
- `VaultCategoryResponse.java`
- `SaveVaultCategoryRequest.java`
- `ddl/vault_category.sql`

### Frontend
- `pages/admin/vault-categories/index.vue`
- `composables/useVaultCategoryApi.ts`
- `types/vault.ts` 확장 (`VaultCategory` 타입, `Credential.categoryUuid` 추가)
- `pages/vault/index.vue` 수정 — 카테고리 탭 + 필터 파라미터
- `components/vault/CredentialFormModal.vue` 수정 — 카테고리 드롭다운
