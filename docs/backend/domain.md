# 도메인 정의

## 이 문서의 목적

이 문서는 각 도메인이 어떤 책임을 가지는지와 도메인 경계를 어떻게 나누는지를 설명한다.

## 리뉴얼 진행 상태

- 2026-10-25 기준 [리뉴얼 계획](../../work/todo/common/todo-project-renewal.md) 2단계 통합 내 할 일 API·완료 시각 기록을 구현했다. 기존 DB에는 배포 전 추가 마이그레이션이 필요하다.
- `work`의 업무·TODO 트리·Git 연동은 유지한다. 3단계에서 일일·주간보고와 관리자 보고 운영 API·서비스·Mapper·모델 및 초안 생성을 제거했다.
- 내 할 일 통합 조회·완료 시각은 2단계, 서버 정기 요약·개별 메일은 4단계, 간트·완료 통계 읽기 API는 5단계에 구현했다. 사용자 요청으로 실제 메일 발송은 비활성 상태를 유지한다.
- 보고서 런타임 기능 제거와 `tb_wrk_dly_rpt`, `tb_wrk_dly_rpt_wrk_unit`, `tb_wrk_wkl_rpt`의 데이터·테이블 물리 삭제는 분리한다. 이번 리뉴얼에서는 기존 데이터를 보존한다.

## 도메인 목록

| 도메인 | 책임 |
|--------|------|
| `account` | 회원 가입, 탈퇴, 계정 정보 관리 |
| `auth` | 로그인, 로그아웃, 토큰 발급과 갱신 |
| `profile` | 프로필 조회와 수정 |
| `board` | 게시글 CRUD, 목록, 검색, 카테고리 |
| `comment` | 댓글과 대댓글 CRUD |
| `file` | 파일과 이미지 업로드, 저장 관리 |
| `notice` | 공지사항 목록, 상세, 운영 공지 관리 |
| `tag` | 태그 생성, 조회, 게시글-태그 매핑 |
| `favorite` | 좋아요 토글 |
| `vault` | 자격증명과 카테고리 보관 관리 |
| `work` | 업무, TODO 트리, 내 할 일 통합 조회, 메일 알림, Git 연동 |
| `admin` | 회원 관리, 제재, 운영 기능 |

## 도메인 경계 원칙

- `account`, `auth`, `profile`은 서로 다른 책임으로 분리한다
- `auth`는 `account`를 참조할 수 있지만 역방향 참조는 허용하지 않는다
- `board`와 `admin`은 분리한다
- `admin`은 운영 목적으로 `board`를 참조할 수 있지만 역방향 참조는 허용하지 않는다
- `file`은 독립 도메인으로 유지하고 다른 도메인은 URL만 저장한다
- 도메인 간 직접 호출은 최소화한다

## 도메인별 테이블 축약

| 도메인 | 축약 | 예시 |
|--------|------|------|
| `account` | `mbr_acct` | `tb_mbr_acct` |
| `auth` | `auth` | `tb_auth_tkn` |
| `profile` | `mbr_pfl` | `tb_mbr_pfl` |
| `board` | `brd` | `tb_brd_pst`, `tb_brd_ctgr` |
| `comment` | `brd_cmt` | `tb_brd_cmt` |
| `file` | `file` | `tb_file` |
| `notice` | `ntc` | `tb_ntc` |
| `tag` | `tag` | `tb_tag`, `tb_brd_tag` |
| `favorite` | `fvt` | `tb_fvt` |
| `vault` | `vlt` | `tb_vlt_cat`, `tb_vlt_crd` |
| `work` | `wrk` | `tb_work_unit`, `tb_wrk_todo`; 보고서 테이블은 종료된 기능의 보관 스키마 |
| `life` | `lf` | `tb_lf_todo`, `tb_lf_todo_cat` |
| `admin` | `adm` | `tb_adm_log` |

## 설계 시 참고 포인트

- 새로운 기능이 기존 도메인 책임에 포함되는지 먼저 판단한다
- 관리 기능이라고 해서 무조건 `admin`에 넣지 말고 운영 목적 여부를 확인한다
- 파일 저장이 필요하면 파일 자체는 `file`, 참조만 각 도메인에 둔다
- 공지, 금고, 업무 기능은 각각 `notice`, `vault`, `work` 도메인에 우선 배치한다

## 보고서 종료와 보존 경계 (3단계)

