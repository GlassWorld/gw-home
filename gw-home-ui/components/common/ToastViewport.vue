<script setup lang="ts">
const { toastList, removeToast } = useToast()
</script>

<template>
  <Teleport to="body">
    <div v-if="toastList.length" class="toast-viewport" aria-live="polite" aria-atomic="true">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toastList"
          :key="toast.id"
          :class="['toast-viewport__item', `toast-viewport__item--${toast.variant}`]"
        >
          <span>{{ toast.message }}</span>
          <button type="button" class="toast-viewport__close" @click="removeToast(toast.id)">
            X
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-viewport {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1100;
  display: grid;
  gap: 10px;
  width: min(360px, calc(100vw - 24px));
}

.toast-viewport__item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-medium);
  border: 1px solid rgba(147, 210, 255, 0.18);
  background: rgba(7, 21, 39, 0.94);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(14px);
}

.toast-viewport__item--success {
  border-color: rgba(29, 107, 99, 0.34);
  color: #8ee9bf;
}

.toast-viewport__item--error {
  border-color: rgba(184, 59, 51, 0.28);
  color: #ffb3b3;
}

.toast-viewport__item--info {
  color: var(--color-text);
}

.toast-viewport__close {
  flex: none;
  min-width: auto;
  min-height: auto;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  opacity: 0.7;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.2s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 768px) {
  .toast-viewport {
    top: 14px;
    right: 12px;
    left: 12px;
    width: auto;
  }
}
</style>
