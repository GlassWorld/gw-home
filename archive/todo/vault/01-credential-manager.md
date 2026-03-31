# Todo: 개인 자격증명 관리 (Vault)

> **연관 문서**: [review/vault/01-credential-manager.md](../../review/vault/01-credential-manager.md) / [review/vault/03-field-consolidation.md](../../review/vault/03-field-consolidation.md)
> **작업 분류**: HEAVY
> **도메인**: vault (신규)

---

## Phase 1 — DB

- [x] **1-1** DDL 작성: `tb_vlt_crd` 테이블 생성 스크립트
  - PK: `tb_vlt_crd_idx` BIGSERIAL
  - UUID: `tb_vlt_crd_uuid` UUID DEFAULT gen_random_uuid()
  - 컬럼: `ttl`, `dsc`, `lgn_id`, `pwd`, `url`, `memo`
  - 감사 컬럼: `created_by`, `created_at`, `updated_by`, `updated_at`, `del_at`
  - 인덱스: `created_by` + `del_at` 복합 인덱스
- [x] **1-2** 신규 테이블 DDL 정의 확정 (`tb_vlt_crd.sql`)

---

## Phase 2 — Backend Share (VO)

- [x] **2-1** `CrdVo.java` 생성 (`gw-home-share`)
  - `BaseVo` 상속
  - 필드: `ttl`, `dsc`, `lgnId`, `pwd`, `url`, `memo`

---

## Phase 3 — Backend Mapper

- [x] **3-1** `VaultMapper.java` 인터페이스 생성 (`gw-home-infra-db`)
  - `selectCredentialList` — 목록 조회 (검색어, created_by 조건)
  - `selectCredential` — 단건 조회 (uuid, created_by 조건)
  - `insertCredential` — 등록
  - `updateCredential` — 수정
  - `deleteCredential` — 소프트 삭제 (`del_at` 업데이트)
- [x] **3-2** `VaultMapper.xml` 작성
  - resultMap 정의 (`CrdVo`)
  - 각 쿼리 구현 (동적 검색 포함)

---

## Phase 4 — Backend API

- [x] **4-1** DTO 생성 (`gw-home-api`)
  - `CredentialResponse.java` — 응답 DTO (uuid, ttl, dsc, lgnId, pwd, url, memo, createdAt)
  - `SaveCredentialRequest.java` — 등록/수정 공용 요청 DTO (ttl 필수, pwd 필수)
- [x] **4-2** `VaultService.java` 생성
  - `getCredentialList(String keyword, String loginId)` — 목록 조회
  - `getCredential(String uuid, String loginId)` — 상세 조회
  - `saveCredential(SaveCredentialRequest request, String loginId)` — 등록
  - `updateCredential(String uuid, SaveCredentialRequest request, String loginId)` — 수정
  - `deleteCredential(String uuid, String loginId)` — 삭제
- [x] **4-3** `VaultController.java` 생성
  - `GET /api/v1/vault/credentials` — 목록 조회 (query param: `keyword`)
  - `GET /api/v1/vault/credentials/{uuid}` — 상세 조회
  - `POST /api/v1/vault/credentials` — 등록
  - `PUT /api/v1/vault/credentials/{uuid}` — 수정
  - `DELETE /api/v1/vault/credentials/{uuid}` — 삭제
  - 모든 엔드포인트: `@AuthenticationPrincipal` 또는 SecurityContext에서 `loginId` 추출

---

## Phase 5 — Frontend Types & Composable

- [x] **5-1** `types/vault.ts` 생성
  - `Credential` — 목록/상세 타입
  - `SaveCredentialPayload` — 등록/수정 요청 타입
  - `CredentialListParams` — 목록 조회 파라미터 타입
- [x] **5-2** `composables/useVaultApi.ts` 생성
  - `fetchCredentialList(params)` — 목록 조회
  - `fetchCredential(uuid)` — 상세 조회
  - `createCredential(payload)` — 등록
  - `updateCredential(uuid, payload)` — 수정
  - `removeCredential(uuid)` — 삭제

