# Review: 헤더 즐겨찾기 네비게이션

## 요청 요약

사이드메뉴로 이동한 메뉴 중 사용자가 자주 쓰는 항목을 **개인별로 즐겨찾기** 설정할 수 있어야 한다.

- 사이드메뉴에서 별표 체크/해제
- 체크된 메뉴를 헤더 영역에 별도 노출
- 설정은 **사용자별로 독립 저장**

---

## 분류

**HEAVY**

이유:

- 사용자별 저장이 필요해 프론트 단독 처리 불가
- `profile` 또는 별도 사용자 설정 저장 구조가 필요
- 공통 UI (`AppHeader.vue`, `AppSidebarNavigation.vue`, `app.vue`) 동시 수정
- 백엔드 API + DB + 프론트 연동 동시 작업

---

## 현재 상태

### 1. 헤더 구조

`AppHeader.vue` 는 현재 다음 역할만 가진다.

- 사이드메뉴 열기 버튼
- 브랜드 링크
- 사용자 프로필 링크
- 메모 버튼
- 로그아웃 버튼

즉, 즐겨찾기 메뉴를 렌더링할 **헤더 전용 네비게이션 영역은 아직 없다.**

### 2. 사이드 구조

`AppSidebarNavigation.vue` 에는 정적 메뉴 배열이 하드코딩되어 있다.

- 일반 메뉴: `primaryNavigationItems`
- 관리자 메뉴: `adminNavigationItems`

현재는 단순 이동 링크만 렌더링하며, 항목별 메타 정보(즐겨찾기 가능 여부, 키, 정렬, 권한 범위)가 없다.

### 3. 개인 설정 저장 구조

기존에 사용자별 단건 설정은 `tb_mbr_prfl.memo` 와 `/api/v1/profiles/me/memo` 흐름이 이미 있다.

이 패턴은 이번 기능에도 참고 가능하다.

- 본인 설정만 저장
- profile 도메인에서 처리
- 공개 프로필 응답에는 섞지 않음

---

## 핵심 설계 결정

### 1. 저장 위치

**우선안: `tb_mbr_prfl` 에 즐겨찾기 메뉴 설정 컬럼 추가**

예시:

- `fav_menu_json TEXT`

저장 예시:

```json
["/dashboard","/notices","/work"]
```

선정 이유:

- 사용자당 1세트 설정이라 별도 테이블보다 단순하다
- 이미 `memo` 처럼 개인 설정성 데이터가 profile에 있다
- 과잉 설계를 피할 수 있다

배제안:

- `tb_mbr_fav_menu` 별도 테이블 신설
  - 메뉴 정렬/히스토리/공유 기능이 없으면 과하다

### 2. API 범위

**profile 도메인에 `me/navigation-favorites` 전용 API 추가**

후보:

- `GET /api/v1/profiles/me/navigation-favorites`
- `PUT /api/v1/profiles/me/navigation-favorites`

응답 예시:

```json
{
  "favorite_menus": ["/dashboard", "/notices", "/work"]
}
```

저장 규칙:

- 본인 것만 조회/수정
- 허용된 메뉴 경로만 저장
- 관리자 전용 경로는 ADMIN만 저장 가능

### 3. 프론트 메뉴 정의 방식

사이드/헤더에서 같은 기준을 써야 하므로, 메뉴 메타데이터를 공통 정의로 분리해야 한다.

예시 메타:

- `key`
- `to`
- `label`
- `section`
- `adminOnly`
- `favoriteEnabled`

권장 위치:

- `gw-home-ui/constants/navigation.ts`
  또는
- `gw-home-ui/composables/useNavigationMenu.ts`

중요:

- 공통 UI 규칙상 공통 모듈 수정은 범위 영향이 있으므로 작업 단계에서 명시적으로 관리해야 한다.

### 4. 헤더 표시 방식

헤더에는 즐겨찾기 전체를 길게 노출하지 말고, **최대 3~5개**로 제한하는 편이 안전하다.

권장:

- 데스크톱: 최대 4개
- 모바일: 숨김 또는 1~2개 축약

이유:

- 헤더는 이미 프로필/메모/로그아웃 버튼이 있어 폭이 좁다
- 관리자 계정은 메뉴 종류가 많아 한 줄 붕괴 위험이 있다

### 5. 정렬 기준

초기 버전은 **사용자가 체크한 목록의 저장 순서**를 그대로 사용하거나,
단순히 **공통 메뉴 정의 순서 기반 필터링**으로 가는 것이 적절하다.

권장 우선안:

- 저장은 배열로 하되,
- 렌더링은 공통 메뉴 정의 순서를 유지

이유:

- 별도 drag/drop 정렬 UI 없이도 일관된 결과를 낼 수 있다
- 구현이 단순하다

---

## 영향 범위

### Backend

- `profile` 도메인 API 추가
- `profile` mapper / XML 수정
- profile 관련 DTO 추가
- profile VO 필드 확장 가능성
- DDL 추가 필요

### Frontend

- `AppSidebarNavigation.vue` 에 별표 토글 UI 추가
- `AppHeader.vue` 에 즐겨찾기 메뉴 렌더링 추가
- 공통 메뉴 정의 분리
- 즐겨찾기 API composable/type 추가
- 앱 초기 로딩 시 개인 설정 fetch 전략 필요

