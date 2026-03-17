# Architecture

## 모듈 구조

```
gw-home/
├── share/
├── api/
└── infra-db/
```

## 모듈 책임

### share
- 공통 응답 DTO (`ApiResponse`, `PageResponse`)
- 공통 예외 (`BusinessException`, `ErrorCode`)
- 유틸, 상수, 공통 인터페이스
- 다른 모듈에 의존하지 않음

### api
- `Controller` — HTTP 요청/응답 처리
- `Service` — 비즈니스 로직
- `Mapper` (interface) — MyBatis 매퍼 인터페이스
- `domain/{도메인}/` 구조로 분리
- `share` 의존 O, `infra-db` 직접 의존 X

### infra-db
- MyBatis Mapper XML (`resources/mapper/{도메인}/`)
- `DataSource`, `MyBatis` 설정
- DB 마이그레이션 스크립트 (`sql/ddl/`, `sql/dml/`)
- `api` 모듈의 Mapper 인터페이스를 구현

## 의존 방향

```
api → share
infra-db → api (Mapper XML 연결)
```

## 패키지 규칙

```
com.gw.{module}.{domain}.{layer}

예:
com.gw.api.board.controller
com.gw.api.board.service
com.gw.api.board.mapper
com.gw.api.board.dto
com.gw.share.common.exception
```
