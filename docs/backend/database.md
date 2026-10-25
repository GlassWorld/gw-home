# 데이터베이스 규칙

## 이 문서의 목적

이 문서는 DDL 작성과 테이블 설계 시 지켜야 할 공통 기준을 설명한다.

## 기본 식별자 규칙

```sql
{table}_idx    BIGSERIAL PRIMARY KEY
{table}_uuid   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()
```

- 내부 조인과 참조에는 `_idx`를 사용한다
- 외부 노출과 API 식별에는 `_uuid`를 사용한다

## 시간 컬럼 규칙

```sql
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
```

- `TIMESTAMP`는 사용하지 않는다
- `updated_at`은 애플리케이션 또는 트리거로 갱신한다

## 감사 컬럼 규칙

```sql
created_by  VARCHAR(100)
updated_by  VARCHAR(100)
```

- `created_by`, `updated_by`에는 내부 숫자 키가 아니라 로그인 ID를 저장한다

## 소프트 삭제 규칙

```sql
del_at  TIMESTAMPTZ
```

- `NULL`이면 정상 데이터
- 값이 있으면 삭제된 데이터로 본다

## 테이블 네이밍

```text
tb_{축약도메인}_{축약대상}
예: tb_brd_pst, tb_brd_cmt, tb_mbr_acct
```

## 파일 도메인 원칙

- 파일 업로드는 `file` 도메인이 전담한다
- 다른 도메인은 업로드 결과 URL만 저장한다
- 게시글 본문 이미지도 URL 문자열로 저장한다
- 파일 메타데이터는 `tb_file`에만 저장한다

## 예시 DDL

```sql
CREATE TABLE tb_brd_pst (
    brd_pst_idx   BIGSERIAL PRIMARY KEY,
    brd_pst_uuid  UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    ttl           VARCHAR(300) NOT NULL,
    cntnt         TEXT NOT NULL,
    img_url       TEXT,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_at        TIMESTAMPTZ
);

CREATE INDEX idx_brd_pst_uuid ON tb_brd_pst (brd_pst_uuid);
```

## DDL 파일 위치

```text
{project}-infra-db/src/main/resources/sql/ddl/{domain}/{table}.sql
```

- 기준은 도메인별 개별 DDL 파일이다
- 문서용 생성 스냅샷은 [all-ddl.sql](../all-ddl.sql)이다. 빈 `home` 스키마에서만 실행되며 기존 DB의 갱신·초기화용 파일이 아니다. 전체 앱의 설치 자동화가 아니므로 개별 도메인 DDL을 기준으로 한다.
- 개별 DDL을 바꾸면 집계 스크립트도 함께 최신화한다

## 리뉴얼 테이블 생성·적용 안내

**파일 반영과 DB 반영은 별개다. 현재 저장소에는 DDL 자동 실행·Flyway·Liquibase 연결이 없으므로 서버 재시작만으로 테이블이 생성되지 않는다. 실제 DB에는 아직 실행하지 않았다.**

### 생성·변경·보존 대상

| 구분 | 테이블 / 컬럼 | 적용 내용 |
|------|---------------|-----------|
| 선행 기반 | `tb_mbr_acct`, `tb_work_unit`, `tb_wrk_todo` | 기존 계정·업무·TODO를 그대로 사용 |
| 2단계 변경 | `tb_wrk_todo.cmpl_at` | `TIMESTAMPTZ` 컬럼과 소유자·일정 인덱스 추가. 과거 완료는 NULL 유지 |
| 4단계 생성 | `tb_wrk_ntf_stg` | 수신 주소 확인·시간대·요약/개별 알림 설정 |
| 4단계 생성 | `tb_wrk_todo_rmd` | TODO별 지정 시각 예약·버전·일정 스냅샷 |
| 4단계 생성 | `tb_wrk_ntf_dlvr` | 발송 회차·계정별 중복 방지·시도/결과 이력 |
| 5단계 재사용 | `tb_work_unit`, `tb_wrk_todo` | 간트·완료 통계는 새 테이블 없이 일정·진행률·완료 시각 조회 |
| 보고서 보존 | `tb_wrk_dly_rpt`, `tb_wrk_dly_rpt_wrk_unit`, `tb_wrk_wkl_rpt` | 기존 테이블·행 유지. 리뉴얼 적용 SQL에 생성·변경·삭제 없음 |
| Git 보존 | `tb_wrk_git_acct`, `tb_wrk_git_prj`, `tb_wrk_unit_git_cfg`, `tb_wrk_unit_git_prj` | 기존 연동·데이터 유지. 리뉴얼 적용 SQL에서 변경하지 않음 |

