<script setup lang="ts">
const { toastList, removeToast } = useToast()
</script>

<template>
  <Teleport to="body">
    <div class="toast-viewport" aria-live="polite" aria-relevant="additions">
      <TransitionGroup name="toast" @before-leave="disableLeavingElement">
        <div v-for="toast in toastList" :key="toast.id" :class="['toast-viewport__item', `toast-viewport__item--${toast.variant}`]">
          <span>{{ toast.message }}</span>
          <button type="button" class="toast-viewport__close" aria-label="알림 닫기" @click="removeToast(toast.id)">×</button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-viewport { position: fixed; top: calc(var(--header-height) + 16px); right: max(16px, env(safe-area-inset-right)); z-index: 1100; display: grid; gap: 12px; width: min(380px, calc(100% - 32px)); pointer-events: none; }
.toast-viewport__item { display: flex; align-items: center; gap: 12px; padding: 8px 8px 8px var(--card-padding); border: 1px solid var(--color-border); border-left: 4px solid var(--color-primary); border-radius: var(--radius-medium); background: var(--color-surface); color: var(--color-text); box-shadow: var(--shadow-raised); pointer-events: auto; }
.toast-viewport__item > span { flex: 1; min-width: 0; overflow-wrap: anywhere; font-size: 0.875rem; }
.toast-viewport__item--success { border-left-color: var(--color-success); }
.toast-viewport__item--error { border-left-color: var(--color-danger); }
.toast-viewport__close { flex: none; width: 44px; height: 44px; border: 0; border-radius: var(--radius-small); background: transparent; color: var(--color-text-muted); font-size: 1.5rem; }
.toast-viewport__close:hover { background: var(--color-surface-muted); }
</style>
