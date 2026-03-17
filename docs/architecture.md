# Architecture

## 모듈 구조

```
gw-home/
├── gw-home-share/
├── gw-home-api/
└── gw-home-infra-db/
```

Gradle 모듈명은 `{project}-share`, `{project}-api`, `{project}-infra-db` 형식을 사용한다.
예: `projectName=gw-home`이면 `gw-home-share`, `gw-home-api`, `gw-home-infra-db`

## 모듈 책임

### `{project}-share` (`{project}-share/`)
- 공통 응답 DTO (`ApiResponse`, `PageResponse`)
- 공통 예외 (`BusinessException`, `ErrorCode`)
- 공용 `VO` / `JVO`
  - `VO`: 단일 테이블 컬럼 구조 그대로
  - `JVO`: 조인/확장 조회 구조
  - 공통 PK/UUID/감사 컬럼은 `BaseVo` 상속으로 관리
  - 내부 필드와 클래스명은 DDL 축약형을 그대로 따른다 (`AcctVo`, `BrdPstJvo`)
- 유틸, 상수, 공통 인터페이스
- 다른 모듈에 의존하지 않음

### `{project}-api` (`{project}-api/`)
- `Controller` — HTTP 요청/응답 처리
- `Service` — 비즈니스 로직
- `domain/{도메인}/` 구조로 분리
- `{project}-share` 의존 O, `{project}-infra-db` 직접 의존 O

### `{project}-infra-db` (`{project}-infra-db/`)
- `Mapper` (interface) — MyBatis 매퍼 인터페이스
- MyBatis Mapper XML (`resources/mapper/{도메인}/`)
- `DataSource`, `MyBatis` 설정
- DB 마이그레이션 스크립트 (`sql/ddl/`, `sql/dml/`)
- Mapper 인터페이스와 XML을 함께 관리

## 의존 방향

```
{project}-api → {project}-share
{project}-api → {project}-infra-db
{project}-infra-db → {project}-share
```

## 패키지 규칙

```
com.gw.{module}.{layer}.{domain}

예:
com.gw.api.controller.board
com.gw.api.service.board
com.gw.infra.db.mapper.board
com.gw.api.dto.board
com.gw.share.common.exception
com.gw.share.vo.board
com.gw.share.jvo.board
```