메일을 보내지 않아도 현재 TODO 쓰기 로직의 예약 취소·동기화가 알림 테이블을 사용하므로 **3개 모두 생성해야 한다.** 생성 자체는 메일을 활성화하지 않는다. `WORK_NOTIFICATIONS_ENABLED=false`를 유지한다.

### 기존 DB 적용

1. 적용 대상 DB와 `home` 스키마, 백업, 쓰기 중단 시간을 확인한다. 현재 앱의 `currentSchema=home`과 동일한 곳에 적용한다.
2. [project-renewal-verify.sql](../../gw-home-infra-db/src/main/resources/sql/ddl/work/project-renewal-verify.sql)을 실행해 적용 전 상태를 기록한다. 읽기 전용이며 테이블이 없어도 실행된다.
3. [project-renewal-migrate.sql](../../gw-home-infra-db/src/main/resources/sql/ddl/work/project-renewal-migrate.sql) **전체를 한 번 실행**한다. 기반 테이블을 확인한 뒤 2단계 완료 시각 → 4단계 설정 → TODO 예약 → 발송 이력 순서로 한 트랜잭션에서 반영한다.
4. 동일한 확인 SQL을 다시 실행한다. `required=true`인 모든 행이 `ok=true`여야 한다. `required=false`는 보고서·Git의 보존 현황이며 적용 전과 비교한다. 이 확인은 필수 테이블·컬럼 타입·주요 인덱스 점검이고 모든 제약·기존 데이터 정합성을 대신하지 않는다.
5. SQL 성공 후 API → 프런트를 배포하고 TODO 조회·저장·완료/취소·간트·통계를 확인한다. 메일은 비활성 상태를 유지한다.

DB 클라이언트에서 전체 파일을 실행하거나, 본인이 구성한 PostgreSQL 접속 서비스(아래는 `gw-home` 예시)를 사용한다. 명령행에 비밀번호를 넣지 않는다.

```sh
psql --dbname="service=gw-home" --set=ON_ERROR_STOP=1 --file=gw-home-infra-db/src/main/resources/sql/ddl/work/project-renewal-verify.sql
psql --dbname="service=gw-home" --set=ON_ERROR_STOP=1 --file=gw-home-infra-db/src/main/resources/sql/ddl/work/project-renewal-migrate.sql
psql --dbname="service=gw-home" --set=ON_ERROR_STOP=1 --file=gw-home-infra-db/src/main/resources/sql/ddl/work/project-renewal-verify.sql
```

- 통합 적용 파일에 트랜잭션이 포함되어 있으므로 `psql --single-transaction`을 추가하지 않는다. 오류가 난 GUI 세션은 `ROLLBACK;` 후 원인을 확인하고 전체 파일을 다시 실행한다.
- `unterminated dollar-quoted string`이 `DO $$` / `missing_tables TEXT`에서 발생했다면 검사 블록이 중간에 나뉘어 전송되었을 가능성이 있다. 수정본은 사전 검사 본문을 일반 SQL 문자열(`DO '…'`)로 인용한다. 해당 마이그레이션 실행 세션에서 `ROLLBACK;`을 실행하고, 수정 파일을 다시 연 뒤 `BEGIN;`부터 마지막 `COMMIT;`까지 전체 스크립트를 실행한다. `DECLARE`나 `BEGIN` 내부 행만 선택 실행하지 않는다. 기존 DB에는 `all-ddl.sql` 대신 `project-renewal-migrate.sql`을 사용한다.
- 잠금 대기 5초를 넘으면 실패한다. 쓰기 중단·잠금 해소 후 재시도한다. 인덱스 생성은 기존 TODO 쓰기에 영향을 줄 수 있으므로 배포 시간을 확보한다.
- 정상 적용 후 재실행해도 컬럼·테이블·인덱스와 데이터가 중복 생성되지 않는다. 기존 객체의 잘못된 정의를 `IF NOT EXISTS`가 수리해 주는 것은 아니다. 확인 SQL이 실패하면 구조를 먼저 대조한다.
- 기반 테이블이 없거나 기존 `cmpl_at`이 다른 타입이면 자동 보정하지 않고 중단한다. `home` 밖의 같은 이름 테이블을 대신 사용하지 않는다.
- 별도 2단계·4단계 파일을 이미 정상 적용한 환경도 통합 파일을 실행할 수 있다. 통합 파일은 원본 두 파일의 내용을 복제하므로 함께 변경하며 검증 스크립트로 동기화를 확인한다.
- 앱 롤백 시 추가 컬럼·테이블·이력은 유지한다. 보고서 물리 삭제, 과거 완료일 역산, `drop-legacy-tables.sql` 실행은 이 절차에 포함하지 않는다. 보고서 즐겨찾기 정리는 기존의 별도 선택 작업이다.

