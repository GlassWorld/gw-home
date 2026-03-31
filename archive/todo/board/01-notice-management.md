# Todo: 공지사항 기능 개발

> review: `review/board/01-notice-management.md`
> 작업 분류: **HEAVY** — notice 전용 도메인 추가 + 관리자 권한 제어 + 대시보드 노출 + 전역 내비게이션 개편

---

## 작업 전제

- [x] 공지와 게시글은 완전히 분리한다.
- [x] 공지 등록/수정/삭제는 관리자만 가능하게 한다.
- [x] 공지 상세 본문은 마크다운 렌더링을 적용한다.
- [x] 대시보드에는 최신 공지 제목 목록을 별도 섹션으로 노출한다.
- [x] 헤더 직접 링크 구조는 사이드메뉴 버튼 기반 구조로 전환한다.

---

## Step 1. DB — notice 전용 스키마 추가

- [x] 공지 전용 DDL 파일 생성
  - 후보: `gw-home-infra-db/src/main/resources/sql/ddl/notice/tb_ntc.sql`
- [x] 공지 테이블 컬럼 설계
  - `tb_ntc_idx BIGSERIAL`
  - `tb_ntc_uuid UUID`
  - `ttl`
  - `cntnt`
  - `view_cnt`
  - `pblshd_yn` 또는 게시 상태 컬럼 여부 검토
  - 공통 감사 컬럼 / `del_at TIMESTAMPTZ`
- [x] 필요 시 인덱스 추가
  - UUID
  - 게시 상태 + 생성일 또는 공지일 정렬 기준
- [x] 롤백 가능 여부를 고려해 DDL 작성

---

## Step 2. Backend — share 모듈 notice 모델 추가

- [x] `gw-home-share` 에 notice 전용 `Vo` 생성
- [x] `gw-home-share` 에 notice 목록/상세용 `Jvo` 생성
- [x] 검색 조건용 search vo 생성
  - 관리자 목록 검색
  - 사용자 목록 검색
  - 대시보드 최신 공지 조회

---

## Step 3. Backend — infra-db notice mapper 추가

- [x] `NoticeMapper.java` 생성
- [x] `NoticeMapper.xml` 생성
- [x] 사용자용 SQL 작성
  - 공지 목록 조회
  - 공지 상세 조회
  - 대시보드 최신 공지 제목 조회
- [x] 관리자용 SQL 작성
  - 공지 목록 조회
  - 공지 등록
  - 공지 수정
  - 공지 삭제(소프트 삭제)
  - 필요 시 게시 상태 변경

---

## Step 4. Backend — notice DTO 추가

- [x] 사용자용 DTO 생성
  - `NoticeSummaryResponse`
  - `NoticeDetailResponse`
- [x] 관리자용 DTO 생성
  - 관리자 목록은 `NoticeListRequest`, `NoticeSummaryResponse` 재사용
  - `CreateNoticeRequest`
  - `UpdateNoticeRequest`
- [x] 대시보드용 DTO 생성 여부 검토
  - 기존 `NoticeSummaryResponse` 재사용 가능하면 재사용

---

## Step 5. Backend — notice service 구현

- [x] `NoticeService.java` 생성
  - `getNoticeList()`
  - `getNotice()`
  - `getDashboardNotices()`
- [x] 관리자용 공지 관리 service 생성 또는 service 내부 메서드 분리
  - `createNotice()`
  - `updateNotice()`
  - `deleteNotice()`
  - `getAdminNoticeList()`
- [x] 상세 조회 시 조회수 증가 정책 반영
- [x] 관리자 권한 검증은 controller 레벨 + admin 경로 보호로 이중 보장

---

## Step 6. Backend — controller 추가

- [x] `NoticeController.java` 생성
  - `GET /api/v1/notices`
  - `GET /api/v1/notices/{noticeUuid}`
  - `GET /api/v1/notices/dashboard`
- [x] `AdminNoticeController.java` 생성
  - `@PreAuthorize("hasRole('ADMIN')")`
  - `GET /api/v1/admin/notices`
  - `POST /api/v1/admin/notices`
  - `PUT /api/v1/admin/notices/{noticeUuid}`
  - `DELETE /api/v1/admin/notices/{noticeUuid}`

