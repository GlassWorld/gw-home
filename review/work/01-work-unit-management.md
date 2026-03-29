# Review: 업무등록 기능 구현

> 작업 레벨: **HEAVY** (신규 DB 테이블 + Full-stack 동시 작업)
> 관련 todo: `todo/work/01-work-unit-management.md`

---

## 작업 개요

일일보고에서 반복 사용하는 업무를 사전에 등록하고 관리하는 기능이다.
업무와 보고는 모두 계정별 독립 데이터로 관리한다.
공유는 이번 범위에 포함하지 않으며, 이후 별도 기능으로 분리한다.
업무는 삭제 대신 사용안함 처리로 유지하며, 이후 일일보고 등록, 주간보고 취합, AI 요약의 기준 단위로 확장 가능한 구조를 목표로 한다.

---

## 변경 목적

- 일일보고 작성 시 자유 텍스트 입력보다 등록 업무 선택 중심으로 전환할 기반 마련
- 개인 프로젝트에서 반복되는 업무명을 표준화해 중복 생성을 줄임
- 이후 보고 집계와 AI 요약에서 동일 업무를 안정적으로 추적할 수 있도록 식별 구조 정리
- 업무와 보고의 소유권을 계정 단위로 명확히 고정해 공유 기능과 분리

---

## 예상 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| DB | `tb_work_unit` 신규 테이블 생성 |
| Backend — share | `WorkUnitVo.java`, 목록 검색용 `WorkUnitListSearchVo.java` 신규 |
| Backend — api | `WorkUnitController`, `WorkUnitService`, Request/Response DTO 신규 |
| Backend — infra-db | `WorkUnitMapper` interface + XML 신규 |
| Frontend — types | `work.ts` 타입 정의 신규 |
| Frontend — composables | `useWorkUnitApi.ts` 신규 |
| Frontend — pages | `pages/work/index.vue` 신규 |
| Frontend — components | 등록/수정 폼 또는 모달 컴포넌트 신규 |

이번 단계는 `work` 도메인 신규 추가로 제한한다.
일일보고 도메인 자체 구현은 범위 밖이지만, 이후 연결 가능한 필드와 API 응답 구조는 이번 문서에서 선반영한다.

---

## 영향 범위 판단

- 레이어 분류: **Full-stack**
- HEAVY 사유:
  - 신규 DB 스키마 추가
  - 백엔드 API와 프론트 관리 화면 동시 구현
  - 이후 보고 기능이 참조할 기준 엔터티 신규 도입
- 도메인 경계:
  - 소유 회원은 `account`의 `mbr_acct_idx`를 참조하되, 기능 책임은 `work` 도메인에 한정
  - 업무 조회, 수정, 비활성화, 보고 연계는 모두 본인 계정 데이터만 대상으로 한다
  - 공유/협업/타인 업무 조회는 이번 범위에서 제외한다
  - `share` 모듈에 VO 추가가 필요하므로 공통 모델 영향 있음

---

## 핵심 설계 방향

### 1. 업무는 "개별 보고 항목"이 아니라 "보고에서 선택하는 기준 단위"로 본다

- 업무명, 설명, 카테고리, 상태, 사용여부만 1차 범위로 유지한다.
- 일일보고 본문, 수행일자, 실제 투입시간 같은 실행 이력 데이터는 분리한다.
- 업무 마스터와 보고 이력 모두 계정 소유 데이터로 저장한다.

### 2. 삭제 대신 사용안함 처리로 관리한다

- 업무 이력과 향후 보고 참조 무결성을 위해 `use_yn` 중심으로 비활성화한다.
- `del_at`은 공통 `BaseVo` 상속에 따라 유지하지만, 기능상 삭제 플로우는 제공하지 않는다.

### 3. 정렬은 확장 가능하되 1차 구현은 단순해야 한다

- 요구사항의 "자주 사용하는 업무 / 최근 사용 업무 우선 배치"는 실제 사용 이력이 아직 없으므로,
  1차에서는 확장 가능한 컬럼만 준비하고 기본 정렬은 `use_yn DESC`, `updated_at DESC`, `created_at DESC`로 둔다.
- 일일보고 기능이 추가되면 `last_used_at`, `use_count`를 갱신하는 방식으로 자연스럽게 확장한다.

---

## DB 설계

### 테이블: `tb_work_unit`

