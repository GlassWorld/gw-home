# Todo: 전체 테마 색감 통일 (로그인 → 앱 전체)

> **연관 문서**: [review/ui/01-dark-theme-align.md](../../review/ui/01-dark-theme-align.md)
> **작업 분류**: NORMAL
> **도메인**: ui (공통 스타일)

---

## Phase 1 — CSS 토큰 교체 (main.css)

- [x] **1-1** `assets/styles/main.css` — CSS 변수 전체 교체
  - `--color-background`: `#f4efe6` → `#0a1628`
  - `--color-surface`: 베이지 → `rgba(255, 255, 255, 0.07)`
  - `--color-surface-strong`: `#fffdf8` → `rgba(255, 255, 255, 0.12)`
  - `--color-border`: 갈색 계열 → `rgba(147, 210, 255, 0.18)`
  - `--color-text`: `#2c241c` → `#e8f4ff`
  - `--color-text-muted`: `#746553` → `rgba(147, 210, 255, 0.65)`
  - `--color-primary`: `#c45c2d` → `#3a9fd6`
  - `--color-primary-strong`: `#9f421a` → `#1d7cb8`
  - `--color-accent`: `#1d6b63` → `#6ec1ff`
  - `--color-danger`: `#b83b33` → `#e05c5c`
  - `--shadow-soft`: 갈색 그림자 → `rgba(2, 12, 27, 0.45)` 네이비 그림자
- [x] **1-2** `body` 배경 교체
  - 베이지 radial+linear → `linear-gradient(135deg, #0a1628 0%, #0d2847 50%, #0a3d62 100%)`
- [x] **1-3** `input-field` 스타일 다크 배경 대응
  - 배경 `rgba(255,255,255,0.82)` → `rgba(255,255,255,0.08)`
  - 텍스트 색상, border 색상 다크 테마에 맞게 조정
  - focus outline 블루 계열로 교체
- [x] **1-4** `button-secondary` 스타일 다크 배경 대응
  - 배경 `rgba(255,255,255,0.58)` → `rgba(255,255,255,0.1)`
  - border 색상 `--color-border` 기반 유지 (토큰 교체로 자동 반영 확인)

---

## Phase 2 — AppHeader 하드코딩 색상 교체

- [x] **2-1** `components/common/AppHeader.vue` — 헤더 배경 교체
  - `.app-header` 배경 `rgba(249, 244, 234, 0.8)` → `rgba(10, 22, 40, 0.85)`
  - border-bottom `rgba(116, 87, 51, 0.12)` → `rgba(147, 210, 255, 0.12)`

---

## Phase 3 — Dashboard 하드코딩 색상 교체

- [x] **3-1** `pages/dashboard/index.vue` — 프로필 카드 배경 교체
  - `.dashboard-page__profile-card` 배경 `rgba(255, 255, 255, 0.66)` → `rgba(255, 255, 255, 0.06)`

---

## 작업 완료 기준

- 로그인 → 대시보드 전환 시 색감 단절 없음
- 모든 텍스트가 다크 배경 대비 가독성 유지
- 버튼, 입력 필드, 카드, 헤더가 다크 네이비 테마에 자연스럽게 융화
