# Review: 개인 자격증명 관리 (Vault)

> **연관 문서**: [todo/vault/01-credential-manager.md](../../todo/vault/01-credential-manager.md) / work 미생성

---

## 작업 개요

사용자가 자주 사용하는 계정/비밀번호 정보를 등록·수정·삭제하고, 클립보드 복사까지 제공하는 개인 자격증명 관리 메뉴(`vault`) 를 신규 구현한다.

---

## 변경 목적

- 개인 비밀번호/계정 정보를 앱 내에서 직접 관리할 수 있도록 기능을 제공한다.
- 외부 비밀번호 앱 의존 없이 플랫폼 내에서 자체 관리 가능.

---

## 신규 도메인

| 항목 | 값 |
|------|----|
| 도메인명 | `vault` |
| 테이블 | `tb_vlt_crd` (credential) |
| 패키지 | `com.gw.{module}.*.vault` |
| 프론트 경로 | `/vault` |

> 기존 도메인 목록(account, auth, profile, board, comment, file, tag, favorite, admin)에 없는 **신규 도메인**이다.

---

## 데이터 모델

### 테이블: `tb_vlt_crd`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `tb_vlt_crd_idx` | BIGSERIAL PK | 내부 PK |
| `tb_vlt_crd_uuid` | UUID | 외부 노출 식별자 |
| `ttl` | VARCHAR(200) NOT NULL | 제목 |
| `dsc` | TEXT | 설명 |
| `lgn_id` | VARCHAR(200) | 아이디/계정명 |
| `pwd` | TEXT NOT NULL | 비밀번호 (평문 저장) |
| `url` | TEXT | URL |
| `memo` | TEXT | 메모 |
| `created_by` | VARCHAR(100) | 생성자 (로그인 ID) |
| `created_at` | TIMESTAMPTZ | 생성일시 |
| `updated_by` | VARCHAR(100) | 수정자 |
| `updated_at` | TIMESTAMPTZ | 수정일시 |
| `del_at` | TIMESTAMPTZ | 소프트 삭제 |

> **사용자별 분리**: `created_by` 기준으로 본인 데이터만 조회/수정/삭제 가능하도록 WHERE 조건 적용.

---

## API 목록

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/vault/credentials` | 목록 조회 (제목/설명 검색 지원) |
| GET | `/api/v1/vault/credentials/{uuid}` | 상세 조회 |
| POST | `/api/v1/vault/credentials` | 등록 |
| PUT | `/api/v1/vault/credentials/{uuid}` | 수정 |
| DELETE | `/api/v1/vault/credentials/{uuid}` | 삭제 |

---

## 프론트엔드 화면 구성

| 화면 | 설명 |
|------|------|
| `/vault` (index) | 카드형 목록 + 검색 + 신규 등록 버튼 |
| `CredentialDetailModal` | 상세 모달 — 평문 비밀번호 노출, 수정/삭제/복사 버튼 |
| `CredentialFormModal` | 등록/수정 공용 폼 모달 |
| `CredentialCard` | 개별 카드 컴포넌트 — 제목, 설명, 복사/상세 버튼 |

---

## 영향 범위 분석

| 항목 | 영향 여부 | 내용 |
|------|-----------|------|
| 신규 DB 테이블 | ✅ | `tb_vlt_crd` 생성 |
| 기존 테이블 변경 | ❌ | 없음 |
| 공통 모듈 변경 | ❌ | `BaseVo` 상속만, 수정 없음 |
| 인증/보안 구조 | ❌ | 기존 Spring Security + JWT 그대로 사용 |
| 타 도메인 영향 | ❌ | 독립 도메인 |
| 프론트 라우팅 변경 | ❌ | 신규 경로 `/vault` 추가만 |
| 사이드바/네비 수정 | ✅ | 메뉴 항목 추가 필요 |

---

## 리스크 분석

| 리스크 | 수준 | 대응 |
|--------|------|------|
| 비밀번호 평문 저장 | 중 | 요구사항이 평문 저장임을 확인. 추후 암호화는 별도 작업으로 분리. |
| 사용자별 데이터 격리 미흡 | 높 | 전 API에 `created_by = 현재 로그인 사용자` 조건 필수 적용 |
| 클립보드 API 미지원 환경 | 낮 | 브라우저 Clipboard API 사용, 미지원 시 fallback 토스트 안내 |

---

## 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성 (`CREATE TABLE tb_vlt_crd`)
- [ ] 기존 데이터 영향 검토 (신규 테이블 — 영향 없음)
- [ ] NULL / DEFAULT 정책 확인
- [ ] 인덱스 영향 확인 (`created_by`, `del_at` 기준 인덱스 추가 검토)
- [ ] 롤백 SQL 작성 (`DROP TABLE tb_vlt_crd`)
- [ ] 운영 반영 순서 정의
- [ ] 데이터 마이그레이션 필요 여부 확인 (신규 — 불필요)

---

## 구현 예상 파일 목록

### Backend (gw-home-api)
- `VaultController.java`
- `VaultService.java`
- `CredentialResponse.java`
- `SaveCredentialRequest.java`

### Backend (gw-home-infra-db)
- `VaultMapper.java`
- `VaultMapper.xml`
- `ddl/vault.sql`

### Backend (gw-home-share)
- `CrdVo.java`

### Frontend (gw-home-ui)
- `pages/vault/index.vue`
- `components/vault/CredentialCard.vue`
- `components/vault/CredentialDetailModal.vue`
- `components/vault/CredentialFormModal.vue`
- `composables/useVaultApi.ts`
- `types/vault.ts`

---

## 작업 분류

**HEAVY** — 신규 DB 테이블 생성 + 신규 도메인 Full-stack 동시 작업

---

## 대안

1. 현재 안: 신규 `vault` 도메인으로 완전 분리 구현 → **채택**
2. 대안: `profile` 도메인에 포함 → 도메인 경계 위반 가능성으로 기각
