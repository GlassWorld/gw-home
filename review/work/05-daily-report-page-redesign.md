# Review: 일일보고 작성 페이지 전환 및 개인 Git 계정관리 재설계

> 작업 레벨: **HEAVY**
> 레이어: **Full-stack**
> 관련 review: `review/work/02-daily-report.md`
> 관련 todo: `todo/work/08-daily-report-page-redesign.md` (재작성 필요)

---

## 작업 개요

초기 요청에서는 업무 단위로 Git 연동 정보를 등록하고, 일일보고 작성 시 해당 업무 기준 커밋을 불러오는 흐름으로 보였다.
하지만 변경사항이 반영되면서 핵심 개념이 바뀌었다.

이제 필요한 구조는 다음과 같다.

- 사용자 → Git 계정 여러 개
- Git 계정 → 프로젝트 여러 개
- 업무 → 프로젝트 여러 개

즉, Git token / 계정은 업무 소속 정보이 아니라 **개인 자산**이다.
따라서 단순히 업무에 Git 설정 필드를 붙이는 방식은 장기적으로 맞지 않고, 개인의 Git 계정관리와 프로젝트 연결 구조를 먼저 세운 뒤 업무가 프로젝트를 참조하는 방식으로 재설계해야 한다.

---

## 변경 목적

- 업무별 중복 토큰 입력을 없애고 개인 단위 Git 자산을 재사용 가능하게 정리
- 하나의 Git 계정으로 여러 저장소를 관리하고, 여러 업무가 같은 프로젝트를 참조할 수 있게 구조화
- 일일보고에서 업무 선택 시 해당 업무에 연결된 프로젝트들의 커밋을 자연스럽게 조회 가능하도록 기반 마련
- 향후 "개인 Git 계정관리" 화면과 "프로젝트 연결 관리" 화면으로 확장 가능한 모델 확보

---

## 핵심 변경사항

### 1. Git token 저장 위치가 바뀐다

기존 해석:
- 업무가 provider / repository / author / token 을 가진다

변경 후:
- 사용자가 Git 계정 여러 개를 가진다
- token 은 Git 계정에 속한다

이 차이는 매우 크다.
같은 사용자가 여러 업무에서 같은 Git 계정을 반복 사용해야 할 수 있으므로, 업무에 token 을 직접 매달면 중복 저장과 유지보수 비용이 커진다.

### 2. repository 는 "Git 계정 하위 프로젝트"로 다뤄야 한다

기존 해석:
- 업무가 Git 저장소 URL 여러 개를 가진다

변경 후:
- Git 계정이 여러 프로젝트를 가진다
- 업무는 프로젝트 여러 개와 연결된다

즉, 업무는 더 이상 Git API 설정의 소유자가 아니라 **프로젝트 참조자** 가 된다.

### 3. 개인의 "Git 계정관리" 기능이 별도 필요하다

이제는 업무등록 화면 확장만으로는 부족하다.
최소한 아래 중 하나는 필요하다.

- `/settings/git-accounts` 같은 개인 Git 계정관리 화면
- 또는 `/work/git-accounts` 같은 개인 연동 관리 메뉴

여기서 해야 할 일:

- provider 선택
- 계정 식별용 이름/라벨 입력
- author name 입력
- access token 암호화 저장
- 계정별 프로젝트 등록 또는 동기화

---

## 현재 판단: 기존 구현 방향과 충돌

현재 작업 기준으로는 `업무 1 : Git 연동 N` 방향으로 이미 구현/계획이 진행된 상태다.
하지만 이번 변경사항이 맞다면 그 방향은 개념적으로 어긋난다.

충돌 지점:

- 업무가 token 을 직접 가진다는 전제
- 업무가 repository 연결의 소유자라는 전제
- 업무불러오기에서 업무 자체의 Git 설정을 읽는 흐름

이 구조는 지금 요구한 개념과 다르므로, 그대로 밀고 가면 재작업 가능성이 매우 높다.

---

## 재설계 권장 개념 모델

### 1. 사용자 Git 계정

역할:
- 개인이 소유한 Git provider 연동 정보

예상 필드:
- `provider`
- `account label` 또는 `account name`
- `author name`
- `encrypted access token`
- `useYn`

주의:
- token 은 원문 비노출
- author 기준은 이름(name)
- provider 는 선택형

### 2. Git 프로젝트

역할:
- 특정 Git 계정으로 접근 가능한 repository

예상 필드:
- `git account idx`
- `repository url`
- `project name`
- 필요 시 `owner/repository` 또는 provider 별 project path

주의:
- 프로젝트는 Git 계정 하위에 속함
- 동일 저장소가 다른 계정으로 중복 연결될 수 있는지 정책 결정 필요

### 3. 업무-프로젝트 매핑

역할:
- 업무 하나가 여러 프로젝트를 참조

