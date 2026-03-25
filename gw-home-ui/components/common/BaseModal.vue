<script setup lang="ts">
const props = withDefaults(defineProps<{
  visible: boolean
  title: string
  eyebrow?: string
  width?: string
}>(), {
  eyebrow: '',
  width: '720px'
})

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="base-modal">
      <div class="base-modal__backdrop" @click="emit('close')" />
      <section class="base-modal__panel content-panel" :style="{ maxWidth: width }">
        <header class="base-modal__header">
          <div class="base-modal__title-box">
            <p v-if="eyebrow" class="base-modal__eyebrow">{{ eyebrow }}</p>
            <div class="base-modal__title-row">
              <h2 class="base-modal__title">{{ title }}</h2>
              <slot name="title-extra" />
            </div>
          </div>

          <CommonBaseButton
            class="base-modal__close"
            variant="secondary"
            size="small"
            type="button"
            aria-label="닫기"
            @click="emit('close')"
          >
            <span aria-hidden="true">X</span>
          </CommonBaseButton>
        </header>

        <div class="base-modal__body">
          <slot />
        </div>

        <footer v-if="$slots.actions" class="base-modal__actions">
          <slot name="actions" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.base-modal {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 20px;
}

.base-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(25, 20, 15, 0.4);
}

.base-modal__panel {
  position: relative;
  width: min(100%, 760px);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 20px;
  display: grid;
  gap: 16px;
}

.base-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.base-modal__title-box {
  display: grid;
  gap: 4px;
}

.base-modal__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.base-modal__eyebrow {
  margin: 0;
  color: var(--color-accent);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.base-modal__title {
  margin: 0;
  min-width: 0;
  font-size: clamp(1.12rem, 2vw, 1.4rem);
  line-height: 1.2;
}

.base-modal__body {
  display: grid;
  gap: 16px;
}

.base-modal__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.base-modal__close {
  min-width: auto;
  min-height: auto;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.base-modal__close:hover {
  transform: none;
}

@media (max-width: 768px) {
  .base-modal {
    padding: 12px;
  }

  .base-modal__panel {
    padding: 16px;
    gap: 14px;
  }

  .base-modal__actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    justify-content: initial;
  }

  .base-modal__actions :deep(button) {
    width: 100%;
  }
}
</style>
