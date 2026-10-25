import { nextTick, onBeforeUnmount, onMounted, toValue, watch, type MaybeRefOrGetter, type Ref } from 'vue'

interface OverlayLayer {
  key: symbol
  panel: HTMLElement
  priority: number
  close: () => void
}

const layers: OverlayLayer[] = []
let previousOverflow = ''
let previousRootInert = false
const focusableSelector = 'a[href], button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'

// 중첩 창 중 화면 위에 표시되는 창을 찾는다.
function topLayer(): OverlayLayer | undefined {
  return layers.reduce<OverlayLayer | undefined>((top, layer) => !top || layer.priority >= top.priority ? layer : top, undefined)
}

// 현재 창에서 실제로 보이는 키보드 이동 대상을 찾는다.
function focusableElements(panel: HTMLElement): HTMLElement[] {
  return Array.from(panel.querySelectorAll<HTMLElement>(focusableSelector))
    .filter(element => element.tabIndex >= 0 && element.getClientRects().length > 0 && !element.closest('[inert]'))
}

// 가장 위 창에서만 닫기와 키보드 순환을 처리한다.
function handleOverlayKeydown(event: KeyboardEvent) {
  const active = topLayer()
  if (!active || event.defaultPrevented) return
  if (event.key === 'Escape') {
    event.preventDefault()
    active.close()
  } else if (event.key === 'Tab') {
    const elements = focusableElements(active.panel)
    const first = elements[0] ?? active.panel
    const last = elements.at(-1) ?? active.panel
    if (!active.panel.contains(document.activeElement) || (event.shiftKey && document.activeElement === first) || (!event.shiftKey && document.activeElement === last) || !elements.length) {
      event.preventDefault()
      ;(event.shiftKey ? last : first).focus()
    }
  }
}

// 모달과 모바일 메뉴의 배경 스크롤·포커스 수명주기를 공유한다.
export function useOverlayLayer(visible: MaybeRefOrGetter<boolean>, panel: Ref<HTMLElement | null>, close: () => void, priority: MaybeRefOrGetter<number> = 30) {
  const key = Symbol('overlay')
  let previousFocus: HTMLElement | null = null
  let revision = 0

  // 닫힌 창의 잠금을 해제하고 열기 전 위치로 포커스를 돌린다.
  function release() {
    revision++
    const index = layers.findIndex(layer => layer.key === key)
    if (index < 0) return
    const wasTop = topLayer()?.key === key
    layers.splice(index, 1)
    if (!layers.length) {
      document.body.style.overflow = previousOverflow
      const root = document.getElementById('__nuxt')
      if (root) root.inert = previousRootInert
      document.removeEventListener('keydown', handleOverlayKeydown)
    }
    if (wasTop) {
      if (previousFocus?.isConnected && !previousFocus.closest('[inert]')) previousFocus.focus({ preventScroll: true })
      else topLayer()?.panel.focus({ preventScroll: true })
    }
  }

  // 렌더링된 창을 등록하고 닫기 버튼부터 키보드 탐색을 시작한다.
  async function synchronize() {
    if (!toValue(visible)) { release(); return }
    const currentRevision = ++revision
    previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    await nextTick()
    if (currentRevision !== revision || !toValue(visible) || !panel.value || layers.some(layer => layer.key === key)) return
    if (!layers.length) {
      previousOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      const root = document.getElementById('__nuxt')
      previousRootInert = root?.inert ?? false
      if (root) root.inert = true
      document.addEventListener('keydown', handleOverlayKeydown)
    }
    layers.push({ key, panel: panel.value, priority: toValue(priority), close })
    ;(focusableElements(panel.value)[0] ?? panel.value).focus({ preventScroll: true })
  }

  onMounted(() => {
    watch(() => toValue(visible), () => { void synchronize() }, { immediate: true })
  })
  onBeforeUnmount(release)
}
