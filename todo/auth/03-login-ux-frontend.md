# 로그인 UX 기능 추가 (Part A — 프론트) — Todo

> 리뷰 문서: [`review/auth/03-login-ux-features.md`](../../review/auth/03-login-ux-features.md)
> 연관 todo: [`todo/auth/04-login-account-lock.md`](04-login-account-lock.md) (Part B)
> 작업 분류: NORMAL

---

## 작업 범위

- `gw-home-ui/components/auth/LoginForm.vue` 만 수정
- Backend / DB 변경 없음

---

## Todo

### 1. 로그인 아이디 저장 체크박스
- [ ] 체크박스 UI 추가 (`label` + `input[type=checkbox]`)
- [ ] 체크 시 `localStorage.setItem('saved-login-id', loginId)` 저장
- [ ] 체크 해제 시 `localStorage.removeItem('saved-login-id')` 제거
- [ ] 컴포넌트 마운트 시 `localStorage` 에서 값 읽어 `loginId` 초기값 및 체크 상태 복원
- [ ] 체크박스 스타일 파란 유리 팔레트에 맞게 적용

### 2. Caps Lock 감지
- [ ] 비밀번호 input 에 `@keyup` 이벤트 바인딩
- [ ] `event.getModifierState('CapsLock')` 으로 상태 감지
- [ ] Caps Lock ON 시 input 하단에 경고 문구 표시
  - "Caps Lock이 켜져 있습니다"
- [ ] Caps Lock OFF 시 경고 문구 숨김
- [ ] 경고 문구 스타일 (노란/주황 계열 — 어두운 배경에서 가시성 확보)
