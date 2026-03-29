# Architecture

## 전체 모듈 구조

```
gw-home/
├── gw-home-share/      # 공통 DTO, 예외, 유틸 (Backend 공유)
├── gw-home-api/        # Controller, Service, DTO
├── gw-home-infra-db/   # MyBatis Mapper, XML, DB 설정
└── gw-home-ui/         # Nuxt3 프론트엔드
```

## Backend 모듈 책임

### `{project}-share`
- 공통 응답 DTO (`ApiResponse`, `PageResponse`)
- 공통 예외 (`BusinessException`, `ErrorCode`)
- 공통 범용 유틸 (`share.util`)
- 공용 `VO` / `JVO` (단일 테이블 / 조인 조회 모델)
- `BaseVo`: 공통 PK/UUID/감사 컬럼 (`idx`, `uuid`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`, `delAt`)
- 다른 모듈에 의존하지 않음

### `{project}-api`
- `Controller` — HTTP 요청/응답
- `Service` — 비즈니스 로직
- `convert/{domain}/` — 도메인 응답 변환 책임
- `dto/{domain}/` — Request / Response DTO
- `{project}-share` 의존 O, `{project}-infra-db` 직접 의존 O

### `{project}-infra-db`
- `Mapper` (interface) — MyBatis 매퍼 인터페이스
- MyBatis Mapper XML (`resources/mapper/{domain}/`)
- `DataSource`, `MyBatis` 설정
- DB 마이그레이션 스크립트 (`sql/ddl/`, `sql/dml/`)

## Frontend 모듈 책임

### `{project}-ui` (Nuxt3)
- 루트 진입: `/` → `/dashboard` 이동
- 비인증 상태에서 보호 페이지 접근 시 `/login` 리다이렉트
- `composables/` — API 호출 훅
- `components/` — 공통 컴포넌트
- `pages/` — 라우트 기반 페이지
- `stores/` — 상태 관리 (Pinia)
- `types/` — TypeScript 타입 정의

## Backend 의존 방향

```
{project}-api → {project}-share
{project}-api → {project}-infra-db
{project}-infra-db → {project}-share
{project}-ui → {project}-api (HTTP / REST)
```

## Backend 패키지 규칙

```
com.gw.{module}.{layer}.{domain}

예:
com.gw.api.controller.board
com.gw.api.service.board
com.gw.api.convert.board
com.gw.api.dto.board
com.gw.infra.db.mapper.board
com.gw.share.vo.board
com.gw.share.jvo.board
com.gw.share.common.exception
```