### 신규 업무 스키마 생성

- 계정 등 앱 기반 도메인 생성이 끝난 `home` 스키마를 전제로 `tb_work_unit.sql` → `tb_wrk_todo.sql` → `tb_wrk_ntf.sql` 순서로 실행한다. 작업 세션의 `search_path`를 `home`으로 명시하고 트랜잭션 안에서 실행한다.
- 신규 `tb_wrk_todo.sql`에는 이미 `cmpl_at`과 일정 인덱스가 있으므로 완료 시각 추가 파일은 필수가 아니다. 마지막에는 동일한 확인 SQL을 실행한다.
- Git 기능을 새로 설치할 때는 계정·업무 다음에 `tb_wrk_git_acct.sql` → `tb_wrk_git_prj.sql` → `tb_wrk_unit_git_cfg.sql` → `tb_wrk_unit_git_prj.sql`을 적용한다. 기존 DB의 Git 테이블은 재생성하지 않는다.
- 종료된 보고서 테이블은 현재 앱 실행의 필수 조건이 아니다. 보관·복구용 DDL과 기존 데이터는 유지한다.
- `docs/all-ddl.sql`은 참고용 스냅샷으로, 모든 도메인의 최신 설치 파일을 대신하지 않는다. 기존 테이블이 있는 `home`에서는 오류로 중단하며 데이터 삭제 구문은 없다.

### 적용 SQL 검증 기록 (2026-10-25)

- [renewal-schema.cjs](../../gw-home-api/src/test/resources/work/renewal-schema.cjs): 원본 동기화, 빈 DB 확인, 신규 생성, 기존 데이터·완료일 미상 보존과 재실행, 스키마 분리, 중간 오류의 전체 롤백, 잘못된 완료 시각 타입 거부, 통합 DDL의 재초기화 차단, 인덱스 누락 감지에 더해 달러 인용 블록 분리 오류 재현·수정본 문장별 실행을 포함한 12개 시나리오를 통과했다.
- 기존 알림 SQL 검증 13개도 통과했다. 운영 DB 접속 없이 격리 PGlite에서 실행했으며 애플리케이션 의존성을 추가하지 않았다.
- 재현: 테스트용 PGlite가 설치된 환경에서 저장소 루트 기준으로 아래 명령을 실행한다. 마지막 인자는 설치된 패키지 디렉터리이며 현재 로컬 검증 경로의 예시다.

```sh
node gw-home-api/src/test/resources/work/renewal-schema.cjs gw-home-api/build/verification/node_modules/@electric-sql/pglite
```

- 실제 DB의 적용 여부·권한·잠금·운영 데이터 규모는 아직 검증하지 않았다. 확인 SQL의 성공은 런타임의 모든 제약·업무 기능 검증을 대신하지 않는다.

## TODO 완료 시각 마이그레이션 (리뉴얼 2단계)

- 신규 DB: `sql/ddl/work/tb_wrk_todo.sql`과 `docs/all-ddl.sql`에 `cmpl_at TIMESTAMPTZ` 및 소유자·일정 인덱스를 반영했다.
- 기존 DB: 현재 리뉴얼 버전은 위 통합 적용 파일을 사용한다. [tb_wrk_todo_completion_migrate.sql](../../gw-home-infra-db/src/main/resources/sql/ddl/work/tb_wrk_todo_completion_migrate.sql)은 2단계만 반영할 때의 원본이며 재실행해도 컬럼·인덱스를 중복 생성하지 않는다.
- 적용 전 백업과 테이블 잠금을 허용할 배포 시간을 확인하고 SQL 적용 → 서버 → 프런트 순으로 배포한다. 이번 작업에서는 실제 DB를 변경하지 않았다.
- 기존 완료는 NULL을 유지한다. `updated_at`으로 역산하지 않는다. 직접 완료 시각만 기록하며 반복 완료는 보존, 완료 취소는 NULL, 재완료는 새 시각으로 저장한다.
- 자식이 생겨 부모가 된 항목은 직접 완료 시각을 비운다. 마지막 자식 삭제로 다시 최하위가 된 항목에 새 완료 시각을 만들지 않는다.
- 같은 업무의 TODO 쓰기는 소유 업무 행의 `FOR UPDATE` 잠금 안에서 수행한다. 읽기 전용 통합 목록은 소유자·삭제·활성 조건과 최하위 조건을 공통 SQL로 사용한다.
- 앱 롤백 시 추가 컬럼·인덱스는 유지해도 이전 코드와 호환된다. 컬럼을 삭제하는 롤백은 신규 완료 기록을 잃으므로 별도 데이터 작업으로 취급한다.

