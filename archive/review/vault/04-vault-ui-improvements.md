# Review: Vault UI 개선 (메모 줄바꿈 / 카테고리 검색 / 카테고리 색깔)

## 작업 개요

Vault 기능 3가지 UI 개선:
1. 증명 상세(CredentialDetailModal) 메모 텍스트 줄바꿈 + 마크다운 렌더링
2. 자격증명 등록/수정 폼(CredentialFormModal) 카테고리 셀렉트박스 검색 기능
3. 카테고리에 색상 설정 기능 (badge 색 커스터마이징)

---

## 항목 1: 메모 줄바꿈 / 마크다운 렌더링

### 변경 목적
현재 메모가 한 줄로 표시됨. 줄바꿈 및 마크다운(볼드, 링크, 코드 블록 등) 렌더링 지원.

### 예상 영향 범위
| 파일 | 변경 내용 |
|------|-----------|
| `components/vault/CredentialDetailModal.vue` | `<dd>` → 마크다운 렌더 컴포넌트로 교체 |
| `nuxt.config.ts` / `package.json` | `@nuxtjs/mdc` 또는 `marked` 라이브러리 추가 |

### 구현 방향 ✅ 확정: 옵션 B
- `marked` 라이브러리로 HTML 변환 후 `v-html` 렌더링
- `DOMPurify` sanitize 필수 (XSS 방지)
- 의존성 추가: `marked`, `dompurify`, `@types/dompurify`

### 리스크
- `v-html` XSS → DOMPurify sanitize 적용으로 방지
- 기존 메모 데이터 형식 변경 없음 (DB 그대로)

### 작업 분류: **NORMAL**

---

## 항목 2: 카테고리 셀렉트박스 검색 기능

### 변경 목적
카테고리 목록이 많아질 경우 `<select>` 드롭다운만으로는 탐색 불편. 검색 입력으로 필터링.

### 예상 영향 범위
| 파일 | 변경 내용 |
|------|-----------|
| `components/vault/CredentialFormModal.vue` | 기존 `<select>` → 검색 가능 셀렉트로 교체 |
| `components/common/SearchableSelect.vue` (신규, 선택) | 공통 검색 가능 셀렉트 컴포넌트 |

### 구현 방향 ✅ 확정: 옵션 B
- 커스텀 combobox: `input` + 드롭다운 오버레이
- 공통 컴포넌트 `components/common/SearchableSelect.vue` 신규 생성
- Props: `options: { value: string; label: string }[]`, `modelValue: string`, `placeholder?: string`
- 키보드 네비게이션(↑↓ Enter Esc) 지원

### 작업 분류: **NORMAL**

---

## 항목 3: 카테고리 색깔 입히기

### 변경 목적
카테고리별로 색상을 지정해 badge를 컬러로 표시. 카드 및 상세 모달에서 구분 용이.

### 예상 영향 범위

#### Backend (DB + API 변경 — HEAVY)
| 레이어 | 파일 | 변경 내용 |
|--------|------|-----------|
| DDL | `tb_vlt_ctgr` | `color VARCHAR(7)` 컬럼 추가 (HEX, nullable) |
| VO | `VltCtgrVo.java` (share 모듈) | `color` 필드 추가 |
| Response DTO | `VaultCategoryResponse.java` | `color` 필드 추가 |
| Request DTO | `SaveVaultCategoryRequest.java` | `color` 필드 추가 |
| Service | `VaultCategoryService.java` | color 저장/수정 처리 |
| Mapper XML | `VaultCategoryMapper.xml` | INSERT/UPDATE SQL에 color 추가 |

#### Frontend
| 파일 | 변경 내용 |
|------|-----------|
| `types/vault.ts` | `VaultCategory.color?: string` 추가 |
| `composables/useVaultCategoryApi.ts` | `toCategory()` color 매핑 추가 |
| `pages/admin/vault-categories/index.vue` | 색상 입력 필드 (color picker) 추가 |
| `components/vault/CredentialCard.vue` | badge 색상 동적 적용 |
| `components/vault/CredentialDetailModal.vue` | badge 색상 동적 적용 |

### DB 마이그레이션 체크리스트
- [ ] DDL 변경 SQL 작성 (`ALTER TABLE tb_vlt_ctgr ADD COLUMN color VARCHAR(7)`)
- [ ] 기존 데이터 영향 검토 (NULL 허용 → 기존 데이터 영향 없음)
- [ ] NULL / DEFAULT 정책 확인 (DEFAULT NULL, 미설정 시 기본 accent 색상 사용)
- [ ] 인덱스 영향 확인 (color는 인덱스 불필요)
- [ ] 롤백 SQL 작성 (`ALTER TABLE tb_vlt_ctgr DROP COLUMN color`)
- [ ] 운영 반영 순서 정의 (DB 먼저 → 백엔드 → 프론트)
- [ ] 데이터 마이그레이션 필요 여부 확인 (불필요, nullable)

### 작업 분류: **HEAVY** (DB 스키마 변경 + share 모듈 변경 + Full-stack)

---

## 전체 작업 분류 요약

| 항목 | 분류 | 비고 |
|------|------|------|
| 메모 마크다운 | NORMAL | marked + DOMPurify 추가 |
| 카테고리 검색 셀렉트 | NORMAL | 공통 컴포넌트 신규 생성 |
| 카테고리 색상 | HEAVY | DB 스키마 변경 포함 |

## 관련 파일

- `gw-home-ui/components/vault/CredentialDetailModal.vue`
- `gw-home-ui/components/vault/CredentialFormModal.vue`
- `gw-home-ui/components/vault/CredentialCard.vue`
- `gw-home-ui/pages/admin/vault-categories/index.vue`
- `gw-home-api/.../dto/vault/VaultCategoryResponse.java`
- `gw-home-api/.../dto/vault/SaveVaultCategoryRequest.java`
- `gw-home-api/.../service/vault/VaultCategoryService.java`
- `gw-home-infra-db/.../mapper/vault/VaultCategoryMapper.xml`
