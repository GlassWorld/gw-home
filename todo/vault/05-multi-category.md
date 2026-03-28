# Todo: Vault 자격증명 복수 카테고리 지원

> **연관 문서**: [review/vault/05-multi-category.md](../../review/vault/05-multi-category.md)
> **작업 분류**: HEAVY
> **도메인**: vault + admin

---

## 목표

개인 자격증명 1건에 여러 카테고리를 연결할 수 있도록 구조를 변경한다.
기존 단일 카테고리 구조(`tb_vlt_crd.vlt_cat_idx`)는 점진적으로 이행하고,
최종적으로는 조인 테이블 기반 다대다 구조로 전환한다.

---

## 권장안

- 신규 조인 테이블 `tb_vlt_crd_cat` 도입
- 기존 `tb_vlt_crd.vlt_cat_idx` 값은 이행 SQL로 조인 테이블에 복사
- Backend/Frontend는 `categoryUuid` 단일 필드에서 `categoryUuids` / `categories[]` 구조로 전환
- 점진 이행을 위해 초기 단계에서는 기존 컬럼을 유지하고, 최종 단계에서 제거

---

## Phase 1 — DB 설계 및 DDL

- [x] **1-1** 신규 DDL: `tb_vlt_crd_cat` 테이블 생성
  - PK: `vlt_crd_cat_idx` BIGSERIAL
  - FK: `tb_vlt_crd_idx` → `tb_vlt_crd.tb_vlt_crd_idx`
  - FK: `tb_vlt_cat_idx` → `tb_vlt_cat.tb_vlt_cat_idx`
  - 감사성 최소 컬럼: `created_at`
  - UNIQUE: `(tb_vlt_crd_idx, tb_vlt_cat_idx)`
- [x] **1-2** 인덱스 추가
  - `tb_vlt_crd_cat(tb_vlt_crd_idx)`
  - `tb_vlt_crd_cat(tb_vlt_cat_idx)`
- [x] **1-3** 기존 테이블 DDL 반영 방향 정리
  - 신규 조인 테이블 정의와 기존 테이블 최신 정의를 함께 유지
- [x] **1-4** `docs/all-ddl.sql` 최신화
  - 신규 조인 테이블 반영
  - 단계적 이행 기준 설명 정리

---

## Phase 2 — 데이터 이관 전략

- [x] **2-1** 기존 `tb_vlt_crd.vlt_cat_idx` 데이터를 `tb_vlt_crd_cat` 로 이관하는 SQL 작성
  - `vlt_cat_idx IS NOT NULL` 인 자격증명만 대상
  - 중복 삽입 방지 조건 포함
- [ ] **2-2** 이행 후 검증 쿼리 작성
  - 기존 `vlt_cat_idx` 건수와 조인 테이블 건수 비교
- [x] **2-3** 운영 반영 순서 명시
  - 조인 테이블 생성
  - 데이터 이관
  - 안정화 후 구식 컬럼 제거

---

## Phase 3 — Backend Share / VO / JVO

- [x] **3-1** `gw-home-share` 에 자격증명-카테고리 매핑 VO 추가
  - 예: `CrdCatVo.java`
- [x] **3-2** 다중 카테고리 응답용 JVO 또는 전용 조회 모델 추가
  - 자격증명 1건 + 카테고리 목록 조합 처리 전략 확정
- [x] **3-3** 기존 `CrdVo.vltCatIdx` 사용처 정리
  - 이행 단계 동안 유지 여부 명시

---

## Phase 4 — Backend Mapper

- [x] **4-1** `VaultMapper` 조회 구조 변경
  - 목록/상세 조회에서 카테고리 다건 반환 가능하도록 쿼리 재설계
  - 중복 행 방지 전략 확정
- [x] **4-2** 자격증명-카테고리 매핑 Mapper 추가
  - 자격증명 기준 전체 삭제
  - 다건 삽입
  - 카테고리 UUID 목록 기준 조회
- [x] **4-3** 기존 `categoryUuid` 단일 조건 필터 유지 여부 반영
  - 목록 필터는 단일 카테고리 선택 기준 유지

---

## Phase 5 — Backend API / Service

- [x] **5-1** `SaveCredentialRequest` 변경
  - `categoryUuid` → `categoryUuids`
- [x] **5-2** `CredentialResponse` 변경
  - 단일 `categoryUuid/categoryName/categoryColor` 제거 또는 deprecated 처리
  - `categories[]` 구조 도입
- [x] **5-3** `VaultService` 저장/수정 로직 변경
  - 자격증명 저장 후 매핑 다건 저장
  - 수정 시 기존 매핑 정리 후 재삽입
- [x] **5-4** `VaultService` 목록/상세 응답 조합 변경
  - 다중 카테고리 응답 매핑
- [x] **5-5** 삭제 시 매핑 정리 보장
  - 자격증명 삭제 시 매핑 레코드 정리

---

## Phase 6 — Frontend Type / Composable

- [x] **6-1** `gw-home-ui/types/vault.ts` 변경
  - `Credential.categories` 배열 타입 추가
  - `SaveCredentialPayload.categoryUuid` → `categoryUuids?: string[]`
- [x] **6-2** `useVaultApi.ts` 요청/응답 매핑 수정
  - 다중 카테고리 요청 body 반영
  - 다중 카테고리 응답 파싱
- [x] **6-3** 기존 단일 카테고리 필드 사용처 점검
  - 카드, 상세, 목록, 모달 전부 전환

---

## Phase 7 — Frontend UI

- [x] **7-1** `CredentialFormModal.vue` 수정
  - 단일 선택 → 복수 선택 UI
  - 검색 가능한 멀티 셀렉트 또는 체크리스트 방식 적용
- [x] **7-2** `CredentialCard.vue` 수정
  - 다중 카테고리 badge 렌더링
- [x] **7-3** `CredentialDetailModal.vue` 수정
  - 다중 카테고리 badge 렌더링
- [x] **7-4** `pages/vault/index.vue` 수정
  - 목록 필터는 기존 단일 카테고리 유지
  - 각 카드에 다중 카테고리 표시

---

## Phase 8 — 점진 이행 정리

- [x] **8-1** 구식 컬럼 `tb_vlt_crd.vlt_cat_idx` 제거 시점 확정
- [ ] **8-2** 기존 테이블 DDL 정리
- [x] **8-3** 코드베이스에서 `vltCatIdx`, `categoryUuid` 단일 구조 완전 제거

---

## 검증 항목

- [x] 자격증명 등록 시 복수 카테고리 저장 성공
- [x] 자격증명 수정 시 카테고리 추가/삭제/교체 반영
- [x] 목록/상세 화면에서 다중 카테고리 정상 표시
- [x] 카테고리 0개(미분류) 상태도 정상 저장/조회
- [x] 카테고리 필터로 기존처럼 목록 조회 가능
- [x] 카테고리 삭제 시 매핑 정리 규칙이 의도대로 동작

---

## 리스크 체크리스트

- [x] 중복 행으로 목록 개수 왜곡되지 않음
- [x] API 스펙 변경에 따른 프론트 회귀 없음
- [x] 관리자 카테고리 삭제 시 연결 데이터 처리 규칙 명확화
- [x] 안정화 전까지 `vlt_cat_idx` 제거 금지
