# Review: 공지사항 기능 개발

> 작업 분류: **HEAVY**
> Full-stack 변경 + 관리자 권한 제어 + 대시보드 노출 + 전역 내비게이션 구조 변경 포함

---

## 작업 개요

공지사항 기능을 추가한다.

- 관리자 권한을 가진 사용자만 공지사항을 등록할 수 있어야 한다.
- 대시보드에서 최신 공지사항 제목을 노출해야 한다.
- 공지 상세 본문은 마크다운 형식으로 표시해야 한다.
- 관리자 전용 공지관리 메뉴를 생성해야 한다.
- 헤더 메뉴가 과밀해지고 있으므로, 사이드메뉴를 여는 버튼 기반 구조로 전환해야 한다.

---

## 변경 목적

- 운영 공지를 일반 게시글과 구분해 관리자가 통제 가능한 정보 채널을 만든다.
- 로그인 직후 대시보드에서 중요한 공지 제목을 바로 확인하게 한다.
- 상단 헤더에 누적된 링크를 정리해 내비게이션 확장성을 확보한다.

---

## 설계 결정 사항

### 1. 공지사항 저장 방식

**공지와 게시글은 완전히 분리한다.**

- 공지사항은 board 카테고리나 기존 게시글 테이블 재사용으로 처리하지 않는다.
- 공지 전용 테이블, VO/JVO, Mapper, Service, Controller, 페이지를 별도로 둔다.
- 이렇게 해야 권한, 목록 정책, 대시보드 노출, 상세 마크다운 정책을 board와 독립적으로 관리할 수 있다.

### 2. 권한 경계

**읽기 API와 관리 API 모두 notice 전용으로 구성하되, 관리 경로는 admin 하위로 둔다.**

- 사용자 조회: `notice`
- 관리자 등록/수정/삭제/관리 목록: `admin/notice`
- board 도메인과 API, DTO를 공유하지 않는다.

### 3. 공지 상세 마크다운 처리

**공지 상세는 프론트에서 `marked + DOMPurify` 조합을 재사용한다.**

- 현재 `DailyReportDetailModal.vue` 에 같은 패턴이 이미 있다.
- 공지 상세 페이지에서만 적용하고, board 상세 렌더링은 기존 동작을 유지한다.

### 4. 내비게이션 구조

**기존 `AppHeader` 링크 나열을 축소하고, 전역 사이드메뉴를 여는 버튼을 추가한다.**

- 현재 헤더에는 게시글, 업무, 일일보고, 주간보고, 자격증명, 관리자 링크가 직접 나열되어 있다.
- 공지 목록/공지관리까지 추가하면 모바일과 데스크톱 모두 밀도가 과도해진다.
- `app.vue` 레벨에서 헤더 + 사이드메뉴 레이아웃을 함께 조정해야 한다.

---

## 예상 영향 범위

### Backend

| 레이어 | 파일 | 변경 내용 |
|--------|------|-----------|
| DDL | notice 전용 SQL | 공지 테이블 신규 생성 |
| share VO/JVO | notice 전용 모델 | 공지 저장/조회 모델 추가 |
| Controller | `NoticeController.java` | 공지 목록/상세/대시보드 조회 API 추가 |
| Controller | `AdminNoticeController.java` | 관리자 공지 등록/수정/삭제/관리 목록 API 추가 |
| Service | notice 전용 service | 공지 조회/관리 로직 추가 |
| DTO | notice/admin DTO | 공지 목록, 상세, 등록/수정 요청/응답 추가 |
| Mapper | notice 전용 mapper | 공지 전용 쿼리 추가 |
| Mapper XML | notice 전용 XML | 공지 목록/상세/관리/대시보드 SQL 추가 |

### Frontend

| 파일 | 변경 내용 |
|------|-----------|
| `pages/dashboard/index.vue` | 최신 공지 제목 노출 영역 추가 |
| `pages/notices/*` | 공지 목록/상세 화면 추가 |
| `pages/admin/...` | 공지관리 화면 추가 |
| `components/common/AppHeader.vue` | 직접 링크 나열 축소, 사이드메뉴 버튼 추가 |
| `app.vue` | 사이드메뉴 셸 구조 반영 |
| 공통/신규 navigation 컴포넌트 | 메뉴 목록, 관리자 메뉴 조건부 노출 |

### 인증/보안

- 백엔드: `/api/v1/admin/**` + `@PreAuthorize("hasRole('ADMIN')")` 패턴 재사용
- 프론트: `middleware/admin.ts` 재사용
- 일반 게시글과 공지 API 완전 분리

---

## 핵심 검토 포인트

### 1. 관리자만 등록