- 종료 API: `/api/v1/daily-reports/**`, `/api/v1/weekly-reports/**`(공개 조회·원본 조회·AI 초안 포함), `/api/v1/admin/daily-reports/**` 및 각 기본 경로. 인증·권한 검사 후 공통 `ApiResponse.fail`의 404를 반환한다. 미인증·관리자 접근 제한은 기존 SecurityConfig를 따른다.
- `DailyReportMapper`와 `WorkUnitMapper.refreshWorkUnitUsageStats`를 제거해 런타임 SQL이 보고서 테이블을 참조하지 않는다. 보고서 전용 OpenAI 설정·프롬프트·생성 상수도 제거했다.
- `tb_wrk_dly_rpt`, `tb_wrk_dly_rpt_wrk_unit`, `tb_wrk_wkl_rpt`와 업무의 `use_cnt/last_used_at` 값은 보존한다. 기존 업무 응답 필드는 호환을 위해 남겨 두지만 더 이상 갱신·표시·정렬에 사용하지 않으며 TODO 실적이 아니다. `recent/frequent` 정렬 요청은 최근 수정순으로 호환한다.
- WorkGitController/Service/Mapper, WorkGitCommitClient, 토큰 암호화와 Git 데이터는 유지한다. 커밋 API의 기존 날짜 매개변수 `reportDate`도 호환을 위해 유지한다.
- 선택적 즐겨찾기 정리: [remove-retired-report-favorites.sql](../../gw-home-infra-db/src/main/resources/sql/ddl/profile/remove-retired-report-favorites.sql). 백업 후 정확히 네 보고서 메뉴 경로만 제거하고 순서를 보존한다. 잘못된 JSON·배열이 아닌 값은 건너뛴다. 재실행 시 변경 없는 행의 수정 시각도 유지한다.
- 이번 작업은 실제 DB에 접속하거나 마이그레이션을 실행하지 않았다. 보관용 보고서 DDL을 기존 DB에 다시 실행할 필요가 없다. 물리 삭제·이관·백업 반출은 별도 작업이다. 롤백은 이전 앱 버전과 프로필 백업을 이용하며 보고서 데이터 복원은 필요하지 않다.

## 간트차트와 완료 통계 (5단계)

