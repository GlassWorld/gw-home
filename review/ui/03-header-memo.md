# Review: 헤더 메모 기능

> 작업 분류: **HEAVY**
> DB 스키마 변경 + share 모듈 변경 포함

---

## 작업 개요

헤더 계정정보 영역 옆에 메모 버튼 추가.
버튼 클릭 시 모달이 열리고, 개인 메모를 작성/수정/저장할 수 있다.
이전에 저장된 메모가 있으면 모달 오픈 시 자동 표시된다.

---

## 변경 목적

로그인 상태에서 빠르게 접근 가능한 개인 전용 메모 공간 제공.
대시보드 이동 없이 헤더에서 즉시 메모 확인/수정 가능.

---

## 설계 결정 사항

### 1. 메모 저장 위치

**`tb_mbr_prfl` 에 `memo TEXT` 컬럼 추가.**

- 메모는 사용자 1인당 1개 (profile 과 동일 구조)
- `profile` 도메인이 이미 per-user 데이터를 담당하므로 자연스럽게 확장
- 별도 테이블(`tb_mbr_memo`) 신설은 오버엔지니어링

### 2. memo 노출 범위

- `GET /api/v1/profiles/{uuid}` (공개) — **memo 포함 안 함**
- `GET /api/v1/profiles/me/memo` (인증 필요, 본인만) — memo 반환
- `PUT /api/v1/profiles/me/memo` — 메모 저장/수정

기존 `PUT /api/v1/profiles/me` (프로필 수정) API는 변경하지 않는다.

### 3. 메모 저장 방식

최초 작성 시와 수정 모두 `PUT /api/v1/profiles/me/memo` 한 개 엔드포인트로 처리 (upsert 아님, profile 행이 이미 존재하므로 UPDATE).

### 4. 프론트 메모 로드 시점

헤더 메모 버튼 클릭 시 API 호출로 최신 메모를 불러옴 (항상 최신 데이터 보장, 스토어 캐시 미사용).

---

## 예상 영향 범위

### Backend

| 레이어 | 파일 | 변경 내용 |
|--------|------|-----------|
| DDL | `tb_mbr_prfl` | `memo TEXT` 컬럼 추가 |
| share VO | `PrflVo.java` | `memo` 필드 추가 |
| Mapper | `ProfileMapper.java` | `selectMemoByLoginId()`, `updateMemo()` 추가 |
| Mapper XML | `ProfileMapper.xml` | 해당 SQL 추가 |
| Service | `ProfileService.java` | `getMemo()`, `saveMemo()` 추가 |
| Controller | `ProfileController.java` | `GET /me/memo`, `PUT /me/memo` 추가 |
| DTO | `MemoResponse.java` (신규) | `memo: String` |
| DTO | `SaveMemoRequest.java` (신규) | `memo: String` |

> `ProfileResponse` 는 변경하지 않는다 (공개 프로필에 memo 노출 차단).

### Frontend

| 파일 | 변경 내용 |
|------|-----------|
| `components/common/AppHeader.vue` | 메모 버튼 추가, 모달 가시성 제어 |
| `components/common/HeaderMemoModal.vue` (신규) | 메모 작성/수정 모달 |
| `composables/useMemoApi.ts` (신규) | `fetchMemo()`, `saveMemo()` |
| `types/api/memo.ts` (신규) | `MemoApiResponse`, `SaveMemoPayload` |

---

## DB 마이그레이션 체크리스트

- [ ] DDL 변경 SQL 작성
  ```sql
  ALTER TABLE tb_mbr_prfl ADD COLUMN memo TEXT;
  ```
- [ ] 기존 데이터 영향 검토 (NULL 허용 → 기존 프로필 영향 없음)
- [ ] NULL/DEFAULT 정책 확인 (DEFAULT NULL)
- [ ] 인덱스 영향 확인 (불필요)
- [ ] 롤백 SQL 작성
  ```sql
  ALTER TABLE tb_mbr_prfl DROP COLUMN memo;
  ```
- [ ] 운영 반영 순서: DB → 백엔드 → 프론트
- [ ] 데이터 마이그레이션 필요 여부: 불필요

---

## 리스크 분석

| 리스크 | 대응 |
|--------|------|
| 공개 프로필 API에 memo 노출 | `ProfileResponse` 에 memo 필드 추가 안 함 |
| memo 크기 무제한 | `SaveMemoRequest` 에 `@Size(max=2000)` 검증 추가 |
| `PrflVo` (share) 변경 → 전체 profile 관련 코드 영향 | memo 필드 추가만, 기존 로직 무변경 |

---

## 전체 파일 목록

### 신규 (4개)
```
gw-home-api/.../dto/profile/MemoResponse.java
gw-home-api/.../dto/profile/SaveMemoRequest.java
gw-home-ui/components/common/HeaderMemoModal.vue
gw-home-ui/composables/useMemoApi.ts
gw-home-ui/types/api/memo.ts
```

### 변경 (6개)
```
gw-home-share/.../vo/profile/PrflVo.java
gw-home-infra-db/.../mapper/profile/ProfileMapper.java
gw-home-infra-db/.../mapper/profile/ProfileMapper.xml
gw-home-api/.../service/profile/ProfileService.java
gw-home-api/.../controller/profile/ProfileController.java
gw-home-ui/components/common/AppHeader.vue
```

### DDL (1개)
```
gw-home-infra-db/.../sql/ddl/profile/tb_mbr_prfl_add_memo.sql
```

---

## 관련 파일

- `gw-home-infra-db/.../sql/ddl/profile/tb_mbr_prfl.sql`
- `gw-home-share/.../vo/profile/PrflVo.java`
- `gw-home-api/.../controller/profile/ProfileController.java`
- `gw-home-api/.../service/profile/ProfileService.java`
- `gw-home-ui/components/common/AppHeader.vue`
