# 프로젝트 구조

## 이 문서의 목적

이 문서는 저장소에서 어떤 디렉터리에 무엇이 있는지 빠르게 찾기 위한 안내서다.

## 최상위 구조

```text
gw-home/
├── docs/                  사용자용 문서
├── .ai/                   AI 작업용 문서
├── gw-home-share/         공통 백엔드 모듈
├── gw-home-api/           API 모듈
├── gw-home-infra-db/      DB 연동 모듈
└── gw-home-ui/            프론트엔드 모듈
```

## 문서 디렉터리

### `docs/`

- `common/`: 아키텍처, 구조, API 계약
- `backend/`: 백엔드 규칙, DB 규칙, 도메인 설명
- `frontend/`: 프론트엔드 규칙, 인증 흐름, 페이지 안내

### `.ai/`

- AI 작업용 진입 문서와 규칙 문서
- 사용자 설명보다는 작업 흐름과 제약 조건 중심

## 백엔드 소스 구조

### `gw-home-api`

```text
src/main/java/com/gw/api/
├── controller/{domain}/
├── service/{domain}/
├── convert/{domain}/
└── dto/{domain}/
```

### `gw-home-infra-db`

```text
src/main/java/com/gw/infra/db/
└── mapper/{domain}/

src/main/resources/
├── mapper/{domain}/
└── sql/
    ├── ddl/{domain}/
    └── dml/
```

### `gw-home-share`

```text
src/main/java/com/gw/share/
├── common/
├── vo/{domain}/
└── jvo/{domain}/
```

## 프론트엔드 소스 구조

### `gw-home-ui`

```text
gw-home-ui/
├── pages/
├── features/
├── components/
├── composables/
├── stores/
├── types/
├── assets/
├── middleware/
├── nuxt.config.ts
├── tsconfig.json
└── package.json
```

## 찾는 법

- API 요청/응답 구조를 보고 싶다면 `gw-home-api`의 `dto/`, `controller/`를 본다
- SQL과 매퍼를 보고 싶다면 `gw-home-infra-db`의 `mapper/`, `sql/`을 본다
- 공통 응답, 예외, VO/JVO를 보고 싶다면 `gw-home-share`를 본다
- 화면과 라우팅을 보고 싶다면 `gw-home-ui/pages/`를 본다
- 기능 단위 UI 조합과 화면 로직을 보고 싶다면 `gw-home-ui/features/`를 함께 본다
