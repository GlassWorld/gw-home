# 백엔드 쿼리·공통 소스 리팩토링 계획

- 상태: 1·2차 구현 및 로컬 검증 완료. 운영 DB 성능·다중 연결 검증은 미실시.
- 문서 정리 (2026-10-25): 사용자 요청에 따라 관련 소스·검증·문서를 별도 커밋으로 묶는다. 아래 커밋 제외/미실시 설명은 당시 실행 범위이며 이번 커밋 요청을 제한하지 않는다.
- 커밋 경계: 공통 페이징·게시판/공지/관리자 개선을 먼저 반영한다. 기존 보고서가 참조하는 infra-db `PageSortSupport`의 최종 삭제와 신규 업무 목록의 `PageResponse.of` 적용은 후속 리뉴얼 커밋에 포함한다.

## Goal

- 반복 SQL·페이징·모델 변환을 공통화하고 기존 API 동작을 유지한다.

## Scope

- 아래는 1차 완료 범위이며, 사용자가 승인한 후속 범위는 문서 하단의 2차 개선 항목을 따른다.
- 수정: `BoardMapper.xml`, `NoticeMapper.xml`, `AccountMapper.java`의 offset 타입.
- 이동/수정: infra-db `PageSortSupport` → share `common/query/PageSortSupport`, `PageSortSearchVo`, `PageResponse`.
- 수정: `BoardConvert`, `NoticeConvert`, `BoardPostListRequest`, `BoardService`, `NoticeService`, `AdminService`, `AdminAccountService`, `WorkTodoInboxService`, `WorkNotificationService`의 관련 목록 처리.
- 검증: 공유 페이징 테스트, 게시판·공지 변환/서비스 회귀, 관리자 계정 offset 회귀, 실제 MyBatis SQL 바인딩과 격리 SQL 실행 검증.
- 회귀 보완: 기존 `GlobalExceptionHandlerTest`가 상세 메시지를 입력하고 기본 메시지를 기대해 실패했다. HEAD와 현재 구현이 모두 상세 메시지 우선임을 확인했으므로 런타임 코드는 유지하고, 기본 메시지 fixture를 바로잡고 상세 메시지 보존 테스트를 분리한다.
- 제외: 새로운 기능·API 필드·DB 스키마·권한 변경, 프런트, 운영 DB/메일/배포, 커밋.

## Current State

- [검토 문서](../../review/common/review-backend-query-refactor.md)에 중복과 영향 범위를 기록했다.
- 기존 리뉴얼 변경이 많으므로 변경 전 파일을 기준으로 필요한 부분만 편집한다.
- 페이지/크기 기본값 1/20, 업무 기능의 별도 크기 제한, 검색어 와일드카드와 공백 처리 차이는 유지한다.

## Action Items

- [x] 공통 페이징 유틸 위치·offset·응답 생성을 정리하고 사용처를 갱신한다.
- [x] 게시판·공지 변환 책임과 중복 문자열 처리를 기존 convert/util로 모은다.
- [x] Mapper SQL 조각을 추출해 중복 컬럼과 목록/건수 필터를 공유한다.
- [x] 경계값·검색·삭제·집계·바인딩 회귀를 검증한다.
- [x] 백엔드 전체 테스트 및 빌드 후 결과와 제한을 기록한다.

## Done Criteria

- 기존 검색 종류/기본값/응답 구조와 삭제 제외 조건이 유지된다.
- 큰 페이지 offset이 overflow 없이 long으로 SQL에 바인딩된다.
- 목록과 건수의 검색 조건이 공유되고 테스트로 검증된다.
- 기존 변경은 보존되고 이번 변경·검증 범위가 명확히 기록된다.

## Result (2026-10-25)

