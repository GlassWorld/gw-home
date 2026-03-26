# Review: 상단 헤더 높이/폭 축소

## 작업 개요

`AppHeader` 컴포넌트의 높이와 내부 여백을 줄여 컨텐츠 영역을 더 넓게 확보.

## 변경 목적

현재 헤더 `min-height: 64px` 이 다소 크게 느껴짐. 슬림하게 축소하여 콘텐츠 노출 영역 확대.

## 예상 영향 범위

| 파일 | 변경 내용 |
|------|-----------|
| `components/common/AppHeader.vue` | `min-height`, `gap`, 폰트 크기 조정 |

## 변경 상세

| 속성 | 현재 | 변경 후 |
|------|------|---------|
| `.app-header__inner` `min-height` | `64px` | `48px` |
| `.app-header__inner` `gap` | `20px` | `16px` |
| `.app-header__brand` `font-size` | `1.2rem` | `1.05rem` |
| 모바일 `padding-top/bottom` | `10px` | `8px` |

## 리스크

- 없음. 단일 컴포넌트 스타일 변경, 기능 영향 없음.

## 작업 분류: **LIGHT**

## 관련 파일

- `gw-home-ui/components/common/AppHeader.vue`
