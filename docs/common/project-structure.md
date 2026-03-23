# Project Structure

## 디렉토리 구조 (전체)

```
gw-home/
├── README.md
├── docs/
│   ├── common/
│   │   ├── architecture.md         # 전체 아키텍처
│   │   └── project-structure.md    # 이 파일
│   ├── backend/
│   │   ├── backend-rules.md        # 백엔드 코딩 규칙
│   │   ├── database.md             # DB DDL 규칙
│   │   └── domain.md               # 도메인 정의
│   └── frontend/
│       ├── frontend-rules.md       # 프론트 코딩 규칙
│       ├── pages.md                # 페이지 목록 및 라우트
│       └── auth-flow.md            # 인증 흐름
├── .ai/
│   ├── AGENTS.md
│   ├── CORE_RULES.md
│   ├── TASK_ROUTER.md
│   ├── SKILL_INDEX.md
│   ├── project/
│   │   └── summary.md
│   └── skills/
│       ├── backend/
│       │   ├── create-domain-structure.md
│       │   └── create-service.md
│       ├── frontend/
│       │   ├── create-page.md
│       │   └── create-component.md
│       ├── database/
│       │   ├── generate-ddl.md
│       │   └── create-mapper.md
│       └── common/
│           └── create-api-endpoint.md
├── todo/
│   ├── 00-overview.md
│   ├── 01-project-setup.md
│   ├── 02-share-module.md
│   ├── 03-infra-db-module.md
│   ├── 04-domain-account.md
│   ├── 05-domain-auth.md
│   ├── 06-domain-profile.md
│   ├── 07-domain-board.md
│   ├── 08-domain-comment.md
│   ├── 09-domain-file.md
│   ├── 10-domain-tag.md
│   ├── 11-domain-favorite.md
│   ├── 12-domain-admin.md
│   └── 13-ui-module.md
├── gw-home-share/
├── gw-home-api/
├── gw-home-infra-db/
└── gw-home-ui/                     # Nuxt3
    ├── pages/
    │   ├── login.vue
    │   ├── dashboard/
    │   │   └── index.vue
    │   ├── board/
    │   │   ├── index.vue
    │   │   └── [boardUuid].vue
    │   └── profile/
    │       └── index.vue
    ├── components/
    ├── composables/
    ├── stores/
    ├── types/
    ├── assets/
    ├── nuxt.config.ts
    ├── tsconfig.json
    └── package.json
```

## Backend 소스 구조 (`{project}-api`)

```
gw-home-api/src/main/java/com/gw/api/
├── controller/
│   ├── account/    AccountController.java
│   ├── auth/       AuthController.java
│   ├── profile/    ProfileController.java
│   ├── board/      BoardController.java
│   ├── comment/    CommentController.java
│   ├── file/       FileController.java
│   ├── tag/        TagController.java
│   ├── favorite/   FavoriteController.java
│   └── admin/      AdminController.java
├── service/
│   └── {domain}/   {Domain}Service.java
└── dto/
    └── {domain}/   {Domain}Request.java, {Domain}Response.java
```

## Backend 소스 구조 (`{project}-infra-db`)

```
gw-home-infra-db/src/main/java/com/gw/infra/db/
└── mapper/
    └── {domain}/   {Domain}Mapper.java

gw-home-infra-db/src/main/resources/
├── mapper/
│   └── {domain}/   {Domain}Mapper.xml
└── sql/
    ├── ddl/
    │   └── {domain}/   {table}.sql
    └── dml/
```

## Backend 소스 구조 (`{project}-share`)

```
gw-home-share/src/main/java/com/gw/share/
├── common/
│   ├── exception/  BusinessException.java, ErrorCode.java
│   └── response/   ApiResponse.java, PageResponse.java
├── vo/
│   └── {domain}/   {Domain}Vo.java
└── jvo/
    └── {domain}/   {Domain}Jvo.java
```
