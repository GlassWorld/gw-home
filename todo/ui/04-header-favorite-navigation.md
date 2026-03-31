# Todo: 헤더 즐겨찾기 네비게이션

> review: `review/ui/04-header-favorite-navigation.md`
> 작업 분류: **HEAVY** — 사용자별 설정 저장 + profile API 확장 + 공통 헤더/사이드 UI 동시 수정

---

## 목표

- [ ] 사이드메뉴에서 메뉴별 즐겨찾기 체크/해제 가능
- [ ] 즐겨찾기 설정은 사용자별로 독립 저장
- [ ] 헤더에 개인 즐겨찾기 메뉴를 노출
- [ ] 관리자 메뉴는 권한에 맞게만 저장/노출

---

## Step 1. DB — profile 에 즐겨찾기 설정 컬럼 추가

- [ ] `tb_mbr_prfl` 에 즐겨찾기 메뉴 저장 컬럼 추가
  - 후보: `fav_menu_json TEXT`
- [ ] nullable 허용, 미설정 시 빈 배열로 해석
- [ ] rollback 또는 참고 DDL 주석 포함

예상 파일:

- `gw-home-infra-db/src/main/resources/sql/ddl/profile/tb_mbr_prfl_add_fav_menu_json.sql`

---

## Step 2. Backend — share/profile 모델 확장

- [ ] `PrflVo` 에 즐겨찾기 메뉴 저장 필드 추가
- [ ] 기존 공개 프로필 응답에는 노출하지 않도록 범위 유지

예상 파일:

- `gw-home-share/src/main/java/com/gw/share/vo/profile/PrflVo.java`

---

## Step 3. Backend — mapper / XML 확장

- [ ] profile mapper 에 즐겨찾기 조회 메서드 추가
- [ ] profile mapper 에 즐겨찾기 저장 메서드 추가
- [ ] account idx 기준 조회/저장 처리

메서드 후보:

- `selectNavigationFavoritesByAccountIdx(Long mbrAcctIdx): String`
- `updateNavigationFavorites(Long mbrAcctIdx, String favoriteMenusJson, String updatedBy): int`

예상 파일:

- `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/profile/ProfileMapper.java`
- `gw-home-infra-db/src/main/resources/mapper/profile/ProfileMapper.xml`

---

## Step 4. Backend — DTO 추가

- [ ] 즐겨찾기 조회 응답 DTO 추가
- [ ] 즐겨찾기 저장 요청 DTO 추가
- [ ] 최대 개수 및 값 검증 규칙 추가

DTO 후보:

- `NavigationFavoriteResponse`
  - `favoriteMenus: List<String>`
- `SaveNavigationFavoriteRequest`
  - `favoriteMenus: List<String>`

검증 규칙:

- 최대 개수 제한
  - 권장: 5
- 중복 금지
- 허용된 메뉴 경로만 저장

예상 파일:

- `gw-home-api/src/main/java/com/gw/api/dto/profile/NavigationFavoriteResponse.java`
- `gw-home-api/src/main/java/com/gw/api/dto/profile/SaveNavigationFavoriteRequest.java`

---

## Step 5. Backend — service 로직 추가

- [ ] 본인 계정 기준 즐겨찾기 조회 로직 추가
- [ ] 저장 시 허용 메뉴 화이트리스트 검증
- [ ] 관리자 전용 메뉴 경로 저장 권한 검증
- [ ] 잘못된 경로/중복/개수 초과 처리

세부 규칙:

- 비관리자는 `/admin/*` 저장 불가
- 권한 변경으로 기존 admin 메뉴가 남은 경우 조회 시 필터링
- 저장값이 비어 있으면 `[]` 반환

예상 파일:

- `gw-home-api/src/main/java/com/gw/api/service/profile/ProfileService.java`

---

## Step 6. Backend — controller API 추가

- [ ] `GET /api/v1/profiles/me/navigation-favorites`
- [ ] `PUT /api/v1/profiles/me/navigation-favorites`

원칙:

- 인증 필요
- 본인 설정만 처리

예상 파일:

- `gw-home-api/src/main/java/com/gw/api/controller/profile/ProfileController.java`

---

## Step 7. Frontend — 공통 메뉴 메타 정의 분리

- [ ] 헤더/사이드가 공통으로 쓰는 메뉴 정의 작성
- [ ] 일반 메뉴 / 관리자 메뉴 / 즐겨찾기 가능 여부 메타 포함
- [ ] route path 와 label 을 한 소스에서 관리

메타 후보:

- `key`
- `to`
- `label`
- `adminOnly`
- `favoriteEnabled`

예상 파일:

- `gw-home-ui/constants/navigation-menu.ts`
  또는
