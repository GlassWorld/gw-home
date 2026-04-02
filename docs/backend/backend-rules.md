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
- 응답 DTO 조립은 도메인별 변환 책임으로 분리하는 것을 권장한다
- 서비스 메서드에는 진입, 완료, 실패 로그를 남긴다

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