---

## Step 7. Frontend — 타입과 composable 추가

- [x] `gw-home-ui/types/api/notice.ts` 생성
  - `NoticeSummary`
  - `NoticeDetail`
  - `NoticeListParams`
  - `CreateNoticeForm`
  - `UpdateNoticeForm`
- [x] `gw-home-ui/composables/useNoticeApi.ts` 생성
  - `fetchNoticeList()`
  - `fetchNotice()`
  - `fetchDashboardNotices()`
  - `fetchAdminNoticeList()`
  - `createNotice()`
  - `updateNotice()`
  - `deleteNotice()`

---

## Step 8. Frontend — 사용자 공지 화면 추가

- [x] `gw-home-ui/pages/notices/index.vue` 생성
  - 공지 목록
  - 제목, 등록일, 조회수 표시
  - 제목 클릭 시 상세 이동
- [x] `gw-home-ui/pages/notices/[noticeUuid].vue` 생성
  - 공지 상세
  - 마크다운 렌더링 적용
  - `marked + DOMPurify` 패턴 재사용
- [x] 공지 상세 스타일 작성
  - 제목, 메타 정보, 본문 가독성
  - 코드블록/리스트/링크 스타일 포함

---

## Step 9. Frontend — 관리자 공지관리 화면 추가

- [x] `gw-home-ui/pages/admin/notices/index.vue` 생성
  - `definePageMeta({ middleware: 'admin' })`
  - 공지 목록 + 등록/수정 폼 또는 모달
  - 삭제 액션
  - 필요 시 게시 상태 표시
- [x] 관리자 폼 검증 반영
  - 제목 필수
  - 본문 필수
- [x] 저장 성공/실패 토스트 처리

---

## Step 10. Frontend — 대시보드 공지 섹션 추가

- [x] `gw-home-ui/pages/dashboard/index.vue` 수정
  - 최근 게시글과 별도 섹션으로 공지 제목 노출
  - 기본 계획: 최신 5건 제목 노출
  - 제목 클릭 시 공지 상세 이동
- [x] 공지 로딩 실패 시 섹션 단위 메시지 처리
- [x] 공지가 없을 때 빈 상태 메시지 처리

---

## Step 11. Frontend — 전역 사이드메뉴 구조 추가

- [x] `gw-home-ui/components/common/AppSidebarNavigation.vue` 생성
  - 일반 메뉴
    - 대시보드
    - 공지사항
    - 게시글
    - 업무등록
    - 일일보고
    - 주간보고
    - 자격증명
  - 관리자 메뉴
    - 계정관리
    - 공지관리
    - 일일보고 관리
    - 금고 카테고리 관리
- [x] `gw-home-ui/components/common/AppHeader.vue` 수정
  - 기존 nav 링크 제거 또는 축소
  - 사이드메뉴 열기 버튼 추가
- [x] `gw-home-ui/app.vue` 수정
  - 사이드메뉴 상태 관리
  - 헤더 + 본문 셸 구조 조정
  - 모바일/데스크톱 반응형 처리

---

## Step 12. 문서 정리

- [x] `docs/frontend/pages.md` 에 공지/공지관리 라우트 추가
- [x] 필요 시 프로젝트 요약 문서에 notice 도메인 반영 검토

---

## Step 13. 검증

- [x] 관리자 계정만 공지 등록/수정/삭제가 가능한지 확인
- [x] 일반 사용자는 공지 조회만 가능한지 확인
- [x] 게시글 기능이 공지 도입 후 컴파일/타입체크 기준으로 영향을 받지 않는지 확인
- [x] 대시보드에서 공지 제목이 정상 노출되도록 구현 및 타입체크 확인
- [x] 공지 상세 마크다운이 안전하게 렌더링되도록 구현 및 타입체크 확인
- [x] 사이드메뉴 전환 후 데스크톱/모바일 레이아웃이 깨지지 않는지 확인

---

## 관련 문서

- `review/board/01-notice-management.md`