```sql
CREATE TABLE tb_work_unit (
    work_unit_idx    BIGSERIAL    PRIMARY KEY,
    work_unit_uuid   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx     BIGINT       NOT NULL,
    ttl              VARCHAR(200) NOT NULL,
    dscr             TEXT,
    ctgr             VARCHAR(100),
    sts              VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    use_yn           CHAR(1)      NOT NULL DEFAULT 'Y',
    use_cnt          INTEGER      NOT NULL DEFAULT 0,
    last_used_at     TIMESTAMPTZ,
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at           TIMESTAMPTZ
);

CREATE INDEX idx_work_unit_mbr_acct ON tb_work_unit (mbr_acct_idx);
CREATE INDEX idx_work_unit_listing ON tb_work_unit (mbr_acct_idx, use_yn, updated_at DESC, created_at DESC);
CREATE INDEX idx_work_unit_last_used_at ON tb_work_unit (mbr_acct_idx, last_used_at DESC);
```

### 컬럼 메모

| 컬럼 | 설명 |
|------|------|
| `ttl` | 업무명 |
| `dscr` | 설명 |
| `ctgr` | 카테고리, 1차는 자유 입력 |
| `sts` | 진행상태 (`IN_PROGRESS`, `DONE`, `ON_HOLD`) |
| `use_yn` | 사용 여부 (`Y`, `N`) |
| `use_cnt` | 향후 자주 사용하는 업무 우선 정렬용 누적 사용 횟수 |
| `last_used_at` | 최근 사용 업무 우선 정렬용 마지막 사용 시각 |

### 상태값 정의

| `sts` 값 | 표시명 |
|----------|--------|
| `IN_PROGRESS` | 진행중 |
| `DONE` | 완료 |
| `ON_HOLD` | 보류 |

### 중복 정책

- 동일 회원 기준으로 `ttl`은 완전 동일 중복을 막는 것이 바람직하다.
- 1차 구현에서는 `lower(ttl)` 기준 중복 체크를 서비스 또는 SQL에서 수행한다.
- 카테고리가 달라도 같은 업무명을 중복 생성하는 경우가 더 혼란스럽기 때문에 회원 단위 고유성이 적절하다.
- 다른 계정의 동일한 업무명은 허용한다. 계정 간 공유/충돌 해결은 별도 기능으로 다룬다.

---

## 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성 (`gw-home-infra-db/src/main/resources/sql/ddl/work/tb_work_unit.sql`)
- [ ] 롤백 SQL 작성 (정책상 미작성)
- [ ] 기존 데이터 영향 검토 — 신규 테이블이므로 없음
- [ ] NULL / DEFAULT 정책 확인 — `sts` DEFAULT `'IN_PROGRESS'`, `use_yn` DEFAULT `'Y'`, `use_cnt` DEFAULT `0`
- [ ] 인덱스 영향 확인 — 목록 조회와 향후 사용 이력 정렬 인덱스 포함
- [ ] 운영 반영 순서 정의 — DDL 반영 → backend 배포 → frontend 배포
- [ ] 데이터 마이그레이션 필요 여부 확인 — 없음

---

## API 설계

기존 프로젝트 규칙에 맞춰 `/api/v1` 버전을 사용한다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/v1/work-units` | 목록 조회 |
| POST | `/api/v1/work-units` | 신규 등록 |
| PUT | `/api/v1/work-units/{uuid}` | 수정 |
| PUT | `/api/v1/work-units/{uuid}/use` | 사용여부 변경 |
| GET | `/api/v1/work-units/options` | 일일보고 선택용 간략 목록 |

### 목록 조회 쿼리 파라미터

| 파라미터 | 설명 |
|----------|------|
| `keyword` | 업무명 검색 |
| `category` | 카테고리 필터 |
| `status` | 진행상태 필터 |
| `useYn` | 사용여부 필터, 기본값 `Y` |
| `sort` | `recent` / `frequent` / `updated` |

### 응답 필드 제안

```json
{
  "uuid": "work-unit-uuid",
  "title": "일일보고 기능 설계",
  "description": "화면/API/DDL 정리",
  "category": "기획",
  "status": "IN_PROGRESS",
  "useYn": "Y",
  "useCount": 3,
  "lastUsedAt": "2026-03-29T09:00:00+09:00",
  "updatedAt": "2026-03-29T10:00:00+09:00"
}
```

### 일일보고 연계용 옵션 응답

- 선택 컴포넌트에서 바로 쓸 수 있도록 `uuid`, `title`, `category`, `status` 정도만 내리는 얇은 응답을 별도로 둔다.
- 비활성 업무는 기본 제외하고, 필요 시 `includeInactive=true` 같은 명시적 옵션으로만 노출한다.
- 응답 대상은 항상 현재 로그인 계정이 소유한 업무만 한정한다.

**외부 식별자는 `uuid`만 사용한다. 내부 PK `_idx`는 응답에 노출하지 않는다.**

---

## 프론트 화면 설계

### 페이지 제안

- 경로: `/work`
- 구성:
  - 상단 요약/액션 영역
  - 검색창
  - 카테고리 필터, 상태 필터, 사용여부 토글
  - 등록/수정 폼
  - 업무 목록

### UX 포인트

- 등록과 수정은 동일 폼을 재사용한다.
- 수정 시 선택한 업무 값이 폼에 즉시 채워져야 한다.
- 비활성화는 삭제 버튼 대신 `사용안함` 또는 사용여부 토글로 표현한다.
- 기본 목록은 활성 업무 중심으로 보여주고, 비활성 포함 여부는 명시적으로 전환한다.

### 기존 패턴 재사용 후보

- 백엔드 CRUD 패턴은 `AdminVaultCategoryController`, `VaultCategoryService` 구조를 참고할 수 있다.
- 프론트 단일 관리 페이지 패턴은 `gw-home-ui/pages/admin/vault-categories/index.vue`와 유사하다.
- 다만 이번 기능은 "삭제"가 아니라 "사용안함"이므로 액션 의미만 바꿔야 한다.

---

## 파일 목록

### Backend

```
gw-home-infra-db/
  src/main/resources/sql/ddl/work/
    tb_work_unit.sql
  src/main/java/com/gw/infra/db/mapper/work/
    WorkUnitMapper.java
  src/main/resources/mapper/work/
    WorkUnitMapper.xml