- 공지 등록/수정/삭제는 `/api/v1/admin/notices` 계열로 분리해 `ADMIN`만 허용한다.
- 일반 게시글 API는 공지 로직을 전혀 알지 못하게 유지한다.

### 2. 대시보드 공지 제목 노출

- 현재 대시보드는 최근 게시글 5건만 조회한다.
- 공지 제목 노출은 최근 게시글과 별도 섹션으로 분리하는 편이 명확하다.
- 공지 1건 고정 노출인지, 최신 N건 노출인지 `todo` 단계에서 확정이 필요하다.

### 3. 공지 상세 마크다운

- 공지 전용 상세 페이지에서만 마크다운 렌더링을 적용한다.
- board 화면과 스타일, 데이터 모델은 그대로 둔다.

### 4. 공지관리 메뉴

- 현재 관리자 메뉴는 헤더에 `계정관리`만 직접 노출한다.
- 공지관리 추가 시 관리자 메뉴 묶음 구조가 필요하다.
- 향후 `관리자 > 계정관리 / 공지관리 / 일일보고 / 금고카테고리` 형태로 확장 가능한 사이드메뉴가 적합하다.

### 5. 헤더에서 사이드메뉴 전환

- 이는 단순 링크 추가가 아니라 전역 앱 셸 변경이다.
- `AppHeader.vue` 만 수정하면 끝나지 않고, `app.vue` 와 모바일 반응형 동작까지 함께 봐야 한다.

---

## 리스크 분석

| 리스크 | 대응 |
|--------|------|
| notice를 board에 섞어 구현해 도메인 경계가 다시 흐려질 위험 | notice 전용 DDL/API/UI로 분리 유지 |
| 새 도메인 추가로 파일 수와 구현 범위가 커짐 | notice 범위를 CRUD + 대시보드 노출 + 메뉴 개편으로만 제한 |
| 마크다운 렌더링 도입 시 XSS 위험 | `marked` 결과를 `DOMPurify.sanitize()` 후 렌더링 |
| 헤더를 급히 사이드메뉴로 바꾸다 전역 레이아웃 회귀 발생 | `app.vue` 기준으로 셸 구조 먼저 정리 후 헤더/메뉴 분리 |
| 대시보드 공지와 최근 게시글이 중복/혼재되어 정보 우선순위가 흐려짐 | 공지 섹션을 별도 카드로 분리 |

---

## 권장 구현 방향

1. notice 도메인을 board와 분리해 전용 DDL/API/UI를 만든다.
2. 관리자 공지 작성/수정/삭제는 `/api/v1/admin/notices` 계열로 둔다.
3. 사용자용 공지 조회는 `/api/v1/notices` 계열로 둔다.
4. 대시보드는 `최근 공지` 섹션을 별도로 두고 제목 클릭 시 공지 상세로 이동시킨다.
5. 공지 상세는 전용 페이지에서 마크다운 렌더링을 적용한다.
6. 전역 내비게이션은 헤더 링크 나열 대신 햄버거 버튼 + 사이드메뉴 구조로 전환한다.

---

## 예상 파일 후보

### 신규 가능성이 높은 파일

```text
review/board/01-notice-management.md
todo/board/01-notice-management.md
gw-home-ui/components/common/AppSidebarNavigation.vue
gw-home-ui/pages/admin/notices/index.vue
gw-home-ui/pages/notices/index.vue
gw-home-ui/pages/notices/[noticeUuid].vue
gw-home-api/src/main/java/com/gw/api/controller/notice/NoticeController.java
gw-home-api/src/main/java/com/gw/api/controller/admin/AdminNoticeController.java
gw-home-api/src/main/java/com/gw/api/service/notice/NoticeService.java
gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/notice/NoticeMapper.java
gw-home-infra-db/src/main/resources/mapper/notice/NoticeMapper.xml
```

### 변경 가능성이 높은 파일

```text
gw-home-ui/app.vue
gw-home-ui/components/common/AppHeader.vue
gw-home-ui/pages/dashboard/index.vue
docs/frontend/pages.md
```

---

## 추가 결정 사항

### 2026-03-31 확정

- 사용자 요청에 따라 **공지와 게시글은 완전히 구분**한다.
- 따라서 board 카테고리 재사용안은 폐기한다.
- 이후 `##계획` 단계에서는 notice를 별도 도메인처럼 취급해 todo를 작성한다.

---

## 관련 문서

- 이 문서: `review/board/01-notice-management.md`
- 참조: `docs/frontend/pages.md`
- 참조: `gw-home-ui/components/common/AppHeader.vue`
- 참조: `gw-home-ui/pages/dashboard/index.vue`
- 참조: `gw-home-ui/components/work/DailyReportDetailModal.vue`
