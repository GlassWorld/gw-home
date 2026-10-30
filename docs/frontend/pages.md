# 페이지 안내

## 이 문서의 목적

이 문서는 현재 프론트엔드 라우트와 각 페이지의 역할을 이해하기 위한 안내서다.

## 리뉴얼 진행 상태

- 2026-10-25 기준 1~5단계 UI·내 할 일·보고서 제거·메일 구현·간트/완료 통계를 반영했다. 메일 실제 발송은 보류하며 기본 디자인은 다크모드다.
- 색상·간격 토큰과 공통 UI 사용 기준은 [프런트엔드 개발 규칙](./frontend-rules.md#디자인-기준과-반응형-공통-ui)을 따른다. 모바일 메뉴, 모달 스크롤·포커스, 대시보드 업무 카드, 로그인 화면을 정비했다.
- 일일보고·내 주간보고·공개 주간보고·관리자 일일보고는 [리뉴얼 계획](../../work/todo/common/todo-project-renewal.md) 3단계에서 제거했다.
- `/work/todos`는 여러 업무의 최하위 내 할 일을 모아 보여준다. 업무·검색·일정·비활성 포함 필터와 분류·페이징을 제공하며 기존 업무별 트리 상세를 유지한다.
- `/work/gantt`, `/work/statistics`는 기존 업무·TODO를 조회하는 일정·완료 통계 화면이다. `/settings`의 메일 알림 패널은 4단계에서 구현했지만 사용자 요청으로 실제 발송은 비활성 상태를 유지한다.
- 보고서 메뉴·즐겨찾기·대시보드 참조를 정리하고 기존 URL 종료 안내를 제공한다. 기존 보고서 데이터의 물리 삭제는 별도 작업이다.

## 주요 라우트

| 경로 | 파일 | 설명 | 인증 필요 |
|------|------|------|-----------|
| `/` | `pages/index.vue` | 루트 진입 시 기본 페이지로 이동 | X |
| `/login` | `pages/login.vue` | 로그인 화면 | X |
| `/dashboard` | `pages/dashboard/index.vue` | 대시보드 | O |
| `/notices` | `pages/notices/index.vue` | 공지사항 목록 | O |
| `/notices/[noticeUuid]` | `pages/notices/[noticeUuid].vue` | 공지사항 상세 | O |
| `/board` | `pages/board/index.vue` | 게시글 목록 | O |
| `/board/[boardUuid]` | `pages/board/[boardUuid].vue` | 게시글 상세 | O |
| `/board/create` | `pages/board/create.vue` | 게시글 작성 | O |
| `/work` | `pages/work/index.vue` | 업무 등록과 조회 | O |
| `/work/todos` | `pages/work/todos/index.vue` | 업무 할 일 통합 조회·등록·수정·완료·완료 취소 | O |
| `/life/todos` | `pages/life/todos/index.vue` | 매일 체크 캘린더·날짜별 체크리스트, 일반 일상 할 일·생활 카테고리 | O |
| `/work/todos/[workUnitUuid]` | `pages/work/todos/[workUnitUuid].vue` | 업무별 TODO 트리 상세 관리 | O |
| `/work/gantt` | `pages/work/gantt/index.vue` | 간트차트·모바일 일정 목록·기간/업무 필터 | O |
| `/work/statistics` | `pages/work/statistics/index.vue` | 완료 추이·현재 현황·업무별 진행률 | O |
| `/work/git-accounts` | `pages/work/git-accounts/index.vue` | Git 계정 및 저장소 연동 관리 | O |
| `/vault` | `pages/vault/index.vue` | 자격증명 보관함 | O |
| `/settings` | `pages/settings/index.vue` | 계정 설정 | O |
| `/security` | `pages/security/index.vue` | OTP와 보안 설정 | O |
| `/admin/accounts` | `pages/admin/accounts/index.vue` | 관리자 계정 관리 | O |
| `/admin/notices` | `pages/admin/notices/index.vue` | 관리자 공지 관리 | O |
| `/admin/vault-categories` | `pages/admin/vault-categories/index.vue` | 관리자 금고 카테고리 관리 | O |

## 초기 진입 흐름

```text
사용자 접속
  -> /
  -> 기본 페이지로 이동
  -> 인증 필요 페이지면 auth 미들웨어 검사
  -> 비인증 상태면 /login 으로 이동
```

## 페이지별 역할

### 일정과 완료 통계

- 간트는 PC에서 차트, 모바일에서 일정 목록으로 시작하며 보기 방식을 바꿀 수 있다. 모바일 조회 조건은 접어 두고 일정과 주요 수치를 먼저 보여준다. 일정을 선택하면 기존 TODO 상세 링크로 이동한다.
- 업무 전체 막대·진행률은 전체 하위 TODO에서 파생한다. TODO 부모의 직접 지정 일정과 구분하고 날짜 한쪽만 있으면 점선 표식, 미지정 최하위는 별도 접기 목록, 기간 밖 조상은 계층 표시로 안내한다.
- 통계는 일·주·월 완료 막대와 숫자 표, 현재 완료율/미완료/지연, 오늘/예정/일정 미지정, 업무별 진행률을 제공한다. 완료일 미상은 현재 완료율에 포함하지만 추이에서 제외한다.
- 간트 기본은 오늘부터 14일·활성 업무, 통계는 최근 30일·비활성 포함이다. 기간·업무·활성 조건·시간대를 명시하고, 완료 추이의 기간과 현재 현황 분모의 차이를 화면에 설명한다.
- pages는 인증 라우트와 feature 조합만 맡는다. `use-work-insights-api` → feature API/타입, `use-work-insights` → 조회/필터/실패/늦은 응답 처리, `WorkInsightsFilters`·`WorkGanttView`·`WorkStatisticsView`·`WorkCompletionChart` → 표현, `assets/styles/work-insights.css` → 공통 반응형 배치다.
- 메뉴·즐겨찾기와 내 할 일에서 두 화면에 접근한다. 다크 토큰·BaseButton/BaseState를 재사용하고 차트 내부 스크롤과 표 대체를 제공한다. 메일 활성화는 이 화면의 선행 조건이 아니다.

### 로그인

- 로그인 ID와 비밀번호 입력
- 이미 로그인한 사용자는 대시보드로 이동

### 대시보드

- 로그인 사용자 요약 정보
- 최근 공지와 최근 게시글 표시
- 주요 화면으로 이동하는 빠른 링크 제공
- 활성 업무의 최하위 TODO 요약과 내 할 일 진입 링크를 제공한다. 보고서 요약·최근 보고·작성 바로가기는 제거했다. 페이지는 `DashboardWorkspacePage`를 조합하고 공지·게시글과 업무 현황을 유지한다.

### 공지사항

- 공지 목록 조회
- 공지 상세 확인

### 게시글

- 목록, 검색, 상세 조회
- 작성자 본인의 수정과 삭제
- 새 게시글 작성

### 설정과 보안

- 메일은 사용자 요청으로 실제 발송을 사용하지 않는다. 기존 설정·예약 UI는 보존하며 서버 발송 스위치가 꺼져 있으면 확인 메일·알림을 보낼 수 없다.
- 계정 정보 수정
- OTP 설정과 보안 설정 관리
- `/settings#work-notifications`: `WorkNotificationSettingsPanel`에서 수신 주소 확인·시간대·정기 요약/개별 활성화, `WorkNotificationHistory`에서 발송 내역을 제공한다. 페이지는 조합만 하고 API·상태는 work feature에 둔다.
- 기존 `WorkTodoDetailModal`은 최하위 TODO에 `WorkTodoReminderPanel`을 조합한다. 설정 시간대의 날짜/시간으로 저장·취소하며 완료/설정 미준비를 안내한다. 공통 버튼·상태·모달, 다크 토큰·모바일 폼 규칙을 재사용한다.
- 메일 상세 링크 `/work/todos/{workUnitUuid}?todo={todoUuid}`는 기존 트리를 읽은 후 일치하는 본인 TODO 모달을 연다. 없는 항목이나 다른 사용자 데이터는 열지 않는다.

### 업무와 관리자 화면

- 업무 관리와 업무별 TODO 트리 등록·수정·완료 처리
- 통합 내 할 일의 빠른 등록은 소속 업무를 선택해 루트 항목으로 추가한다. 상세·수정·하위 추가·완료 취소는 기존 트리 폼과 편집 로직을 공유한다.
- 오늘·예정·지연·완료·일정 없음 분류는 서버 기준이며 현재 완료율 분모는 조회 조건에 해당하는 최하위 항목이다. 기기의 시간대를 표시하고 완료 시각 미기록 항목은 별도로 안내한다.
- 일일보고·개인/공개 주간보고·관리자 일일보고 UI와 API는 종료했다. 업무 목록은 최근 수정순이며 기존 보고서 사용횟수는 표시하지 않는다.
- Git 계정과 저장소 연동 관리
- 관리자 전용 운영 기능 제공

## 미들웨어

```text
middleware/
├── auth.ts   인증 필요 페이지 접근 제어
├── guest.ts  로그인 페이지 같은 비인증 전용 화면 제어
├── admin.ts  관리자 전용 화면 접근 제어
└── retired-reports.global.ts  클라이언트의 이전 보고서 URL 이동 처리
```

## 종료된 보고서 링크

- `/weekly-reports`, `/work/daily-reports`, `/work/weekly-reports`, `/admin/daily-reports`와 각 하위 URL은 `/work/todos?notice=reports-retired`로 연결한다.
- 서버 진입은 `nuxt.config.ts`의 302 routeRules, 클라이언트 이동은 global middleware가 처리한다. 경로 목록은 `constants/retired-report-routes.ts`에서 공유한다.
- 내 할 일에서 종료·데이터 보관 안내를 표시하고 닫을 수 있다. 미인증 사용자는 기존 auth 정책에 따라 로그인 화면으로 이동하고 로그인 성공 후 대시보드로 진입한다.
- 즐겨찾기 조회는 서버 허용 목록과 프런트 메뉴 목록으로 필터링하며 남은 메뉴 순서는 유지한다. 정리 SQL을 실행하지 않아도 보고서 메뉴는 숨겨진다.

## 업무·일상 할 일 메뉴 분리 (2026-10-25)

### 매일 체크 캘린더 (2026-10-27)

- 일상 할 일은 기본으로 `매일 체크`를 표시한다. `일반 할 일` 보기에서 기존 단발성 CRUD·필터·카테고리를 사용한다. `?group=TODAY` 등 기존 분류 링크는 일반 할 일로 진입한다.
- 제목만 등록하면 오늘부터 매일 표시한다. 월간 캘린더는 날짜별 완료/전체 개수, 오늘 표시, 선택 날짜, 이전·다음 달·오늘 이동을 제공한다.
- 항목은 텍스트로 직접 등록한다. 목록은 체크박스·이름·수정·삭제를 한 행에 표시하고 별도 완료 상태 행을 두지 않는다. 삭제 확인 후 모든 날짜의 목록과 체크 집계에서 즉시 제외한다. 추천 운동·항목 관리 전환·반복 종료 UI는 제공하지 않는다.
- 날짜 선택 후 체크 한 번으로 서버에 저장하며 재클릭하면 취소한다. 저장 중 중복 클릭·날짜 이동을 막고 실패 시 기존 체크 상태를 유지한다. 미래 날짜는 조회만 가능하다.
- 제목 수정과 즉시 삭제를 제공한다. 과거에 종료한 항목도 삭제할 수 있으며 삭제 실패 시 목록·체크 상태를 유지한다.
- `LifeDailyChecklist`·`LifeDailyCalendar`와 `use-life-daily`가 화면·월 조회·저장을 담당한다. 늦은 이전 월 응답을 무시하고 오류 재조회·빈 목록·공통 모달·토스트를 제공한다.
- 시간대는 기기 시간대로 API에 전달하고 오늘은 서버 응답으로 확인한다. 달력 날짜는 UTC 날짜 문자열로 변환하지 않는다.
- 매일 체크 항목은 기존 단발성 일상 집계·업무 집계에 포함하지 않는다. 모바일은 캘린더 아래 체크리스트를 배치한다.

### 일반 일상 할 일

- 기존 `/work/todos`는 업무 할 일로 표시하며 링크·즐겨찾기 경로는 유지한다. 간트/통계 메뉴도 업무 용도임을 표시한다.
- `/life/todos`는 독립 life feature를 사용한다. 제목만으로 등록하며 날짜·메모·생활 카테고리는 선택한다. 소속 업무를 요구하지 않는다.
- 생활 카테고리는 화면 내 관리 모달에서 추가·이름 변경·삭제한다. 카테고리 삭제 시 할 일을 지우지 않고 미분류로 전환한다.
- 검색·카테고리·미분류 필터와 전체/오늘/예정/지연/완료/일정 미지정 분류, 서버 페이징을 제공한다. 완료/취소·삭제로 마지막 페이지가 비면 유효한 페이지를 다시 조회한다.
- 등록/수정·삭제 확인·카테고리 관리 모달은 공통 BaseModal을 사용한다. 저장 중 닫기·중복 제출을 차단하고 오류 시 입력 내용을 유지한다.
- 대시보드에서 업무와 일상을 별도 카드로 집계한다. 오늘 남은 일 링크는 각 메뉴의 `?group=TODAY`로 연결한다. 일상 조회 실패는 업무 요약 표시를 막지 않는다.
- 생활 카테고리·API·완료율은 업무와 분리되며 공통 버튼·모달·로딩·오류·빈 상태·페이징·토스트를 재사용한다.
