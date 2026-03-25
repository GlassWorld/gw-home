# Todo: Vault 카테고리 기능 추가

> **연관 문서**: [review/vault/02-category.md](../../review/vault/02-category.md)
> **작업 분류**: HEAVY
> **도메인**: vault + admin

---

## Phase 1 — DB

- [x] **1-1** DDL: `tb_vlt_cat` 테이블 생성
  - PK: `tb_vlt_cat_idx` BIGSERIAL
  - UUID: `tb_vlt_cat_uuid` UUID DEFAULT gen_random_uuid()
  - 컬럼: `nm`, `dsc`, `sort_ord`
  - 감사 컬럼: `created_by`, `created_at`, `updated_by`, `updated_at`, `del_at`
  - 인덱스: `del_at`
- [x] **1-2** DDL: `tb_vlt_crd` 컬럼 추가
  - `vlt_cat_idx BIGINT NULL REFERENCES tb_vlt_cat(tb_vlt_cat_idx) ON DELETE SET NULL`
  - 인덱스: `tb_vlt_crd(vlt_cat_idx)`
- [x] **1-3** 롤백 SQL 작성
  - `ALTER TABLE tb_vlt_crd DROP COLUMN vlt_cat_idx`
  - `DROP TABLE tb_vlt_cat`

---

## Phase 2 — Backend Share (VO)

- [x] **2-1** `CatVo.java` 생성 (`gw-home-share`)
  - `BaseVo` 상속
  - 필드: `nm`, `dsc`, `sortOrd`
- [x] **2-2** `CrdVo.java` 수정 — `vltCatIdx` 필드 추가

---

## Phase 3 — Backend Mapper (카테고리)

- [x] **3-1** `VaultCategoryMapper.java` 인터페이스 생성 (`gw-home-infra-db`)
  - `selectCategoryList` — 전체 목록 (del_at IS NULL, sort_ord ASC)
  - `selectCategory` — 단건 조회 (uuid)
  - `insertCategory` — 등록
  - `updateCategory` — 수정
  - `deleteCategory` — 소프트 삭제
- [x] **3-2** `VaultCategoryMapper.xml` 작성
- [x] **3-3** `VaultMapper.xml` 수정 — `selectCredentialList` 쿼리에 `vlt_cat_idx` 필터 조건 추가

---

## Phase 4 — Backend API (카테고리)

- [x] **4-1** DTO 생성 (`gw-home-api`)
  - `VaultCategoryResponse.java` — 응답 DTO (uuid, nm, dsc, sortOrd)
  - `SaveVaultCategoryRequest.java` — 등록/수정 공용 요청 DTO (nm 필수)
- [x] **4-2** `VaultCategoryService.java` 생성
  - `getCategoryList()` — 전체 카테고리 목록
  - `saveCategory(SaveVaultCategoryRequest)` — 등록 (admin)
  - `updateCategory(String uuid, SaveVaultCategoryRequest)` — 수정 (admin)
  - `deleteCategory(String uuid)` — 소프트 삭제 (admin)
- [x] **4-3** `AdminVaultCategoryController.java` 생성
  - `GET /api/v1/admin/vault-categories` — 목록 조회
  - `POST /api/v1/admin/vault-categories` — 등록
  - `PUT /api/v1/admin/vault-categories/{uuid}` — 수정
  - `DELETE /api/v1/admin/vault-categories/{uuid}` — 삭제
  - `@PreAuthorize("hasRole('ADMIN')")` 적용
- [x] **4-4** `VaultController.java` 수정
  - `GET /api/v1/vault/categories` 엔드포인트 추가 (인증 사용자용 목록 조회)
  - `GET /api/v1/vault/credentials` — query param `categoryUuid` 추가
- [x] **4-5** `VaultService.java` 수정
  - `getCredentialList` — `categoryUuid` 파라미터 추가
  - `saveCredential` / `updateCredential` — `categoryUuid` 처리 추가

---

## Phase 5 — Frontend Types & Composable

- [x] **5-1** `types/vault.ts` 수정
  - `VaultCategory` 타입 추가 (uuid, name, description, sortOrder)
  - `Credential` 타입에 `categoryUuid?: string`, `categoryName?: string` 추가
  - `CredentialListParams`에 `categoryUuid?: string` 추가
  - `SaveCredentialPayload`에 `categoryUuid?: string` 추가
- [x] **5-2** `composables/useVaultCategoryApi.ts` 생성
  - `fetchCategoryList()` — 사용자용 카테고리 목록
  - `fetchAdminCategoryList()` — 관리자용 카테고리 목록
  - `createCategory(payload)` — 등록
  - `updateCategory(uuid, payload)` — 수정
  - `removeCategory(uuid)` — 삭제
- [x] **5-3** `composables/useVaultApi.ts` 수정
  - `fetchCredentialList` — `categoryUuid` 파라미터 반영

---

## Phase 6 — Frontend Vault 페이지 수정

- [x] **6-1** `pages/vault/index.vue` 수정
  - 카테고리 목록 조회 후 필터 탭 렌더링 (전체 + 카테고리별)
  - 탭 클릭 시 `categoryUuid` query param 적용 + 목록 자동 갱신
  - 현재 선택 탭 강조
- [x] **6-2** `components/vault/CredentialFormModal.vue` 수정
  - 카테고리 선택 드롭다운 추가 (선택 항목)
  - 카테고리 목록 `useVaultCategoryApi` 로 조회

---

## Phase 7 — Frontend 관리자 카테고리 페이지 (신규)

- [x] **7-1** `pages/admin/vault-categories/index.vue` 생성
  - 카테고리 목록 테이블 (이름, 설명, 정렬순서)
  - 등록/수정/삭제 기능
  - admin role middleware 적용
  - 삭제 시 confirm 처리

---

## Phase 8 — 네비게이션

- [x] **8-1** 관리자 메뉴에 `Vault 카테고리` 항목 추가 (`/admin/vault-categories`)

---

## 마이그레이션 체크리스트

- [x] DDL: `CREATE TABLE tb_vlt_cat` 작성
- [x] DDL: `ALTER TABLE tb_vlt_crd ADD COLUMN vlt_cat_idx` 작성
- [x] 기존 `tb_vlt_crd` 데이터 영향 검토 (NULL 허용 — 영향 없음)
- [x] 인덱스 추가 확인
- [x] 롤백 SQL 작성
- [x] 운영 반영 순서: `tb_vlt_cat` 생성 → `tb_vlt_crd` 컬럼 추가 → 앱 배포
- [x] 데이터 마이그레이션 필요 여부 확인 (불필요)
