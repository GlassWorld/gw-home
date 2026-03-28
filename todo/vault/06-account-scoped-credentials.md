# Todo: Vault 개인 자격증명 계정별 소유 전환

> **작업 분류**: HEAVY
> **도메인**: vault + account + infra-db

---

## 목표

개인 자격증명 데이터를 `로그인 ID 문자열(created_by)` 기준이 아니라
실제 회원 계정 PK(`mbr_acct_idx`) 기준으로 소유/조회/수정/삭제하도록 전환한다.

이를 통해 로그인 ID 변경 여부와 무관하게,
로그인한 사용자는 자신의 자격증명만 안정적으로 관리할 수 있도록 한다.

---

## 배경

- 현재 `tb_vlt_crd` 는 소유 계정 FK 없이 `created_by` 문자열로만 사용자 식별을 수행한다.
- Vault 조회/상세/수정/삭제 쿼리도 `created_by = loginId` 조건에 의존한다.
- 이 구조는 "개인 자격증명" 성격과 맞지 않고, 계정 기준 소유 보장에도 취약하다.

---

## 권장안

- `tb_vlt_crd.mbr_acct_idx` 컬럼 추가
- 기존 데이터는 `created_by -> tb_mbr_acct.lgn_id` 매핑으로 이관
- Vault Mapper / Service 는 `mbr_acct_idx` 기준으로 소유 판별
- `created_by`, `updated_by` 는 감사용 컬럼으로만 유지
- 권한과 무관하게 로그인한 모든 사용자가 자기 계정 기준으로만 사용

---

## Phase 1 — DB 구조 변경

- [x] **1-1** `tb_vlt_crd` 에 `mbr_acct_idx BIGINT` 컬럼 추가
- [x] **1-2** `mbr_acct_idx` 인덱스 추가
  - `idx_vlt_crd_mbr_acct_del_at (mbr_acct_idx, del_at)`
- [x] **1-3** 컬럼 코멘트 추가
  - `회원 계정 PK`
- [x] **1-4** 신규 생성 DDL 최신화
  - `tb_vlt_crd.sql`
- [x] **1-5** 집계 DDL 최신화
  - `docs/all-ddl.sql`

---

## Phase 2 — 데이터 이관 SQL

- [x] **2-1** 기존 `tb_vlt_crd.created_by` 값을 기준으로 `tb_mbr_acct.lgn_id` 와 매핑
- [x] **2-2** 매핑 결과로 `tb_vlt_crd.mbr_acct_idx` 일괄 업데이트
- [x] **2-3** 이관 후 `mbr_acct_idx IS NULL` 데이터 존재 여부 검증 쿼리 작성
- [x] **2-4** 이관 완료 후 `mbr_acct_idx NOT NULL` 강제
- [x] **2-5** 기존 테이블 DDL 반영

---

## Phase 3 — Backend VO / Mapper

- [x] **3-1** `CrdVo` 에 `mbrAcctIdx` 필드 추가
- [x] **3-2** `VaultMapper` 시그니처를 `createdBy` 문자열 기준에서 `mbrAcctIdx` 기준으로 변경
- [x] **3-3** `VaultMapper.xml` 목록/상세/삭제 조건을 `mbr_acct_idx` 기준으로 변경
- [x] **3-4** 등록 시 `mbr_acct_idx` 저장하도록 INSERT 구문 수정

---

## Phase 4 — Backend Service

- [x] **4-1** 로그인 사용자의 `AcctVo` 조회 후 `account.idx` 확보
- [x] **4-2** Vault 목록 조회를 `mbrAcctIdx` 기준으로 변경
- [x] **4-3** Vault 상세 조회를 `mbrAcctIdx` 기준으로 변경
- [x] **4-4** Vault 수정/삭제 소유 검증을 `mbrAcctIdx` 기준으로 변경
- [x] **4-5** 등록 시 생성자 로그인 ID는 감사용으로만 유지

---

## Phase 5 — 운영 반영 및 검증

- [x] **5-1** `tb_vlt_crd_add_mbr_acct_idx.sql` 실행
- [x] **5-2** `mbr_acct_idx IS NULL` 잔존 건수 점검
- [x] **5-4** 기능 점검
  - 서로 다른 계정 간 자격증명 목록 격리 확인
  - 상세/수정/삭제 권한 확인

---

## 검증 항목

- [x] 같은 서버에서 서로 다른 두 계정이 각자 다른 자격증명 목록을 본다
- [x] 한 계정이 다른 계정의 자격증명 UUID로 조회 시 실패한다
- [x] 한 계정이 다른 계정의 자격증명 UUID로 수정/삭제 시 실패한다
- [x] 로그인 ID 변경과 무관하게 기존 자격증명 소유가 유지된다
- [x] 신규 등록 자격증명에 `mbr_acct_idx` 가 정상 저장된다

---

## 리스크 체크리스트

- [x] `created_by` 와 `tb_mbr_acct.lgn_id` 가 일치하지 않는 데이터 없음
- [x] 이관 후 `mbr_acct_idx IS NULL` 데이터 없음
- [x] 기존 감사 컬럼(`created_by`, `updated_by`) 의미 훼손 없음
- [x] 운영 DB 반영 전 검증 쿼리 준비됨