- `gw-home-ui/composables/useNavigationMenu.ts`

---

## Step 8. Frontend — 타입 / API composable 추가

- [ ] 즐겨찾기 API 타입 정의 추가
- [ ] 조회/저장 composable 추가
- [ ] 헤더/사이드가 함께 사용할 상태 관리 추가

필수 기능:

- `fetchNavigationFavorites()`
- `saveNavigationFavorites(favoriteMenus: string[])`
- 현재 사용자 즐겨찾기 전역 상태 공유

예상 파일:

- `gw-home-ui/types/api/navigation-favorite.ts`
- `gw-home-ui/composables/useNavigationFavoriteApi.ts`
- `gw-home-ui/stores/navigation-favorite.ts` 또는 대응 composable

---

## Step 9. Frontend — AppSidebarNavigation 즐겨찾기 토글

- [ ] 각 메뉴 항목 우측에 별표 토글 추가
- [ ] 별표 클릭 시 이동 이벤트와 분리
- [ ] 저장 중 상태 또는 즉시 반영 UX 결정
- [ ] 관리자 메뉴는 권한 있을 때만 즐겨찾기 가능

UX 기준:

- 링크 클릭: 페이지 이동
- 별표 클릭: 즐겨찾기 토글
- 토글 성공 시 헤더와 즉시 동기화

예상 파일:

- `gw-home-ui/components/common/AppSidebarNavigation.vue`

---

## Step 10. Frontend — AppHeader 즐겨찾기 노출

- [ ] 헤더에 즐겨찾기 링크 그룹 추가
- [ ] 최대 노출 개수 제한
  - 권장: 4
- [ ] 모바일에서는 축약 또는 숨김 처리

표시 기준:

- 저장된 즐겨찾기 중 권한과 메뉴 정의에 맞는 항목만 노출

예상 파일:

- `gw-home-ui/components/common/AppHeader.vue`

---

## Step 11. Frontend — 앱 초기 로딩 동기화

- [ ] 로그인 후 즐겨찾기 설정 초기 로딩
- [ ] 새로고침 시 헤더/사이드 상태 유지
- [ ] 로그아웃 시 즐겨찾기 상태 초기화

예상 파일:

- `gw-home-ui/app.vue`
- `gw-home-ui/composables/useAuth.ts`
- 상태 관리 파일

---

## Step 12. 검증

- [ ] 일반 사용자
  - 일반 메뉴 즐겨찾기 저장/해제 가능
  - 헤더 반영 확인
  - admin 메뉴 즐겨찾기 불가
- [ ] 관리자 사용자
  - 일반/admin 메뉴 모두 저장 가능
  - 헤더 반영 확인
- [ ] 새로고침 후 유지 확인
- [ ] 로그아웃 후 상태 초기화 확인
- [ ] 모바일 헤더 레이아웃 붕괴 여부 확인

---

## 예상 영향 파일

### Backend

- `gw-home-infra-db/src/main/resources/sql/ddl/profile/tb_mbr_prfl_add_fav_menu_json.sql`
- `gw-home-share/src/main/java/com/gw/share/vo/profile/PrflVo.java`
- `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/profile/ProfileMapper.java`
- `gw-home-infra-db/src/main/resources/mapper/profile/ProfileMapper.xml`
- `gw-home-api/src/main/java/com/gw/api/dto/profile/NavigationFavoriteResponse.java`
- `gw-home-api/src/main/java/com/gw/api/dto/profile/SaveNavigationFavoriteRequest.java`
- `gw-home-api/src/main/java/com/gw/api/service/profile/ProfileService.java`
- `gw-home-api/src/main/java/com/gw/api/controller/profile/ProfileController.java`

### Frontend

- `gw-home-ui/constants/navigation-menu.ts`
- `gw-home-ui/types/api/navigation-favorite.ts`
- `gw-home-ui/composables/useNavigationFavoriteApi.ts`
- `gw-home-ui/stores/navigation-favorite.ts`
- `gw-home-ui/components/common/AppSidebarNavigation.vue`
- `gw-home-ui/components/common/AppHeader.vue`
- `gw-home-ui/app.vue`
- `gw-home-ui/composables/useAuth.ts`

---

## 보류 / 결정 필요

- [ ] 저장 포맷을 `TEXT JSON 문자열`로 갈지, 별도 테이블로 갈지
  - 현재 권장: `tb_mbr_prfl.fav_menu_json TEXT`
- [ ] 헤더 최대 노출 개수
  - 현재 권장: 4
- [ ] 모바일에서 즐겨찾기 숨김 여부
  - 현재 권장: 768px 이하에서는 일부만 표시 또는 숨김