### Database

- `tb_mbr_prfl` ALTER 필요

---

## 권장 구현 범위

### 1차 범위

- 사이드메뉴 메뉴 항목별 별표 토글
- 사용자별 즐겨찾기 조회/저장
- 헤더에 즐겨찾기 메뉴 노출
- 관리자 메뉴는 관리자에게만 즐겨찾기 가능

### 제외 권장

- 드래그 앤 드롭 정렬
- 아이콘 커스터마이징
- 메뉴 그룹별 즐겨찾기 섹션 분리
- 공개 공유 즐겨찾기

---

## 백엔드 제안

### DDL

`tb_mbr_prfl` 에 컬럼 추가:

```sql
ALTER TABLE tb_mbr_prfl
ADD COLUMN fav_menu_json TEXT;
```

주의:

- nullable 허용
- 비어 있으면 `[]` 로 해석

### DTO

- `NavigationFavoriteResponse`
  - `favoriteMenus: List<String>`
- `SaveNavigationFavoriteRequest`
  - `favoriteMenus: List<String>`

### Service 검증 규칙

- 허용된 메뉴 경로만 저장
- 중복 제거
- 최대 개수 제한
  - 권장: 5개
- 비관리자가 `/admin/*` 저장 요청 시 거부 또는 필터링

권장:

- 서버에서 조용히 정제하지 말고, 잘못된 값은 검증 에러 처리

---

## 프론트 제안

### Sidebar

각 메뉴 항목 우측에 별표 버튼 추가:

- 체크 상태면 채워진 별
- 미체크 상태면 비어 있는 별
- 메뉴 이동 클릭과 별 토글 클릭 이벤트 분리

### Header

프로필 영역 앞 또는 브랜드 우측에 즐겨찾기 링크 그룹 추가

예시:

- 대시보드
- 공지사항
- 업무등록

모바일 대응:

- 화면이 좁으면 숨기거나 일부만 표시

### 상태 관리

가벼운 전역 상태로 관리하는 편이 적절하다.

후보:

- Pinia store
- `useState` 기반 전역 composable

권장:

- 헤더와 사이드가 동시에 참조하므로 store 사용이 가장 자연스럽다

---

## 리스크

### 1. 공통 메뉴 정의 중복

헤더와 사이드가 서로 다른 메뉴 목록을 들고 가면 불일치가 생긴다.

대응:

- 메뉴 메타를 한 곳에서만 정의

### 2. 관리자 메뉴 저장 오염

관리자였다가 일반 사용자로 바뀐 계정에 `/admin/*` 즐겨찾기가 남을 수 있다.

대응:

- 조회 시 권한 기준으로 필터링
- 저장 시 권한 검증

### 3. 헤더 폭 부족

즐겨찾기 수가 많아지면 헤더가 쉽게 무너진다.

대응:

- 최대 개수 제한
- 모바일 숨김/축약

### 4. profile 도메인 비대화

메모, 즐겨찾기 등 UI 설정이 계속 profile에 쌓일 수 있다.

대응:

- 이번 1차는 profile에 두되,
- 장기적으로 설정 종류가 늘면 `user-settings` 성격 분리 검토

---

## 변경 예상 파일

### Backend

- `gw-home-infra-db/src/main/resources/sql/ddl/profile/...`
- `gw-home-share/src/main/java/com/gw/share/vo/profile/PrflVo.java`
- `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/profile/...`
- `gw-home-infra-db/src/main/resources/mapper/profile/...`
- `gw-home-api/src/main/java/com/gw/api/dto/profile/...`
- `gw-home-api/src/main/java/com/gw/api/service/profile/ProfileService.java`
- `gw-home-api/src/main/java/com/gw/api/controller/profile/ProfileController.java`

### Frontend

- `gw-home-ui/components/common/AppHeader.vue`
- `gw-home-ui/components/common/AppSidebarNavigation.vue`
- `gw-home-ui/composables/...` 즐겨찾기 API/store
- `gw-home-ui/types/api/...`
- 공통 메뉴 정의 파일 1개

---

## 결론

이 기능은 **사이드 UI만 수정하는 작업이 아니라, 사용자별 설정 저장을 포함한 full-stack 공통 UI 작업**으로 보는 것이 맞다.

최적 1차 방향은 다음과 같다.

1. `tb_mbr_prfl` 에 즐겨찾기 메뉴 JSON 컬럼 추가
2. `profiles/me/navigation-favorites` 조회/저장 API 추가
3. 프론트에서 공통 메뉴 메타를 단일 소스로 정리
4. 사이드에서 별표 토글
5. 헤더에서 개인별 즐겨찾기 노출

---

## 관련 파일

- `gw-home-ui/components/common/AppHeader.vue`
- `gw-home-ui/components/common/AppSidebarNavigation.vue`
- `gw-home-ui/components/common/HeaderMemoModal.vue`
- `gw-home-api/src/main/java/com/gw/api/controller/profile/ProfileController.java`
- `gw-home-api/src/main/java/com/gw/api/service/profile/ProfileService.java`
- `review/ui/03-header-memo.md`
