<script setup lang="ts">
import { useId } from 'vue'

const props = withDefaults(defineProps<{
  visible: boolean
  title: string
  eyebrow?: string
  width?: string
  zIndex?: number
  immersive?: boolean
  dismissible?: boolean
}>(), { eyebrow: '', width: '720px', zIndex: 30, immersive: false, dismissible: true })
const emit = defineEmits<{ close: [] }>()
const panelElement = ref<HTMLElement | null>(null)
const titleId = `modal-title-${useId()}`

// 닫기가 허용된 창에서만 배경 클릭과 Escape를 처리한다.
function requestClose() {
  if (props.dismissible) emit('close')
}
useOverlayLayer(() => props.visible, panelElement, requestClose, () => props.zIndex)
</script>

<template>
  <Teleport to="body">
    <Transition name="modal" @before-leave="disableLeavingElement">
      <div v-if="visible" class="base-modal" :class="{ 'base-modal--immersive': immersive }" :style="{ zIndex }">
        <div class="base-modal__backdrop" aria-hidden="true" @click="requestClose" />
        <section
          ref="panelElement"
          class="base-modal__panel"
          :class="{ 'base-modal__panel--immersive': immersive }"
          :style="{ '--base-modal-width': width }"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="titleId"
          tabindex="-1"
        >
          <header class="base-modal__header">
            <div class="base-modal__title-box">
              <p v-if="eyebrow" class="base-modal__eyebrow">{{ eyebrow }}</p>
              <div class="base-modal__title-row">
                <h2 :id="titleId" class="base-modal__title">{{ title }}</h2>
                <slot name="title-extra" />
              </div>
            </div>
            <CommonBaseButton v-if="dismissible" class="base-modal__close" variant="secondary" aria-label="닫기" @click="requestClose">
              <span aria-hidden="true">×</span>
            </CommonBaseButton>
          </header>
          <div class="base-modal__body"><slot /></div>
          <footer v-if="$slots.actions" class="base-modal__actions"><slot name="actions" /></footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.base-modal {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: max(var(--panel-padding), env(safe-area-inset-top)) max(var(--panel-padding), env(safe-area-inset-right)) max(var(--panel-padding), env(safe-area-inset-bottom)) max(var(--panel-padding), env(safe-area-inset-left));
}
.base-modal__backdrop { position: absolute; inset: 0; background: var(--color-overlay); }
.base-modal__panel {
  position: relative;
  display: flex;
  flex-direction: column;
  width: min(100%, var(--base-modal-width));
  min-width: 0;
  max-height: calc(100dvh - 40px - env(safe-area-inset-top) - env(safe-area-inset-bottom));
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-large);
  background: var(--color-surface);
  box-shadow: var(--shadow-raised);
}
.base-modal__panel--immersive { height: calc(100dvh - 40px); }
.base-modal__header {
  display: flex;
  flex: none;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--section-gap);
  padding: var(--panel-padding) var(--panel-padding);
  border-bottom: 1px solid var(--color-border);
}
.base-modal__title-box, .base-modal__title-row { min-width: 0; }
.base-modal__title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.base-modal__eyebrow { margin: 0 0 4px; color: var(--color-primary); font-size: 0.75rem; font-weight: 700; }
.base-modal__title { margin: 0; font-size: 1.25rem; line-height: 1.5; }
.base-modal__close { flex: none; width: var(--button-height); padding: 0; font-size: 1.5rem; }
.base-modal__body {
  display: grid;
  gap: var(--section-gap);
  min-height: 0;
  padding: var(--panel-padding);
  overflow-y: auto;
  overscroll-behavior: contain;
}
.base-modal__actions {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px var(--panel-padding);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}
@media (max-width: 767px) {
  .base-modal { padding: 12px; }
  .base-modal__panel { max-height: calc(100dvh - 24px - env(safe-area-inset-bottom)); }
  .base-modal__panel--immersive { height: calc(100dvh - 24px - env(safe-area-inset-bottom)); }
  .base-modal__header, .base-modal__body, .base-modal__actions { padding: var(--mobile-panel-padding); }
  .base-modal__actions { padding-bottom: max(12px, env(safe-area-inset-bottom)); }
  .base-modal__actions :deep(.base-button) { flex: 1 1 auto; }
}
</style>
