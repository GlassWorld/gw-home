# 백엔드 개발 규칙

## 이 문서의 목적

이 문서는 백엔드 구현 시 반드시 지켜야 할 구조와 네이밍 기준을 설명한다.

## 핵심 원칙

- JPA와 Querydsl은 사용하지 않는다
- 데이터 접근은 MyBatis Mapper 인터페이스와 XML로만 처리한다
- API에는 내부 PK(`_idx`)를 노출하지 않는다
- 외부 식별자는 `uuid`를 사용한다
- 시간 컬럼은 `TIMESTAMPTZ`를 사용한다

## Mapper 규칙

- Mapper 인터페이스는 `{project}-infra-db` 모듈의 `mapper.{domain}` 패키지에 둔다
- Mapper XML은 `{project}-infra-db/src/main/resources/mapper/{domain}/` 아래에 둔다
- XML `namespace`는 Mapper 인터페이스의 전체 경로를 사용한다
- 동적 쿼리는 XML의 `<if>`, `<choose>`, `<foreach>`로 처리한다
- 조회 결과는 기본적으로 `resultType`을 사용한다
- `resultMap`은 컬럼 별칭 충돌이나 중첩 매핑처럼 불가피한 경우에만 사용한다

## 모델 규칙

- 단일 테이블 모델은 `{Domain}Vo`
- 조인/확장 조회 모델은 `{Domain}Jvo`
- `VO`, `JVO`는 `share` 모듈에서 관리한다
- 공통 PK, UUID, 감사 컬럼은 `BaseVo`에 둔다
- DB 컬럼명이 `{table}_idx`, `{table}_uuid`여도 내부 식별자는 `id`, `uuid` 기준으로 맞춘다
- `VO`, `JVO`는 테이블 컬럼 기준 camelCase를 사용한다

## Service 규칙

- 서비스는 비즈니스 흐름과 검증에 집중한다
- 조회 전용 메서드는 읽기 전용 트랜잭션을 사용한다
- `toResponse`, `toVo` 같은 변환 작업은 서비스 내부에 두지 말고 도메인별 `convert` 클래스로 분리하는 것을 기본 원칙으로 한다
- 응답 DTO 조립과 저장용 모델 변환은 도메인별 변환 책임으로 분리한다
- 널 체크, 형변환, 간단 검증은 `gw-home-share` 공통 유틸을 우선 사용한다
- 에러는 `ErrorCode`를 기준으로 공통 처리하고, 상세 메시지는 예외적인 경우에만 최소화해서 사용한다
- 서비스 상단 상수 중 여러 서비스나 도메인에서 재사용 가능한 정책 값은 `gw-home-share`의 공통 영역으로 올릴 수 있는지 먼저 검토한다
- 서비스 메서드에는 진입, 완료, 실패 로그를 남긴다

## Controller 규칙

- 컨트롤러는 HTTP 요청 입구와 응답 반환에 집중한다
- 컨트롤러 `public` 메서드마다 역할을 설명하는 한글 주석 1줄을 작성한다
- 컨트롤러 주석은 요청 목적과 호출 대상을 드러내는 문장으로 작성한다
- 인증 사용자, 관리자, 공개 API 여부가 중요하면 주석에 함께 드러낸다

예시:

```java
// 로그인 사용자의 일일보고 목록을 조회한다.
public ApiResponse<PageResponse<DailyReportResponse>> getDailyReports(...) {
```

```java
// 관리자용 공지사항을 삭제한다.
public ApiResponse<Void> deleteNotice(...) {
```

## 주석과 로그 규칙

- 코드 주석은 모두 한글로 작성한다
- 컨트롤러와 서비스의 `public` 메서드에는 역할을 설명하는 한글 주석 1줄을 둔다
- 복잡한 분기, 예외 조건, SQL 의도처럼 읽는 사람이 바로 이해하기 어려운 부분에만 추가 설명을 남긴다
- 서비스 로그는 최소한 시작, 완료, 실패 3단계를 남긴다
- 검증이나 연관 데이터 해석이 중요한 메서드는 중간 진행 로그를 필요한 범위에서 추가할 수 있다
- 실패 로그에는 원인과 식별 가능한 입력값을 함께 남기되, 민감정보는 기록하지 않는다

예시:

```java
// 로그인 사용자의 주간보고를 생성한다.
public WeeklyReportResponse createWeeklyReport(String loginId, CreateWeeklyReportRequest request) {
    log.info("createWeeklyReport 시작 - loginId: {}, request: {}", loginId, request);
    try {
        ...
        log.info("createWeeklyReport 완료 - loginId: {}, uuid: {}", loginId, response.uuid());
        return response;
    } catch (BusinessException exception) {
        log.error(
                "createWeeklyReport 실패 - loginId: {}, 원인: {}, detailMessage: {}",
                loginId,
                exception.getMessage(),
                exception.getDetailMessage(),
                exception
        );
        throw exception;
    }
}
```

