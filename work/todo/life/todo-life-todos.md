# 업무·일상 할 일 분리 구현 계획

## Goal
- 업무와 일상을 독립 메뉴·카테고리·집계로 관리하고 대시보드에 각각 요약한다.

## Scope
- [검토 문서](../../review/life/review-life-todos.md)의 승인된 범위를 따른다.
- 일상 할 일: 제목 필수, 카테고리/날짜/메모 선택, CRUD·완료/취소, 검색·카테고리·날짜 분류·서버 페이징.
- 사용자별 생활 카테고리 CRUD. 카테고리 삭제 시 할 일은 미분류로 유지.
- 기존 업무 할 일 경로와 데이터는 유지, 표시명과 대시보드 요약을 정리.

## Current state
- 2026-10-25 구현·로컬 검증 완료. 후속 문서 정리·각각 커밋 요청에 따라 일상 기능과 공용 화면의 연결 변경을 별도 커밋으로 묶는다.
- 실제 DB 적용·배포·실제 휴대폰 키보드 검증은 수행하지 않았다. 아래 적용 절차를 따른다.

## Action items
- [x] 독립 DDL·배포 SQL·VO/JVO·MyBatis Mapper 구현
- [x] 소유권·입력 검증·카테고리·할 일 CRUD·완료/집계 API 구현
- [x] 일상 할 일 페이지·폼·카테고리 관리·필터·상태 UI 구현
- [x] 업무 할 일 명칭·메뉴·즐겨찾기·대시보드 분리 요약 반영
- [x] 서비스·HTTP·SQL 검증 및 프런트 타입 검사·빌드·화면 확인
- [x] API·도메인·DB·페이지 문서와 검증 결과 갱신

## Done criteria
- 업무 없이 일상 항목을 만들고 조회·수정·완료/취소·삭제할 수 있다.
- 사용자별 카테고리를 관리하고 필터할 수 있다. 타인 데이터가 노출/수정되지 않는다.
- 일상 데이터가 업무 집계에 포함되지 않으며 메뉴/요약이 구별된다.
- 오류·로딩·빈 목록·저장 중·모바일 폼 동작이 검증된다.
- 적용 SQL을 제공하고 실제 적용 여부를 명확히 보고한다.

## Implementation
- `life` 전용 Controller/Service/Convert/DTO, share VO/JVO/policy, MyBatis Mapper/XML을 추가했다. 업무 API·트리·메일·통계 SQL은 변경하지 않았다.
- `tb_lf_todo_cat`와 `tb_lf_todo`는 계정·카테고리 복합 FK, 소프트 삭제, 소유자/날짜/카테고리 인덱스를 사용한다. 기준 DDL·기존 DB용 트랜잭션 스크립트·집계 DDL을 동기화했다.
- 페이지·API·타입·조회/편집 composable·등록 폼·카테고리 모달을 life feature로 분리하고 공통 UI를 재사용했다.
- 기존 업무 할 일과 새 일상 할 일의 메뉴·즐겨찾기·대시보드 요약을 구별한다. 간트와 완료 통계 메뉴에는 업무 접두사를 붙였다.
- 대시보드의 오늘 링크는 각 목록의 `?group=TODAY`로 연결된다. 일상 요약 오류는 업무 요약에 영향을 주지 않는다.
- 일상 요청의 JSON/날짜 변환 실패는 해당 Controller에서 400으로 처리하여 공통 오류 처리 동작은 유지했다.

## Validation — 2026-10-25

| 검증 | 결과 |
|------|------|
| API 회귀·신규 서비스/HTTP/Mapper | `:gw-home-api:test :gw-home-api:bootJar` 통과, 166 tests / 0 failures / 0 errors |
| 일상 SQL | `life-todo-sql.cjs` 12개 시나리오 통과: 소유권, 분류·필터·페이징, 수정·완료/취소·삭제, 카테고리 미분류 전환, FK, 마이그레이션 재실행 |
| 기존 스키마 회귀 | `renewal-schema.cjs` 12개 시나리오 통과, 새 테이블 포함 집계 DDL과 기존 데이터 보호 확인 |
| UI 정적 검증 | `npm run typecheck` 통과, Nuxt production build 통과 |
| UI 동작 | `tests/life-todos-ui.cjs` 통과: 제목만 등록, 저장 중 닫기 방지, 카테고리 CRUD, 일상 CRUD·완료/취소, 필터·검색·마지막 페이지 보정, 실패 시 입력 보존, 로딩/빈 목록/오류 재조회 |
| 반응형·대시보드 | 360/390/768/1440px 가로 넘침 없음, 390×430 모달 본문 스크롤·하단 액션, 업무/일상 요약 분리·독립 오류 처리·오늘 링크, 브라우저 런타임 오류 0건 |
| 변경 형식 | `git diff --check` 통과 |

- JDK 21의 기본 임시 소켓 연결 오류는 실행 환경에만 `-Djdk.net.unixdomain.tmpdir=C:\Users\GlassWorld\IdeaProjects\gw-home\.gradle`을 지정해 해결했다. JVM/운영 설정 파일은 변경하지 않았다.
- 기본 `.nuxt/dist/client` 생성 권한 오류를 피하기 위해 최종 Nuxt 검증은 `gw-home-api/build/verification/life-nuxt`와 `life-output`으로 분리해 빌드했다. 타입 검사는 활성 상태로 실행했다. 검증 보조 스크립트·산출물은 Git 제외 빌드 폴더에 있다.
- UI 테스트는 임시 로컬 API·가상 계정·headless Chrome을 사용했다. 실제 계정 데이터 저장, 외부 메일, 운영 DB 접속은 수행하지 않았다.
- 검증 화면: `gw-home-api/build/verification/life-screens/`의 일상 목록·대시보드·낮은 모달 PNG. 실제 휴대폰 키보드·터치는 별도 확인이 필요하다.

## Deployment
1. 계정 테이블이 있는 대상 `home` 스키마에 `gw-home-infra-db/src/main/resources/sql/ddl/life/life-todos-migrate.sql` 전체를 실행한다.
2. API·UI를 배포하고 일상 메뉴에서 생성/완료/카테고리 관리와 대시보드 요약을 확인한다.
3. 현재 실제 DB에는 미적용이다. 서버 재시작만으로 새 테이블이 생성되지 않는다.