- 게시판 상세/좋아요·댓글 집계/검색 필터와 공지 상세/요약/검색 필터를 Mapper 내부 SQL 조각으로 추출했다. 기존 statement ID와 파라미터 계약을 유지했다.
- 게시판의 목록·건수는 동일한 EXISTS 기반 작성자 검색을 사용한다. 프로필 없는 글의 제목·본문 검색, 삭제 제외, 카테고리·와일드카드 검색을 유지했다.
- `PageSortSupport`를 share.common.query로 이동하고 offset을 long으로 계산한다. 관리자 계정 Mapper까지 long 바인딩을 적용했다.
- 7개 목록 처리에서 `PageResponse.of`를 사용한다. 전체 페이지 수는 정수 연산을 사용하고 기존 int 응답 상한을 유지한다.
- 게시판/공지 검색 모델과 공지 생성/수정 모델은 기존 Convert에서 구성한다. 공지/계정의 중복 trim은 StringUtil을 재사용한다. 수정한 목록 서비스에는 실패 로그를 보완했다.
- 기존 공통 예외 테스트의 입력/기대값 불일치는 HEAD에서도 동일한 상세 메시지 우선 동작임을 확인했다. 기본 메시지 fixture 수정과 상세 메시지 보존 테스트 추가만 수행했다.

### Verification

| 검증 | 결과 |
|------|------|
| API 테스트 | 138개, 실패·오류·스킵 0 |
| share 테스트 | 9개, 실패·오류·스킵 0 |
| SQL 실행 | 실제 MyBatis BoundSql의 검색/목록/건수/상세/대시보드 22개 격리 PGlite 시나리오 통과 |
| 페이징 경계 | int 최대 페이지의 JDBC setLong 바인딩, 큰 count의 부동소수점 오차 방지, 0건/정확한 배수/마지막 페이지/초과 페이지 확인 |
| 빌드 | `:gw-home-api:bootJar` 성공 |
| diff | `git diff --check` 통과 |

재실행 명령 (저장소 루트, Java 21):

```powershell
./gradlew.bat :gw-home-api:test :gw-home-share:test :gw-home-api:bootJar --offline --no-daemon
node gw-home-api/src/test/resources/query/list-refactor-sql.cjs gw-home-api/build/verification/node_modules/@electric-sql/pglite
```