gw-home-share/
  src/main/java/com/gw/share/vo/work/
    WorkUnitVo.java
    WorkUnitListSearchVo.java

gw-home-api/
  src/main/java/com/gw/api/controller/work/
    WorkUnitController.java
  src/main/java/com/gw/api/service/work/
    WorkUnitService.java
  src/main/java/com/gw/api/dto/work/
    CreateWorkUnitRequest.java
    UpdateWorkUnitRequest.java
    UpdateWorkUnitUseRequest.java
    WorkUnitResponse.java
    WorkUnitOptionResponse.java
```

### Frontend

```
gw-home-ui/
  types/work.ts
  composables/useWorkUnitApi.ts
  pages/work/index.vue
  components/work/
    WorkUnitForm.vue
```

---

## 리스크 분석

| 리스크 | 수준 | 대응 |
|--------|------|------|
| 신규 도메인 도입으로 구현 파일 수가 많음 | 중간 | review → todo → work 순서로 고정하고 단계별 검증 |
| `share` 모듈 VO 추가 필요 | 중간 | `work` 도메인 전용 VO만 추가하고 공통 유틸 수정은 피함 |
| 일일보고 연계 전인데 `use_cnt`, `last_used_at`이 당장은 비어 있음 | 낮음 | null/0 허용, 기본 정렬은 `updated_at` 기반으로 처리 |
| 중복 업무명 정책이 사용자 기대와 다를 수 있음 | 중간 | 1차는 회원 기준 중복 금지로 단순화, 필요 시 추후 예외 정책 추가 |
| 카테고리 자유 입력으로 표준화가 약할 수 있음 | 낮음 | 현재 범위 유지, 추후 카테고리 관리 기능 분리 가능 |
| 계정 소유권 검증이 누락되면 타인 데이터 접근 위험이 생김 | 높음 | 모든 조회/수정 쿼리에 `mbr_acct_idx` 조건을 강제 |

---

## 대안 검토

- **카테고리 코드 테이블 분리**: 현재 요구사항 범위를 넘는다. 1차는 `VARCHAR` 자유 입력으로 두고, 검색/필터 용도로만 사용한다.
- **업무 상태와 사용여부를 하나로 통합**: `DONE`인 업무도 재사용할 수 있으므로 `sts`와 `use_yn`은 분리하는 편이 맞다.
- **비활성화를 `del_at`으로 대체**: 요구사항이 "삭제보다 사용안함"을 명시하고 있어 `use_yn`이 더 적합하다.
- **정렬 우선순위를 프론트 계산으로 처리**: 일일보고와 연계되면 서버 정렬이 필요하므로, 정렬 기준은 API/DB 레이어에서 잡는 편이 낫다.
- **업무를 공유 가능한 공용 마스터로 설계**: 현재 요구사항과 맞지 않는다. 이번 단계는 계정별 독립 마스터로 두고, 공유는 추후 별도 테이블/권한 모델로 분리한다.

---

## 구현 전 제안 순서

1. review 확정
2. `todo/work/01-work-unit-management.md` 생성
3. DDL 정의
4. VO / Mapper / Service / Controller 구현
5. 프론트 타입 / composable / `/work` 페이지 구현
6. 등록, 수정, 비활성화, 검색/필터 검증

---

## 롤백 계획

- DDL 롤백 스크립트는 운영 정책상 제공하지 않는다.
- 배포 되돌리기는 신규 기능의 라우트/API를 이전 배포 상태로 되돌리는 방식으로 처리한다.