## 프로세스 동작 가이드

- 서비스 메서드는 가능한 한 아래 순서를 기준으로 작성한다

1. 시작 로그를 남긴다
2. 인증 주체나 선행 엔티티를 조회한다
3. 요청값을 검증하고 필요한 공통 유틸로 정규화한다
4. 저장용 `VO` 또는 조회용 입력 모델을 조립한다
5. Mapper 또는 외부 연동을 호출한다
6. 필요하면 저장 결과를 재조회하거나 연관 데이터를 보강한다
7. `convert` 클래스로 응답이나 반환 모델을 변환한다
8. 완료 로그 후 반환한다
9. 예외가 발생하면 실패 로그 후 다시 던진다

- 모든 서비스가 완전히 같은 순서를 따를 필요는 없지만, 검증과 변환 책임이 서비스 안에서 뒤섞이지 않도록 유지한다
- 컨트롤러는 요청 수집과 서비스 호출에 집중하고, 비즈니스 분기와 변환 상세 구현은 서비스나 `convert`로 내린다

## 공통화 기준

- 널 체크, 공백 정리, 날짜 순서 검증, 단순 타입 기본값 치환처럼 반복되는 로직은 `gw-home-share` 공통 유틸로 우선 모은다
- 여러 서비스나 도메인에서 재사용 가능한 기술 유틸과 보안 컴포넌트는 `gw-home-share` 배치를 우선 검토한다
- `gw-home-api/util`에는 API 조합 전용 책임만 남기고, `share` 의존만으로 성립하는 공통 기술 컴포넌트는 `share`로 이동하는 것을 기본 방향으로 본다
- 현재 `OtpSecretEncryptor`, `OtpTotpUtil`, `WorkGitAccessTokenEncryptor`는 `gw-home-share`로 이동해 공통 컴포넌트로 관리한다
- 서비스 내부 `toResponse`, `toSummaryResponse`, `toVo` 같은 변환 메서드는 점진적으로 `convert` 클래스로 옮긴다

## 에러와 정책 값 기준

- 사용자 응답 메시지의 기본값은 `ErrorCode`가 제공하는 공통 메시지를 우선 사용한다
- 서비스 상세 메시지는 기본 메시지만으로 의도가 부족한 경우에만 제한적으로 사용한다
- 상세 메시지는 디버그와 로그 문맥을 보조하는 수준으로 유지하고, 서비스마다 표현이 불필요하게 갈라지지 않게 관리한다
- 서비스 상단 상수 중 정책 성격의 값은 공통 정책으로 올릴 수 있는지 먼저 검토한다
- 공통 정책 값은 우선 `gw-home-share`의 `common.policy` 계열에 배치할 수 있는지 검토한다
- 다만 새로운 공통 위치는 현재 패키지 구조와 맞아야 하며, 별도 구조 합의 없이 임의의 `share.model` 같은 새 패키지를 바로 만들지 않는다

## 패키지 구조

```text
com.gw.api.controller.{domain}
com.gw.api.service.{domain}
com.gw.api.convert.{domain}
com.gw.api.dto.{domain}
com.gw.infra.db.mapper.{domain}
com.gw.share.vo.{domain}
com.gw.share.jvo.{domain}
com.gw.share.common.{category}
```

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| Controller 메서드 | 동사 + 명사 | `getBoard`, `createBoard` |
| Service 메서드 | 동사 + 명사 | `findBoard`, `saveBoard` |
| Mapper 메서드 | `select` / `insert` / `update` / `delete` + 명사 | `selectBoardByUuid` |
| Request DTO | `{동사}{명사}Request` | `CreateBoardRequest` |
| Response DTO | `{명사}Response` | `BoardResponse` |
| XML id | Mapper 메서드명과 동일 | `selectBoardByUuid` |

## 구현 시 체크 포인트

- SQL은 명시적으로 작성되어 있는가
- API 응답에 `_idx`가 섞이지 않았는가
- 조회 모델과 저장 모델의 역할이 섞이지 않았는가
- 공통화가 필요한 로직이 `share`로 빠져야 하는 수준인지 검토했는가
- 컨트롤러와 서비스 `public` 메서드에 한글 역할 주석이 있는가
- 서비스 메서드가 시작/완료/실패 로그를 남기는가
- 서비스 내부 변환 메서드를 `convert`로 분리할 수 있는가
- `gw-home-api/util`에 둔 코드가 실제로는 `share` 책임이 아닌지 점검했는가
