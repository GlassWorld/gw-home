# Todo: 인증 계약과 관리자 일일보고 정합성 수정

> 연관 문서: [review/work/03-overall-refactor-check.md](../../review/work/03-overall-refactor-check.md)
> 작업 레벨: **HEAVY**
> 레이어: **Backend + Frontend + Docs**

---

## 작업 범위

- 인증 토큰 저장 방식과 문서/프론트/백엔드 계약 정합성 확정
- 관리자 일일보고 목록 조회의 `cntn` 누락 가능성 수정
- 필요한 범위의 테스트와 문서 반영

### 현재 요청 기준 우선 범위

- 인증 흐름 관련 API/프론트 연동부
- 관리자 일일보고 목록 Mapper/Service/화면 응답
- 관련 테스트와 문서

### 현재 요청 기준 범위 제외

- 회원/권한 체계 전면 개편
- 인증 방식의 대규모 재설계
- 일일보고 기능 외 다른 관리자 화면 개편

---

## 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| Backend - auth | refresh token 전달 방식과 응답 계약 점검 |
| Frontend - auth | 토큰 저장/갱신 방식 점검 |
| Backend - work | 관리자 일일보고 목록 조회 컬럼 및 응답 조립 정합성 수정 |
| Docs | 인증 흐름과 관리자 기능 문서 보정 |
| Test | 인증/관리자 일일보고 회귀 테스트 정리 |

---

## 설계 방향

### 1. 인증은 문서와 구현 중 하나를 기준으로 확정한다

- 현재 구현을 문서에 맞출지
- 문서를 현재 구현에 맞출지

이 선택 없이 부분 수정만 하면 다시 어긋난다.

### 2. 관리자 목록은 조회 컬럼과 응답 fallback을 함께 맞춘다

- `cntn`을 조회에 포함할지
- 응답 조립에서 fallback 기준을 바꿀지

둘 중 하나만 바꾸지 않고 쿼리와 서비스 로직을 함께 정리한다.

### 3. 테스트는 계약 기준으로 다시 묶는다

- 인증 응답 구조
- refresh token 저장/갱신 흐름
- 관리자 일일보고 목록 응답 내용

문서, 구현, 테스트가 같은 기준을 보도록 맞춘다.

---

## 구현 체크리스트

### 1. 인증 계약 정리

- [x] 로그인/토큰 갱신 응답 구조 확인
- [x] 프론트 `useAuth`의 refresh token 저장 방식 확인
- [x] 문서와 실제 구현의 차이 확정
- [x] 선택된 기준에 따라 문서 또는 구현 수정

### 2. 관리자 일일보고 정합성 수정

- [x] 관리자 목록 Mapper의 조회 컬럼 재점검
- [x] `cntn`, `spcl_note` fallback 규칙 정리
- [x] 서비스 응답 조립 로직 보정
- [x] 관리자 화면 표시값 회귀 확인

### 3. 테스트

- [x] 인증 관련 테스트 기대값 재정리
- [x] 관리자 일일보고 목록 테스트 추가 또는 보강
- [x] 프론트 타입/호출부 영향 확인

### 4. 문서

- [x] `docs/frontend/auth-flow.md` 반영
- [x] 관리자 일일보고 관련 문서 메모 반영 여부 검토

---

## 진행 결과

- 인증 계약은 현재 구현을 기준으로 문서를 정리했다.
- 관리자 일일보고 목록 조회에 `cntn` 컬럼을 포함시켜 `note` fallback이 실제로 동작하도록 맞췄다.
- 관리자 화면은 기존 표시 로직을 유지하면서 빈 특이사항 보고서도 본문 대체값이 보이도록 정합성을 맞췄다.

---

## 후보 대상 파일 메모

- `gw-home-api/src/main/java/com/gw/api/controller/auth/AuthController.java`
- `gw-home-api/src/main/java/com/gw/api/service/auth/AuthService.java`
- `gw-home-api/src/main/java/com/gw/api/dto/auth/TokenResponse.java`
- `gw-home-ui/composables/use-auth.ts`
- `gw-home-infra-db/src/main/resources/mapper/work/DailyReportMapper.xml`
- `gw-home-api/src/main/java/com/gw/api/service/work/DailyReportService.java`
- `gw-home-share/src/main/java/com/gw/share/jvo/work/DailyReportAdmJvo.java`
- `docs/frontend/auth-flow.md`