## 메일 알림 스키마 (리뉴얼 4단계)

- [tb_wrk_ntf.sql](../../gw-home-infra-db/src/main/resources/sql/ddl/work/tb_wrk_ntf.sql)은 `tb_wrk_ntf_stg`(설정·수신 확인), `tb_wrk_todo_rmd`(TODO별 예약), `tb_wrk_ntf_dlvr`(발송 회차·이력)를 추가한다. `docs/all-ddl.sql`에도 동일 정의를 반영했다.
- 기존 계정·업무·TODO와 2단계 완료 시각 스키마 적용 후, **4단계 API 배포 전에** 이 SQL을 적용한다. 서버 발송 스위치가 꺼져 있어도 TODO 쓰기는 예약 취소·정합성 쿼리를 실행하므로 테이블이 필요하다.
- 기존 DB에는 전체 `all-ddl.sql`을 실행하지 않는다. 현재 리뉴얼 버전은 위 통합 적용 파일을 사용한다. 알림 DDL만 적용할 때도 대상 스키마와 트랜잭션을 명시하고, 소유자 복합 외래 키용 UNIQUE 인덱스 생성 시 기존 TODO 테이블 잠금 시간을 고려한다.
- `IF NOT EXISTS`로 정상 적용 후 재실행할 수 있다. 기존 업무·TODO·보고서 행을 삭제/이관하지 않는다. 예약/이력 시각은 TIMESTAMPTZ, TODO 일정 스냅샷은 DATE다.
- 외부 API는 UUID만 사용한다. 기존 `BaseVo`의 실제 내부 필드 `idx`를 상속하고 Mapper에서 `AS idx`로 연결해 기존 모델과 호환한다.
- 앱 롤백은 전체 발송 중단 후 이전 서버·UI로 되돌린다. 추가 테이블·이력은 보존하며 DROP을 실행하지 않는다. 데이터 보관 기간·물리 삭제는 별도 운영 정책이다.
- 실제 MyBatis SQL과 새 DDL을 격리된 PGlite에서 실행하는 검증 스크립트는 `gw-home-api/src/test/resources/work/notification-sql.cjs`다. Java 테스트가 `gw-home-api/build/notification-sql.json`을 먼저 생성한다.

## 일정·통계 읽기 모델 (리뉴얼 5단계)

- 신규 DDL 없이 `tb_work_unit`과 `tb_wrk_todo`의 날짜·완료 시각을 재사용한다. 과거 완료일 미상 값은 유지하며 보고서 보관 데이터는 참조하지 않는다.
- `WorkTodoMapper.ownedWorkTodoSelect`는 소유권·활성·분류 원본, `myWorkTodoSource`는 최하위/일정 겹침 필터다. `WorkInsightsMapper`가 이 원본을 재사용하여 간트 조상, 업무별 현재 현황, 시간대별 완료 추이를 조회한다.
- 이번 단계가 스키마를 추가하지 않더라도 2단계 완료 시각 SQL과 4단계 알림 SQL은 기존 API의 선행 배포 조건이다. 메일 발송 비활성화는 해당 테이블 의존을 제거하지 않는다.
- `WorkInsightsMapperContractTest`가 실제 바인딩 SQL을 `build/insights-sql.json`으로 내보낸다. `src/test/resources/work/insights-sql.cjs`에 격리 PGlite 패키지 경로를 전달해 8개 SQL 시나리오를 검증했다. 실제 PostgreSQL JDBC·운영 데이터 규모의 EXPLAIN/인덱스 비용은 후속 회귀·배포 환경에서 확인한다.