- SQL 스크립트의 마지막 인자는 로컬 PGlite 모듈 경로다. 새 운영 의존성은 추가하지 않았다. JDBC 문자열 바인딩의 타입을 재현하기 위해 격리 실행에서는 문자열 파라미터에 varchar 캐스트를 적용한다.
- 이 Windows 환경에서는 Gradle 캐시 접근과 JDK 임시 소켓 경로 설정이 필요했다. 실행 세션에만 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/GlassWorld/IdeaProjects/gw-home/.gradle/tmp`를 적용해 해결했다.
- 운영 PostgreSQL/JDBC 접속, 대용량 실행 계획·성능 측정은 수행하지 않았다. 이번 검증은 동작 보존과 바인딩·경계값 검증이며 성능 향상 수치를 주장하지 않는다.
- 기존 Gradle deprecation과 테스트 unchecked 경고는 남아 있다. DB 변경·메일 발송·배포·커밋은 수행하지 않았다.

## 2차 개선 계획 (2026-10-25)

### 범위와 파일

- 사용자 요청에 따라 검토에서 확인한 6개 개선을 진행한다. 공통 조회 최대 크기는 100으로 보정하며 기본값은 유지한다.
- 공통: `PageSortSupport`, `PaginationTest`.
- 조회: `AdminMapper.xml`, `AccountMapper.java/XML`, `BoardMapper.xml`, `NoticeMapper.xml`, `NoticeService` 대시보드 limit.
- 계정 목록: 신규 `share/vo/account/AcctSmryVo`, `AdminAccountConvert`.
- 상세/첨부: `BoardService`, `NoticeService`, `BoardMapper.java/XML`, `FileMapper.java/XML`, `BoardConvert`.
- 검증: 기존 `BoardServiceTest`, `ListQueryRefactorTest`, `ListMapperContractTest`, `list-refactor-sql.cjs`를 확장한다.
- 문서: 현재 review/todo와 `docs/common/api-contract.md`의 목록 정책.
- 기존 기능의 권한·응답 필드·삭제 제외·조회수 증가·첨부 순서/트랜잭션을 유지한다. UUID는 표준 형식의 대소문자를 정규화하고 잘못된 첨부 UUID는 BAD_REQUEST로 처리한다.

### 실행 항목

- [x] 관리자 목록 LEFT JOIN 및 목록/건수 필터 공통화로 누락을 수정한다.
- [x] 목록 size와 대시보드 limit 상한을 적용하고 실제 응답 size/offset의 일치를 확인한다.
- [x] 동률 정렬의 보조 PK를 추가하고 페이지 중복·누락을 고정 데이터로 검증한다.
- [x] 관리자 계정 목록에서 사용하지 않는 인증 필드를 제거하고 전용 조회 모델을 사용한다.
- [x] 상세 조회를 UPDATE→SELECT로 줄이고 404·증가 횟수·원자적 증가와 삭제 제외 조건을 검증한다.
- [x] 첨부 일괄 조회·검증·저장으로 호출을 줄이고 소유권·순서·중복·실패 시 보존을 검증한다.
- [x] 전체 백엔드 테스트·bootJar·격리 SQL 실행 후 결과를 기록한다.

### 완료 기준

- 카테고리 없는 글도 관리자 목록과 count에서 일치한다.
- 모든 대상 목록에서 최대 size=100과 안정적인 보조 정렬이 적용된다.
- 계정 목록 모델/SQL에 pwd·otpSecret이 없고 기존 목록 응답은 유지된다.
- 게시글/공지 상세에서 조회수는 한 번 증가하고 상세 SELECT는 한 번 수행된다. 미존재/삭제 대상은 404를 유지한다.
- 첨부 10개 교체의 조회/삭제/저장 호출은 3회이며, 소유권·존재 검증 실패 시 기존 연결을 변경하지 않는다.
- 전체 테스트와 실제 BoundSql 격리 실행 결과, 운영 DB 성능 측정 미실시를 구분해 기록한다.

### 2차 결과

| 항목 | 반영 내용 |
|------|-----------|
| 관리자 목록 | 카테고리/계정 LEFT JOIN, 목록과 count 필터 공유. 카테고리·연관 계정이 없는 글도 포함하고 삭제/검색 정책 유지 |
| 공통 조회 크기 | DEFAULT_SIZE=20, MAX_SIZE=100. 초과 size는 100으로 보정하며 응답 size와 offset을 일치시킴. 대시보드 공지 기본 5/최대 100 |
| 안정적 정렬 | 게시판·공지·관리자 회원/게시글·계정의 기존 정렬 뒤 PK DESC 추가. 대시보드 공지도 같은 기준 적용 |
| 계정 목록 모델 | `AcctSmryVo` 도입 및 목록 SELECT/Mapper/Convert 연결. pwd/otp_secret 등 미사용 인증 컬럼 제외. 인증·계정 상세 AcctVo 유지 |
| 상세 조회 | 삭제되지 않은 대상 UPDATE 영향 건수로 404 판단, 상세 SELECT는 한 번만 실행. 기존 트랜잭션과 `view_cnt = view_cnt + 1` 유지 |
| 첨부 교체 | UUID 정규화/중복 제거 → 파일 일괄 조회 → 요청 순서로 소유권·존재 검증 → 연결 삭제 → 다중 행 INSERT. 저장 모델은 BoardConvert에서 구성 |

검증 결과:

- `:gw-home-api:test`: **149개**, 실패·오류·스킵 0.
- `:gw-home-share:test`: **10개**, 실패·오류·스킵 0.
- `:gw-home-api:bootJar`: 성공. 최종 테스트·빌드 명령은 1차 기록과 동일하다.
- 실제 MyBatis BoundSql로 실행한 격리 SQL **35개 시나리오** 통과. 기존 22개에 관리자 카테고리/계정 누락·검색·삭제 포함, 5종 동률 페이징, 조회수 증가/삭제 제외, 파일 일괄 조회/빈 입력, 첨부 순서/일괄 INSERT 실패 후 롤백을 추가했다.
- 서비스 테스트에서 조회수 UPDATE 및 상세 SELECT 각 1회, 첨부 10개의 조회/삭제/INSERT 각 1회, UUID 대소문자·중복·빈 항목·잘못된 형식, 타인/누락/삭제 파일의 기존 연결 보존을 확인했다.
- `git diff --check` 통과. 쿼리 호출 수 감소는 테스트로 확인한 값이며 운영 응답 시간 개선 수치를 의미하지 않는다.
- SQL fixture는 기존 OTP 스크립트의 적용 구문만 실행한다. 해당 스크립트에 함께 있는 롤백 구문은 fixture에서 제외하며 원본 DDL은 변경하지 않았다.
- 실제 DB 접속·다중 JDBC 연결 동시성·대용량 성능 측정·프런트 검증은 수행하지 않았다. 기존 Gradle deprecation/unchecked 경고와 운영 배포 범위는 그대로다.
