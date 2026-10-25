// 퇴장 애니메이션 동안 남아 있는 요소의 클릭·키보드 접근을 막는다.
export function disableLeavingElement(element: Element) {
  element.setAttribute('inert', '')
  element.setAttribute('aria-hidden', 'true')
}