예상 구조:
- `work_unit_idx`
- `git_project_idx`

주의:
- 업무는 Git 계정/토큰을 직접 소유하지 않음
- 프로젝트 연결만 유지

---

## 설계 시 주의점

### 1. 도메인 범위가 넓어진다

이제 단순 `work` 화면 개편이 아니라 아래가 함께 필요하다.

- 개인 Git 계정관리 UI/API
- Git 프로젝트 관리 또는 선택 UI/API
- 업무등록에서 프로젝트 다중 연결 UI/API
- 일일보고에서 업무 기준 프로젝트 커밋 통합 조회

즉, 이번 작업은 기존보다 더 명확한 HEAVY다.

### 2. 업무등록이 아니라 "개인 Git 계정관리"가 선행이어야 한다

이제 순서는 다음이 자연스럽다.

1. 개인 Git 계정관리 구조 추가
2. Git 계정 하위 프로젝트 구조 추가
3. 업무와 프로젝트 매핑 추가
4. 일일보고 작성 페이지에서 업무 기준 커밋 조회 연결

### 3. 커밋 조회 기준도 바뀐다

이전에는 "업무에 붙은 Git 설정 목록" 기준으로 조회했다.
이제는 아래 기준이 된다.

- 업무에 연결된 프로젝트 목록
- 각 프로젝트가 속한 Git 계정의 token 사용
- 프로젝트별 commit 조회 후 통합
- author name 으로 최종 필터링

### 4. UI 진입점도 재설계가 필요하다

필요한 화면 축:

- 개인 Git 계정관리
- Git 계정별 프로젝트 등록/조회
- 업무등록에서 프로젝트 연결
- 일일보고 작성 페이지

즉, 일일보고 개편만 따로 떼어 구현하면 중간 상태가 어색할 수 있다.

---

## 예상 영향 범위

| 구분 | 변경 내용 |
|------|-----------|
| Frontend | 개인 Git 계정관리 화면 신규 필요 |
| Frontend | 업무등록 화면은 Git 계정 직접 입력이 아니라 프로젝트 연결 중심으로 변경 |
| Frontend | 일일보고 작성 페이지는 업무 기준 프로젝트 커밋 조회 구조로 변경 |
| Backend API | Git 계정 CRUD API 필요 |
| Backend API | Git 프로젝트 CRUD 또는 조회 API 필요 |
| Backend API | 업무-프로젝트 매핑 API/DTO 확장 필요 |
| Backend DB | Git 계정, Git 프로젝트, 업무-프로젝트 매핑 테이블 필요 |
| External integration | provider API 를 Git 계정 기준으로 호출 |

---

## 권장 1차 범위

- 개인 Git 계정관리 도입
- access token 암호화 저장
- Git 계정 하위 프로젝트 저장 구조 도입
- 업무등록에서 프로젝트 다중 연결 가능하게 변경
- 일일보고 작성 페이지 전환
- 업무불러오기에서 업무 기준 프로젝트들의 커밋 통합 조회
- 프로젝트 연결이 없는 업무는 업무명만 헤더 세팅

---

## 범위 제외 권장

- 프로젝트 자동 동기화 고도화
- 브랜치 선택
- commit diff 상세 보기
- 주간보고 화면 동시 개편
- 관리자 Git 계정 관리 기능

---

## 예상 수정 파일 범위

### Frontend

- `gw-home-ui/pages/work/daily-reports/*`
- `gw-home-ui/pages/work/index.vue`
- `gw-home-ui/components/work/WorkUnitForm.vue`
- `gw-home-ui/types/work.ts`
- `gw-home-ui/composables/useWorkUnitApi.ts`
- 개인 Git 계정관리용 신규 page / component / composable 추가 필요

### Backend

- `gw-home-api/src/main/java/com/gw/api/dto/work/*`
- `gw-home-api/src/main/java/com/gw/api/service/work/*`
- `gw-home-api/src/main/java/com/gw/api/controller/work/*`
- `gw-home-infra-db/src/main/java/com/gw/infra/db/mapper/work/*`
- `gw-home-infra-db/src/main/resources/mapper/work/*`
- `gw-home-share/src/main/java/com/gw/share/vo/work/*`
- `gw-home-infra-db/src/main/resources/sql/ddl/work/*`

---

## 결론

이번 변경사항을 반영하면, 기존의 "업무가 Git 설정을 가진다"는 전제는 더 이상 맞지 않는다.
핵심은 `일일보고 화면 개편`이 아니라, 그보다 앞단의 `개인 Git 계정관리 + 프로젝트 연결 구조`다.

따라서 이후 계획은 반드시 아래 기준으로 다시 써야 한다.

1. 사용자 Git 계정관리
2. Git 계정 하위 프로젝트 관리
3. 업무-프로젝트 매핑
4. 일일보고 작성 페이지와 커밋 조회 연결