- `WorkInsightsController/Service`는 로그인 계정의 읽기 전용 일정·통계 API를 제공한다. 입력 기간·업무 UUID·IANA 시간대를 검증하고 `WorkInsightsConvert`가 외부 응답과 빈 기간 구간을 조립한다.
- `WorkTodoMapper.ownedWorkTodoSelect`를 기존 목록/요약/메일과 공유한다. 소유자·삭제·활성·업무 필터와 오늘/예정/지연/완료/미정 분류는 동일하다. 쓰기 API와 기존 트리·cmpl_at 처리 규칙은 바꾸지 않았다.
- 간트는 기간과 겹치는 일정, 미지정 항목, 계층에 필요한 조상만 반환한다. 재귀 SQL은 UNION으로 순환을 종료하고 응답 정렬은 반복 방문 방지·비재귀 순회를 사용한다. 최대 93일·2,000행이며 초과 시 좁혀 조회하도록 안내한다.
- 업무 전체 일정은 모든 하위 TODO의 알려진 최소/최대 날짜를 사용하고, 한쪽이라도 날짜 없는 항목이 있으면 partial_schedule을 표시한다. 업무 전체 진행률은 최하위 완료 비율이며 업무의 수동 상태는 별도 필드다.
- 통계는 선택 업무·활성 조건의 현재 최하위 현황과 완료 기간의 추이를 별도로 계산한다. 읽기 전용 REPEATABLE_READ로 같은 요청의 업무 현황·추이 일관성을 보장한다. 기본 비활성 포함으로 지난 업무도 확인할 수 있다.
- 완료 추이는 cmpl_at의 현지 날짜를 일·월요일 시작 주·월로 묶는다. 날짜 경계는 현지 첫날 이상·마지막 날의 다음날 미만이며 DST를 고려한다. 완료일 미상은 현재 완료율에 포함하고 기간 실적에서는 제외한다. 취소·재완료·삭제·leaf 변경에 따라 과거 표시 건수가 달라지는 현재 유효 완료 방식이다.
- 신규 테이블/컬럼·완료 이벤트 이력은 추가하지 않았다. 메일은 기본 비활성 상태를 유지한다. 입력·응답 필드는 [API 계약](../common/api-contract.md#간트완료-통계-api-5단계), 로컬 검증과 한계는 [실행 계획](../../work/todo/common/todo-project-renewal.md#5단계-구현-결과와-검증)을 따른다.

## 내 할 일 메일 알림 (4단계)

- **현재 운영 결정 (2026-10-25): 사용자 요청으로 실제 메일 발송을 사용하지 않는다.** `WORK_NOTIFICATIONS_ENABLED=false`를 유지하며 제공자 연결·인증정보 설정·실제 수신 검증은 보류한다. 아래 내용은 구현 보존 및 향후 재개용 기록이다.
- 비활성 시 예약 작업자는 실행을 건너뛰고, 확인 메일 요청과 SMTP 전송도 준비 상태 검사에서 차단한다. 기존 TODO 쓰기의 예약 동기화는 알림 테이블을 사용하므로 발송 보류와 신규 SQL 적용 여부는 구분한다.
- `/settings#work-notifications`에서 수신 주소 확인, 두 알림의 개별 활성화, IANA 시간대, 매일/평일 요약과 발송 시각을 설정한다. 기본값은 매일 09:00, Asia/Seoul, 두 알림 꺼짐이다.
- 수신 주소 확인 코드는 해시로만 저장하며 10분 유효, 5회 확인 시도, 60초 간격·시간당 5회 발급 제한이다. 실패 횟수는 오류 응답으로 롤백하지 않는다. 주소 변경 시 확인 상태·활성화를 해제하고 대기 예약을 취소한다.
- 개별 알림은 활성 업무의 본인 미완료 최하위 TODO에 미래 절대 시각으로 저장한다. TODO 상세에서 설정 시간대의 시각을 입력하면 UTC 순간으로 변환한다. 완료·삭제·부모 전환·업무 비활성화·개별 알림 해제는 대기 건을 취소하며, 완료 취소는 예약을 되살리지 않는다.
- TODO 시작일/마감일 변경은 지정 발송 시각을 이동시키지 않는다. 미래 예약만 버전을 올려 다시 생성하고, 이미 지난 시각은 취소한다. 제목·내용은 발송 직전 최신 값을 사용한다.
- 요약은 기존 `WorkTodoMapper.myWorkTodoSource`의 소유권·활성·최하위·날짜 분류를 재사용한다. 지연 → 오늘 → 예정 중 3일 이내 마감으로 중복 없이 구성한다. 대상 0건은 건너뛰며 200건 초과는 잘림을 표시하고 전체 목록으로 연결한다. 날짜가 없는 항목은 요약에서 제외한다.
- 목록·트리는 기존 기기 시간대, 메일은 저장한 발송 시간대를 명시해 계산한다. 같은 시간대와 조회 조건에서 날짜 분류가 일치한다. 시작일/마감일 DATE는 변환하지 않고 완료 시각과 예약 시각만 순간으로 다룬다.
- 정기 요약의 DST 공백은 그 간격만큼 뒤로 이동하고 중복 시각은 첫 번째 오프셋을 사용한다. 개별 시각 입력은 공백을 거부하고 중복 시각은 첫 번째 오프셋으로 변환해 안내한다.
- 상세 링크는 `/work/todos/{workUnitUuid}?todo={todoUuid}`이며 기존 인증·소유권 검사를 거친다. 미로그인은 기존 로그인 → 대시보드 동작을 유지하므로 로그인 후 메일 링크를 다시 열면 된다.

### 발송·중복·복구 기준

- 서버가 30초 간격으로 실행한다. 한 번에 요약 최대 100계정을 계획하고 대기 메일을 한 건씩 최대 20개 처리한다. 실제 발송은 설정 시각 이후 첫 실행과 대기열 처리 시점이다. 서버가 중지되어 있으면 보낼 수 없다.
- `(소유자, 발송 키)` UNIQUE와 `FOR UPDATE SKIP LOCKED`로 동일 회차의 중복 생성을 막는다. 개별 키는 예약 UUID+버전, 요약 키는 현지 날짜다. 이미 발송한 날짜의 요약은 설정을 바꾸어도 다시 생성하지 않는다.
- 계정 advisory transaction lock → 기존 업무 행 lock 순서로 TODO 쓰기·설정 변경·발송을 직렬화한다. 외부 호출 동안 계정 잠금을 유지하므로 완료/삭제가 먼저 커밋되면 발송되지 않는다. 발송이 먼저 진행 중이면 완료/삭제가 접수 결과까지 대기한다. 이미 SMTP가 접수한 메일은 회수하지 못한다.
- 상태 흐름: `PENDING/RETRY → CLAIMED → SENDING → SENT/RETRY/FAILED/UNKNOWN`. 무효 예약은 `CANCELLED`.
- 확보 토큰과 2분 lease를 검증하고 SMTP 호출 **이전**에 `SENDING`을 별도 트랜잭션으로 커밋한다. 만료 `CLAIMED`만 재확보하며 만료 `SENDING`(10분)은 `UNKNOWN`으로 남겨 자동 재발송하지 않는다. 발송 결과도 별도 커밋한다.
- 명시적 4xx 임시 거부/연결 거부/DNS 연결 실패만 총 3회, 1분·5분 간격으로 재시도한다. 5xx/인증 거부는 `FAILED`, 접수 가능성이 있는 연결 유실/타임아웃은 `UNKNOWN`이다. SENT는 SMTP 접수이며 최종 수신함 도착/이후 반송까지 보장하지 않는다.
- 예약보다 24시간 넘게 지연된 대기 건은 `EXPIRED`로 종료한다. 요약은 현지 오늘 회차만 복구하고 과거 여러 날짜를 몰아서 보내지 않는다.
- 이력에는 식별자·시각·횟수·정규화된 결과 코드만 저장한다. 메일 본문·SMTP 비밀번호·확인 코드 원문을 로그나 이력에 저장하지 않는다. HTML 입력을 이스케이프하며 텍스트 본문도 함께 전송한다.

### SMTP 운영 설정

| 환경변수 | 기본값 / 의미 |
|----------|---------------|
| `WORK_NOTIFICATIONS_ENABLED` | `false` / 서버 발송 허용 |
| `WORK_SMTP_HOST`, `WORK_SMTP_PORT` | 호스트, `587` |
| `WORK_SMTP_USERNAME`, `WORK_SMTP_PASSWORD` | 제공자 계정과 비밀 저장소에서 주입할 비밀번호 |
| `WORK_SMTP_STARTTLS`, `WORK_SMTP_SSL` | `true`, `false`; 465 SSL 사용 시 각각 false, true |
| `WORK_MAIL_FROM` | 제공자에 등록·허용된 발신 주소 |
| `WORK_MAIL_FROM_NAME` | 발신 표시 이름. UTF-8로 MIME 인코딩 |
| `WORK_SMTP_AUTH_MODE` | `PASSWORD` 또는 `MICROSOFT365_OAUTH2` |
| `WORK_M365_TENANT_ID`, `WORK_M365_CLIENT_ID` | Microsoft 365 테넌트/발송 앱 UUID |
| `WORK_M365_CLIENT_SECRET` | OAuth 앱 비밀의 값. OAuth 모드에서 SMTP 비밀번호 대신 사용 |
| `WORK_MAIL_APP_URL` | 사용자가 접근하는 앱 기본 URL. 운영에서는 HTTPS 사용 |
| `WORK_MAIL_POLL_DELAY_MS` | `30000` / 작업 완료 후 다음 실행 간격 |

- 연결/읽기/쓰기 타임아웃은 각각 10초다. STARTTLS가 켜져 있으면 TLS 협상을 필수로 하고 인증서 서버 이름을 검사한다. 실제 비밀은 저장소·문서에 기록하지 않는다.
- 현재는 발송을 활성화하지 않는다. 향후 사용자가 발송 재개를 요청하면 신규 SQL 적용 → 발송 비활성 상태로 서버·UI 배포 → 제공자/발신 주소/앱 URL 환경설정 → 서버 발송 스위치 켜기 → 사용자 주소 확인·알림 활성화 순서로 적용한다. 이번 구현에서는 실제 DB와 외부 SMTP를 변경하지 않았다.
- 작업자는 외부 호출 중 잠금을 가진 연결 외에 상태 커밋용 연결을 사용하므로 단일 연결 풀을 사용하지 않는다. 기본 Hikari 풀을 유지하고 다중 서버/동시 요청 시 풀 여유와 대기량을 배포 환경에서 확인한다.
- 운영 점검은 `FAILED/UNKNOWN`, 만료 lease, 지연 대기열을 확인한다. `UNKNOWN`은 제공자 로그·수신함 확인 없이 `PENDING`으로 바꾸지 않는다. 장애 중단은 모든 인스턴스에서 서버 발송 스위치를 끄고 재시작한다.
- 구현 근거: [Spring Boot 메일 설정·타임아웃](https://docs.spring.io/spring-boot/reference/io/email.html), [Angus SMTP 응답/전송 속성](https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/package-summary.html).

### 정종태 계정의 Microsoft 365 연결 준비 기록 — 현재 보류

- 사용자 지정 발신 표시: **정종태**, 발신·인증 계정과 본인 수신 주소: **jjt90@genexon.co.kr**. 수신 주소는 본인 앱 설정에서 저장·확인하며 서버 전체 사용자의 수신 주소로 고정하지 않는다.
- 2026-10-25 공개 MX 조회 결과는 `genexon-co-kr.mail.protection.outlook.com`이다. Microsoft 365 보호 서버를 사용한다는 근거이며, 실제 메일함 위치·테넌트 발송 허용 정책까지 확인한 것은 아니다.
- [로컬 설정 예제](../../gw-home-api/.env.mail.microsoft365.example)에 계정·발신 이름을 입력하고 비밀값은 비워 뒀다. 이 파일은 Git 제외 대상이며 배포 산출물에 포함되지 않는다. Spring Boot가 자동으로 읽지 않으므로 IDE/서버 환경변수로 주입해야 한다.
- Microsoft 365 모드는 `smtp.office365.com:587`, 필수 STARTTLS, 발신 주소=인증 계정, XOAUTH2만 허용한다. 기존 PASSWORD 방식은 다른 제공자와 호환을 위해 유지한다. 본 연결은 일반 비밀번호/앱 비밀번호 설정을 사용하지 않는다.
- 앱 토큰은 고정 Microsoft 로그인 엔드포인트에서 SMTP scope `https://outlook.office365.com/.default`와 client credentials로 얻는다. 메모리 캐시에서 만료 60초 전 갱신하고 토큰·비밀·응답 원문을 로그/DB/API에 노출하지 않는다. 토큰 발급 전 통신/429/5xx 실패만 제한 재시도, 거부/잘못된 설정은 실패로 기록한다.
- SMTP 인증 거부 시 캐시를 폐기해 다음 발송에서 새 토큰을 얻는다. SMTP DATA 이후 접수불명 처리와 기존 발송 키·토큰/lease 규칙은 유지한다.

관리자에게 확인할 항목:

1. 이 계정이 Exchange Online 메일함이고 조직 정책상 해당 앱의 SMTP AUTH 사용이 허용되는지 확인한다.
2. 전용 단일 테넌트 앱, Office 365 Exchange Online의 SMTP.SendAsApp 애플리케이션 권한과 관리자 동의를 준비한다.
3. Exchange 서비스 주체를 등록하고 대상 메일함에 필요한 접근/SendAs 권한을 한정한다. 앱 등록 Object ID와 Enterprise Application Object ID를 혼동하지 않는다. 메일 읽기용 IMAP/POP/Graph 권한은 이 앱에서 요청하지 않는다.
4. 테넌트 ID, 앱(client) ID, 클라이언트 비밀의 **값**과 만료 시각을 운영 담당자에게 전달한다. 비밀값은 실행 환경의 비밀 저장소에 넣고 갱신 시 앱을 재시작한다.
5. 사용자가 접근하는 앱 HTTPS URL을 정하고 신규 DB SQL 적용 후 환경변수를 주입한다. 서버 발송을 켠 다음 본인 설정에서 주소 확인 → 정기 요약/개별 알림 활성화 → 자기 주소 수신을 점검한다.

회사 관리자 권한 변경·앱 등록·실제 발송은 이번 로컬 작업에서 수행하지 않았다. 조직에서 SMTP를 금지하면 정책을 임의로 완화하지 않고 관리자 승인 발송 경로를 먼저 확인한다. Microsoft 공식 [OAuth 앱 등록·메일함 권한 안내](https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth)와 [SMTP 서버·TLS 설정](https://learn.microsoft.com/en-us/exchange/mail-flow-best-practices/how-to-set-up-a-multifunction-device-or-application-to-send-email-using-microsoft-365-or-office-365)을 기준으로 운영 적용한다.
