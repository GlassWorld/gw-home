# Review: 업무등록 기능 구현

> 작업 레벨: **HEAVY** (신규 DB 테이블 + Full-stack 동시 작업)
> 관련 todo: `todo/work/01-work-unit-management.md` (미생성)

---

## 작업 개요

일일보고 작성 시 사용할 업무를 사전에 등록·관리하는 기능.
삭제 대신 상태 기반 관리(사용여부, 진행상태)를 기본으로 한다.

---

## 변경 목적

- 일일보고/주간보고 기능의 기준 단위인 업무를 별도 관리
- 등록된 업무를 이후 일일보고 등록 화면에서 선택 방식으로 연결
- 중복 업무 생성 방지 및 이력 관리

---

## 예상 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| DB | `tb_work_unit` 신규 테이블 생성 |
| Backend — share | `WorkUnitVo.java` 신규 |
| Backend — api | `WorkUnitController`, `WorkUnitService`, Request/Response DTO 신규 |
| Backend — infra-db | `WorkUnitMapper` interface + XML 신규 |
| Frontend — types | `work.ts` 타입 정의 신규 |
| Frontend — composables | `useWorkUnit.ts` 신규 |
| Frontend — pages | `pages/work/index.vue` 신규 |
| Frontend — components | 등록/수정 모달 컴포넌트 신규 |

기존 도메인 코드 수정 없음. 공통 모듈 변경 없음.

---

## DB 설계

### 테이블: `tb_work_unit`

```sql
CREATE TABLE tb_work_unit (
    work_unit_idx   BIGSERIAL    PRIMARY KEY,
    work_unit_uuid  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    mbr_acct_idx    BIGINT       NOT NULL,              -- 소유 회원
    ttl             VARCHAR(200) NOT NULL,              -- 업무명
    dscr            TEXT,                               -- 설명
    ctgr            VARCHAR(100),                       -- 카테고리
    sts             VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',  -- 상태 (IN_PROGRESS / DONE / ON_HOLD)
    use_yn          CHAR(1)      NOT NULL DEFAULT 'Y',  -- 사용여부 (Y/N)
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    del_at          TIMESTAMPTZ
);

CREATE INDEX idx_work_unit_mbr_acct ON tb_work_unit (mbr_acct_idx);
CREATE INDEX idx_work_unit_created_at ON tb_work_unit (created_at DESC);
```

**상태값 정의**

| sts 값 | 표시명 |
|--------|--------|
| IN_PROGRESS | 진행중 |
| DONE | 완료 |
| ON_HOLD | 보류 |

---

## 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성 (`gw-home-infra-db/.../ddl/work/tb_work_unit.sql`)
- [ ] 기존 데이터 영향 검토 — 신규 테이블이므로 없음
- [ ] NULL / DEFAULT 정책 확인 — `sts` DEFAULT 'IN_PROGRESS', `use_yn` DEFAULT 'Y'
- [ ] 인덱스 영향 확인 — 신규 인덱스 2개
- [ ] 롤백 SQL 작성 — `DROP TABLE IF EXISTS tb_work_unit;`
- [ ] 운영 반영 순서 정의 — DDL → 앱 배포 순
- [ ] 데이터 마이그레이션 필요 여부 — 없음 (신규)

---

## API 설계

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/work-units` | 목록 조회 (검색: title, 필터: category, status, useYn) |
| POST | `/api/work-units` | 신규 등록 |
| PUT | `/api/work-units/{uuid}` | 수정 |
| PATCH | `/api/work-units/{uuid}/inactive` | 비활성화 |

**외부 식별자: `uuid` 사용 (내부 PK `_idx` 노출 금지)**

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

gw-home-api/
  src/main/java/com/gw/api/controller/work/
    WorkUnitController.java
  src/main/java/com/gw/api/service/work/
    WorkUnitService.java
  src/main/java/com/gw/api/dto/work/
    CreateWorkUnitRequest.java
    UpdateWorkUnitRequest.java
    WorkUnitResponse.java
```

### Frontend

```
gw-home-ui/
  types/work.ts
  composables/work/useWorkUnit.ts
  pages/work/index.vue
  components/work/
    WorkUnitFormModal.vue
```

---

## 리스크 분석

| 리스크 | 수준 | 대응 |
|--------|------|------|
| 신규 도메인으로 기존 코드 영향 없음 | 낮음 | - |
| 이후 일일보고 연결 시 FK 설계 필요 | 중간 | `work_unit_idx` 기준 참조 가능하도록 구조 유지 |
| 카테고리 자유 입력 vs 코드 테이블 | 낮음 | 1차는 자유 입력(VARCHAR), 추후 분리 가능 |

---

## 대안 검토

- **카테고리 코드 테이블 분리**: 현재 요구사항 범위 초과 — 1차 자유 입력으로 결정
- **soft delete vs `use_yn`**: 요구사항이 "사용안함 처리" 명시 → `use_yn` 사용 (del_at은 향후 완전삭제용으로 유지)

---

## 롤백 계획

- DDL 롤백: `DROP TABLE IF EXISTS tb_work_unit;`
- 앱 롤백: 신규 도메인이므로 이전 버전 배포 시 자동 비활성
