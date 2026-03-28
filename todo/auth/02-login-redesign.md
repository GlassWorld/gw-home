# 로그인 화면 개편 — Todo

> 리뷰 문서: [`review/auth/02-login-redesign.md`](../../review/auth/02-login-redesign.md)
> 작업 분류: NORMAL

---

## 작업 범위

- `gw-home-ui/pages/login.vue`
- `gw-home-ui/components/auth/LoginForm.vue`
- `assets/styles/main.css` 는 변경하지 않는다

---

## Todo

### 1. login.vue — 레이아웃 및 글로벌 스타일 override
- [x] 2단 그리드 → 1단 중앙 집중형 레이아웃으로 변경
- [x] `body` 배경색(크림)을 scoped `:global(body)` 로 파란 그라데이션으로 override
  - `linear-gradient(135deg, #0a1628 0%, #0d2847 50%, #0a3d62 100%)`
- [x] `.content-panel` 글로벌 클래스 사용 중단 → 로그인 전용 `.login-card` 클래스로 교체
  - 크림 배경·갈색 그림자 제거, frosted glass 스타일 적용 (`rgba(255,255,255,0.08)` + `backdrop-filter: blur`)
  - 테두리: `rgba(147,210,255,0.25)`, 그림자: 파란 계열 glow
- [x] **Glass World** 대형 타이틀 추가 (흰색, text-shadow glow)
- [x] 서브타이틀 추가 ("유리 세계에 오신 것을 환영합니다", 연한 하늘색)
- [x] 기존 intro 섹션 (`login-page__eyebrow`, `section-title`, `section-description`) 제거

### 2. LoginForm.vue — 폼 스타일 파란 유리 팔레트로 전면 override
- [x] `.input-field` 글로벌 클래스 override — 크림 배경·갈색 테두리 제거
  - 배경: `rgba(255,255,255,0.1)`, 테두리: `rgba(147,210,255,0.3)`, 텍스트: `#fff`
- [x] input focus — `outline`/`border` 파란 계열로 변경
- [x] `.button-primary` 글로벌 클래스 override — 주황 그라데이션 제거
  - 배경: `linear-gradient(135deg, #1a7fc4, #0d5c9e)`, shadow 파란 계열
- [x] label 색상 → `rgba(147,210,255,0.8)` (연한 하늘색)
- [x] placeholder 색상 → `rgba(255,255,255,0.4)`

### 3. 검증
- [x] 모바일(768px 이하) 레이아웃 확인
- [x] 에러 메시지 가시성 확인 (어두운 배경 위 — `message-error` 색상 검토)
