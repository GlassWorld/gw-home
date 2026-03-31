# 로그인 화면 개편 — 리뷰

> 관련 파일: `gw-home-ui/pages/login.vue`, `gw-home-ui/components/auth/LoginForm.vue`

---

## 작업 개요

로그인 페이지를 "Glass World" 브랜드 아이덴티티에 맞게 전면 개편한다.
현재 베이지/크래프트 계열 테마를 **파란 유리(glass morphism)** 느낌으로 전환하고,
**Glass World 타이틀을 페이지 중앙에 강조**하며, 로그인 박스는 가볍고 투명하게 표현한다.

---

## 현재 상태 분석

| 항목 | 현재 |
|------|------|
| 레이아웃 | 좌우 2단 (intro 텍스트 + 폼) |
| 타이틀 | `GW HOME COMMUNITY` (eyebrow 스타일, 작은 강조) |
| 배경 | 베이지/크림 계열 (`#f4efe6`) |
| 패널 | `content-panel` 글로벌 클래스 (배경 크림색, 갈색 그림자) |
| 폼 박스 | 별도 시각 분리 없음 (2단 레이아웃 내 우측) |
| CSS 변수 | `main.css` — 전체 사이트 공통 (orange/brown 팔레트) |

---

## 변경 목적

- 브랜드명 **Glass World**를 첫인상에서 강하게 인지시킨다
- 파란 유리 톤(icy blue glass)으로 "유리세계" 컨셉 시각화
- 로그인 박스를 frosted glass 카드로 가볍게 표현 → 무거운 solid 패널 탈피

---

## 예상 영향 범위

| 파일 | 변경 내용 | 범위 |
|------|-----------|------|
| `pages/login.vue` | 레이아웃 재구성, 타이틀 강조, scoped CSS 추가 | 로그인 페이지 전용 |
| `components/auth/LoginForm.vue` | 폼 scoped 스타일 조정 (input/button 색상) | 로그인 폼 전용 |
| `assets/styles/main.css` | **건드리지 않음** (전역 영향 방지) | 없음 |
| `login.vue` scoped | `body` 배경, `.content-panel` 크림/갈색 shadow 전면 override | 로그인 페이지 전용 |

> `main.css` 공통 변수는 변경하지 않는다.
> 단, `main.css`의 크림/갈색 팔레트(`.content-panel`, `.button-primary`, `.input-field`, `body` 배경)가
> 로그인 페이지에 그대로 적용되므로, `login.vue`와 `LoginForm.vue` scoped CSS에서
> **파란 유리 팔레트로 전면 override** 한다. 글로벌 클래스에 의존하지 않고 로그인 전용 클래스로 직접 스타일링.

---

## 설계 방향

### 레이아웃
- 2단 레이아웃 → **1단 중앙 집중형** (full-screen centered)
- 상단: **Glass World**대형 타이틀 (서브타이틀 포함)
- 하단: 로그인 폼 카드 (frosted glass)

### 배경
- 딥 네이비 → 파란 그라데이션 배경
- 파티클/광택 효과는 CSS로 처리 (JS 불필요)

### 타이틀
```
Glass World         ← 대형, 흰색, 가볍게 glow
유리 세계에 오신 것을 환영합니다   ← 서브, 연한 하늘색
```

### 로그인 박스
- `backdrop-filter: blur(...)` + 반투명 흰색 배경
- 테두리: 얇은 하늘색 반투명 border
- 그림자: 파란 계열 glow

### 컬러 팔레트 (로그인 전용)
| 역할 | 값 |
|------|----|
| 배경 | `linear-gradient(135deg, #0a1628 0%, #0d2847 50%, #0a3d62 100%)` |
| 카드 배경 | `rgba(255,255,255,0.08)` |
| 카드 border | `rgba(147,210,255,0.25)` |
| 타이틀 | `#ffffff` |
| 서브텍스트 | `rgba(147,210,255,0.8)` |
| 버튼 | `linear-gradient(135deg,#1a7fc4,#0d5c9e)` |
| input bg | `rgba(255,255,255,0.1)` |
| input text | `#ffffff` |
| input border | `rgba(147,210,255,0.3)` |

---

## 리스크

| 리스크 | 수준 | 대응 |
|--------|------|------|
| `main.css` 글로벌 스타일과 충돌 | 낮음 | scoped CSS + 필요 시 `:deep()` 최소 사용 |
| `button-primary`, `input-field` 글로벌 클래스 색상 덮어씌우기 | 낮음 | login.vue scoped에서 element 직접 스타일링 |
| 모바일 가독성 | 낮음 | 대형 타이틀에 `clamp()` 적용 |

---

## 작업 분류

**NORMAL** — 단일 도메인 내 복수 파일 (login.vue + LoginForm.vue), 공통 모듈 미변경
