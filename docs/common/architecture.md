# 아키텍처

## 이 문서의 목적

이 문서는 `gw-home` 프로젝트의 전체 구조와 모듈 책임을 빠르게 이해하기 위한 사용자용 안내서다.

## 전체 모듈 구조

```text
gw-home/
├── gw-home-share/      공통 DTO, 예외, 유틸
├── gw-home-api/        Controller, Service, DTO
├── gw-home-infra-db/   MyBatis Mapper, XML, DB 설정
└── gw-home-ui/         Nuxt3 프론트엔드
```

## 모듈별 책임

### `gw-home-share`

- 공통 응답 DTO (`ApiResponse`, `PageResponse`)
- 공통 예외 (`BusinessException`, `ErrorCode`)
- 공용 `VO`, `JVO`
- 공통 식별자와 감사 컬럼을 담는 `BaseVo`

### `gw-home-api`

- HTTP 요청과 응답 처리
- 비즈니스 로직 수행
- 도메인별 Request / Response DTO 관리
- 도메인별 응답 변환 로직 관리

### `gw-home-infra-db`

- MyBatis Mapper 인터페이스
- Mapper XML
- DataSource, MyBatis 설정
- DDL, DML 스크립트 관리

### `gw-home-ui`

- 페이지 라우팅
- 공통 컴포넌트
- API 연동 composable
- 상태 관리 store
- TypeScript 타입 관리

## 의존 방향

```text
gw-home-api      -> gw-home-share
gw-home-api      -> gw-home-infra-db
gw-home-infra-db -> gw-home-share
gw-home-ui       -> gw-home-api (HTTP API 호출)
```

## 백엔드 패키지 규칙

기본 패키지 구조는 아래 순서를 따른다.

```text
com.gw.{module}.{layer}.{domain}
```

예시:

```text
com.gw.api.controller.board
com.gw.api.service.board
com.gw.api.convert.board
com.gw.api.dto.board
com.gw.infra.db.mapper.board
com.gw.share.vo.board
com.gw.share.jvo.board
```

## 함께 보면 좋은 문서

- 백엔드 규칙: [backend-rules.md](/home/glassworld/workspace/gw-home/docs/backend/backend-rules.md)
- 데이터베이스 규칙: [database.md](/home/glassworld/workspace/gw-home/docs/backend/database.md)
- 프론트엔드 규칙: [frontend-rules.md](/home/glassworld/workspace/gw-home/docs/frontend/frontend-rules.md)
