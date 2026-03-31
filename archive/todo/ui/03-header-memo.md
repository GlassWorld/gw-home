# Todo: 헤더 메모 기능

> review: `review/ui/03-header-memo.md`
> 작업 분류: **HEAVY** — DB → 백엔드 → 프론트 순서로 진행

---

## Step 1. DB 스키마 변경

- [x] `tb_mbr_prfl_add_memo.sql` 생성 — 기존 테이블 ALTER 반영
  ```sql
  ALTER TABLE tb_mbr_prfl ADD COLUMN memo TEXT;
  ```
- [x] DB에 직접 실행 확인

---

## Step 2. Backend — share 모듈

- [x] `PrflVo.java` — `memo` 필드 추가
  ```java
  private String memo;
  ```

---

## Step 3. Backend — infra-db 모듈

- [x] `ProfileMapper.java` — 메서드 추가
  - `selectMemoByAccountIdx(mbrAcctIdx: Long): String`
  - `updateMemo(mbrAcctIdx: Long, memo: String, updatedBy: String): int`
- [x] `ProfileMapper.xml` — SQL 추가
  - 기존 SELECT 쿼리에 `memo` 컬럼 포함
  - `updateMemo` UPDATE SQL 작성

---

## Step 4. Backend — DTO

- [x] `MemoResponse.java` 신규 생성 — `memo: String`
- [x] `SaveMemoRequest.java` 신규 생성 — `memo: String`, `@Size(max=2000)`

---

## Step 5. Backend — ProfileService

- [x] `getMemo(loginId): MemoResponse`
  - account 조회 → `selectMemoByAccountIdx` 호출
- [x] `saveMemo(loginId, request): MemoResponse`
  - account 조회 → `updateMemo` 호출 → 저장된 memo 반환

---

## Step 6. Backend — ProfileController

- [x] `GET /api/v1/profiles/me/memo` 추가
- [x] `PUT /api/v1/profiles/me/memo` 추가

---

## Step 7. Frontend — 타입 + composable

- [x] `types/api/memo.ts` 신규 생성
  - `MemoApiResponse { memo: string }`
  - `SaveMemoPayload { memo: string }`
- [x] `composables/useMemoApi.ts` 신규 생성
  - `fetchMemo(): Promise<string>` — GET 호출, `memo` 값 반환
  - `saveMemo(memo: string): Promise<void>` — PUT 호출

---

## Step 8. Frontend — HeaderMemoModal 컴포넌트

- [x] `components/common/HeaderMemoModal.vue` 신규 생성
  - Props: `visible: boolean`
  - 모달 오픈 시 `fetchMemo()` 호출 → textarea에 표시
  - 저장 버튼 클릭 시 `saveMemo()` 호출 → 성공 토스트
  - 로딩/저장 중 상태 처리
  - `CommonBaseModal` 재사용
  - textarea: 기존 `.vault-modal__textarea` 스타일 참고, 최소 높이 200px

---

## Step 9. Frontend — AppHeader 수정

- [x] `components/common/AppHeader.vue` 수정
  - 계정정보(`.app-header__profile`) 옆에 메모 버튼 추가
  - `isMemoVisible` ref 제어
  - `CommonHeaderMemoModal` 추가

---

## 추가 계획 (2026-03-30)

### Step 10. Frontend — 메모 모달 크기 확대

- [x] `HeaderMemoModal.vue` 의 최대 너비 확대
- [x] 모달 본문 최소 높이 또는 viewport 기준 높이 재조정
- [x] textarea 높이와 내부 여백 확대
- [x] 작은 화면에서 과도하게 커지지 않도록 반응형 max-width 확인
- [x] 필요 시 공통 모달 크기 prop 확장 여부 검토

### Step 11. 검증

- [x] 데스크탑에서 기존보다 넓은 작성 공간이 확보되는지 확인
- [x] 저장/닫기 버튼 배치가 깨지지 않는지 확인
- [x] 모바일/태블릿에서 viewport overflow 문제가 없는지 확인
