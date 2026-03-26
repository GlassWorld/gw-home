# Todo: Vault UI 개선

> review: `review/vault/04-vault-ui-improvements.md`

---

## 항목 1: 메모 마크다운 렌더링 (NORMAL)

### 준비
- [x] 의존성 설치: `marked`, `dompurify`, `@types/dompurify`

### 구현
- [x] `CredentialDetailModal.vue` — 메모 `<dd>` 를 `marked` + `DOMPurify` 로 렌더링
  - `computed`로 `parsedMemo` 생성: `DOMPurify.sanitize(marked.parse(memo))`
  - `<dd v-html="parsedMemo">` 로 교체
  - 마크다운 스타일(코드블록, 링크 등) scoped CSS 추가

---

## 항목 2: 카테고리 셀렉트 검색 (NORMAL)

### 공통 컴포넌트 생성
- [x] `components/common/SearchableSelect.vue` 신규 생성
  - Props: `options: { value: string; label: string }[]`, `modelValue: string`, `placeholder?: string`
  - 검색어 입력 → 옵션 필터링
  - 키보드 네비게이션: ↑↓ 이동, Enter 선택, Esc 닫기
  - 외부 클릭 시 드롭다운 닫기 (`onClickOutside` or `@blur`)
  - 스타일: 기존 `.input-field` / `.vault-modal__select` 과 통일

### 적용
- [x] `CredentialFormModal.vue` — 카테고리 `<select>` → `CommonSearchableSelect` 로 교체
  - options: `categoryList` → `{ value: category.categoryUuid, label: category.name }[]`
  - 미분류 옵션 `{ value: '', label: '미분류' }` 포함

---

## 항목 3: 카테고리 색상 (HEAVY)

> ⚠️ DB 스키마 변경 포함. DB → 백엔드 → 프론트 순서로 진행.

### DB
- [x] DDL: `ALTER TABLE tb_vlt_ctgr ADD COLUMN color VARCHAR(7) DEFAULT NULL`
- [x] 롤백 SQL 준비: `ALTER TABLE tb_vlt_ctgr DROP COLUMN color`

### Backend
- [x] `VltCtgrVo.java` (share) — `color` 필드 추가
- [x] `SaveVaultCategoryRequest.java` — `color` 필드 추가
- [x] `VaultCategoryResponse.java` — `color` 필드 추가
- [x] `VaultCategoryMapper.xml` — INSERT/UPDATE/SELECT SQL에 `color` 반영
- [x] `VaultCategoryService.java` — color 저장/수정 흐름 확인 (Vo → Request 매핑)

### Frontend
- [x] `types/vault.ts` — `VaultCategory` 에 `color?: string` 추가
- [x] `composables/useVaultCategoryApi.ts` — `toCategory()` 에 `color` 매핑 추가
- [x] `pages/admin/vault-categories/index.vue` — 색상 `<input type="color">` 필드 추가
- [x] `components/vault/CredentialCard.vue` — badge `background`/`color` 동적 적용
  - color 없으면 기본값 `rgba(110, 193, 255, 0.12)` (accent)
- [x] `components/vault/CredentialDetailModal.vue` — category badge 동적 색상 적용
