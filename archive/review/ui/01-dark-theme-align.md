# Review: 전체 테마 색감 통일 (로그인 → 앱 전체)

> **연관 문서**: [todo/ui/01-dark-theme-align.md](../../todo/ui/01-dark-theme-align.md) / work 미생성

---

## 작업 개요

로그인 페이지의 다크 네이비/블루 글라스모피즘 색감을 기준으로,
메인 대시보드 및 앱 전체 배경/색감을 통일한다.

---

## 현재 색감 분석

### 로그인 페이지 (기준 색감)
| 항목 | 값 |
|------|----|
| 배경 | `linear-gradient(135deg, #0a1628, #0d2847, #0a3d62)` — 다크 네이비 |
| 포인트 블루 | `rgba(110, 193, 255, 0.3)`, `rgba(13, 148, 214, 0.28)` |
| 텍스트 | `#ffffff`, `rgba(147, 210, 255, 0.82)` |
| 카드 배경 | `rgba(255, 255, 255, 0.08)` 글라스모피즘 |
| 카드 테두리 | `rgba(147, 210, 255, 0.25)` |

### 현재 앱 전체 (main.css, AppHeader.vue)
| 항목 | 값 |
|------|----|
| 배경 | `#f4efe6`, `#f9f4ea` — 따뜻한 베이지 |
| Primary | `#c45c2d` — 주황-갈색 |
| Accent | `#1d6b63` — 틸 |
| 텍스트 | `#2c241c` — 다크 브라운 |
| 헤더 배경 | `rgba(249, 244, 234, 0.8)` — 베이지 |
| Surface | `rgba(255, 250, 242, 0.92)` — 따뜻한 흰색 |

---

## 변경 목적

로그인 진입 시의 다크 네이비 분위기가 대시보드 진입 후 갑자기 밝고 따뜻한 베이지로 전환되어 시각적 단절이 발생한다.
전체 앱 색감을 로그인의 다크 네이비/블루 글라스모피즘 테마로 통일하여 일관된 UX를 제공한다.

---

## 영향 범위 분석

| 파일 | 변경 유형 | 내용 |
|------|-----------|------|
| `assets/styles/main.css` | CSS 변수 전체 재정의 | 베이지 → 다크 네이비 계열로 모든 색상 토큰 교체 |
| `components/common/AppHeader.vue` | 하드코딩 색상 제거 | `rgba(249, 244, 234, 0.8)` 베이지 배경 → 다크 글라스 배경 |
| `pages/dashboard/index.vue` | 하드코딩 색상 조정 | `.dashboard-page__profile-card` 배경 `rgba(255,255,255,0.66)` → 다크 글라스 |

> `main.css`는 공통 스타일 파일로 모든 페이지/컴포넌트에 즉시 영향.
> 단, CSS 변수 기반 시스템이므로 변수만 교체하면 대부분 자동 반영.
> 하드코딩된 색상 값은 파일별 개별 수정 필요.

---

## 변경 색상 토큰 (안)

| 토큰 | 현재 | 변경 후 |
|------|------|---------|
| `--color-background` | `#f4efe6` | `#0a1628` |
| `--color-surface` | `rgba(255,250,242,0.92)` | `rgba(255,255,255,0.07)` |
| `--color-surface-strong` | `#fffdf8` | `rgba(255,255,255,0.12)` |
| `--color-border` | `rgba(116,87,51,0.18)` | `rgba(147,210,255,0.18)` |
| `--color-text` | `#2c241c` | `#e8f4ff` |
| `--color-text-muted` | `#746553` | `rgba(147,210,255,0.65)` |
| `--color-primary` | `#c45c2d` | `#3a9fd6` |
| `--color-primary-strong` | `#9f421a` | `#1d7cb8` |
| `--color-accent` | `#1d6b63` | `#6ec1ff` |
| `--color-danger` | `#b83b33` | `#e05c5c` |
| `--shadow-soft` | `rgba(69,45,20,0.12)` | `rgba(2,12,27,0.45)` |
| body background | 베이지 radial+linear | `linear-gradient(135deg, #0a1628, #0d2847, #0a3d62)` |

---

## 리스크 분석

| 리스크 | 수준 | 대응 |
|--------|------|------|
| CSS 변수 교체 후 일부 하드코딩 색상 누락 | 중 | 전체 `.vue` 파일 색상값 검색 후 개별 확인 |
| 가독성 저하 (다크 배경 + 기존 텍스트 색) | 중 | 텍스트 색상 토큰도 함께 교체 필수 |
| input/button 등 폼 요소 대비 저하 | 낮 | `input-field` 스타일 다크 배경 대응 필요 |

---

## 영향 받는 모든 페이지

| 페이지 | 파일 |
|--------|------|
| 대시보드 | `pages/dashboard/index.vue` |
| 게시글 목록 | `pages/board/index.vue` |
| 게시글 상세 | `pages/board/[uuid].vue` |
| 게시글 작성/수정 | `pages/board/create.vue` 등 |
| Vault 목록 | `pages/vault/index.vue` |
| 공통 헤더 | `components/common/AppHeader.vue` |

> login.vue는 자체 `<style scoped>`로 `:global(body)` 재정의 중 — 변경 불필요.

---

## 작업 분류

**NORMAL** — 공통 CSS 파일 1개 + 하드코딩 색상 파일 2~3개 수정.
코드 로직 변경 없음, 스타일 토큰 교체 중심.

---

## 구현 예상 파일 목록

1. `gw-home-ui/assets/styles/main.css` — CSS 변수 전체 교체, body 배경 교체
2. `gw-home-ui/components/common/AppHeader.vue` — 헤더 배경 다크 글라스 적용
3. `gw-home-ui/pages/dashboard/index.vue` — 프로필 카드 배경 조정