---

## Phase 6 — Frontend Components

- [x] **6-1** `components/vault/CredentialCard.vue` 생성
  - Props: `credential: Credential`
  - Emits: `copy`, `detail`
  - 표시: 제목, 설명(2줄 말줄임)
  - 버튼: 복사, 상세
- [x] **6-2** `components/vault/CredentialFormModal.vue` 생성
  - Props: `visible`, `credential?: Credential` (수정 시 기존 데이터)
  - Emits: `saved`, `close`
  - 입력 항목: 제목(필수), 설명, 아이디, 비밀번호(필수), URL, 메모
  - 필수값 검증 처리
  - 저장 성공/실패 토스트
- [x] **6-3** `components/vault/CredentialDetailModal.vue` 생성
  - Props: `visible`, `credential: Credential`
  - Emits: `edit`, `deleted`, `close`
  - 표시: 제목, 설명, 아이디, 비밀번호(평문), URL, 메모
  - 버튼: 복사, 수정, 삭제, 닫기
  - 삭제 시 confirm 다이얼로그 후 삭제 처리

---

## Phase 7 — Frontend Page

- [x] **7-1** `pages/vault/index.vue` 생성
  - 상단: 검색 입력 + 신규 등록 버튼 (우측)
  - 본문: `CredentialCard` 카드 그리드 목록
  - 빈 상태(empty state) UI
  - 복사 버튼 → 클립보드 복사 → "복사되었습니다" 토스트
  - 상세 버튼 → `CredentialDetailModal` 오픈
  - 신규 등록 버튼 → `CredentialFormModal` 오픈
  - CRUD 완료 후 목록 자동 갱신
  - auth middleware 적용

---

## Phase 8 — 네비게이션 메뉴 추가

- [x] **8-1** 사이드바/네비게이션에 `Vault` 메뉴 항목 추가 (`/vault` 경로)

---

## Phase 9 — 필드 통합 (dsc + url + memo → memo)

> 참조: [review/vault/03-field-consolidation.md](../../review/vault/03-field-consolidation.md)

- [x] **9-1** DB 스키마 변경
  - `ALTER TABLE tb_vlt_crd DROP COLUMN dsc`
  - `ALTER TABLE tb_vlt_crd DROP COLUMN url`
  - `ddl/vault/tb_vlt_crd.sql` 업데이트 (dsc, url 제거)
  - 기존 테이블 DDL 최신화
- [x] **9-2** `CrdVo.java` 수정 — `dsc`, `url` 필드 제거
- [x] **9-3** `VaultMapper.xml` 수정 — INSERT/UPDATE/SELECT/resultMap에서 `dsc`, `url` 제거
- [x] **9-4** `CredentialResponse.java` 수정 — `dsc`, `url` 필드 제거
- [x] **9-5** `SaveCredentialRequest.java` 수정 — `dsc`, `url` 필드 제거
- [x] **9-6** `types/vault.ts` 수정 — `Credential`, `SaveCredentialPayload`에서 `description`, `url` 제거
- [x] **9-7** `CredentialCard.vue` 수정 — 카드 본문 표시를 `description` → `memo` 로 변경
- [x] **9-8** `CredentialFormModal.vue` 수정 — 설명/URL 입력 필드 제거, memo 입력란만 유지
- [x] **9-9** `CredentialDetailModal.vue` 수정 — 설명/URL 표시 제거, memo 표시만 유지

---

## 마이그레이션 체크리스트

- [x] DDL 변경 SQL 작성 (`CREATE TABLE tb_vlt_crd`)
- [x] 기존 데이터 영향 검토 (신규 테이블 — 영향 없음 확인)
- [x] NULL / DEFAULT 정책 확인
- [x] 인덱스 영향 확인
- [x] 데이터 마이그레이션 필요 여부 확인 (신규 — 불필요)
