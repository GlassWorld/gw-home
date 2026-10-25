<script setup lang="ts">
withDefaults(defineProps<{
  state: 'loading' | 'empty' | 'error' | 'success'
  title?: string
  message: string
  actionLabel?: string
}>(), { title: '', actionLabel: '' })
defineEmits<{ action: [] }>()
</script>

<template>
  <div class="base-state" :class="`base-state--${state}`" :role="state === 'error' ? 'alert' : 'status'" :aria-busy="state === 'loading'">
    <span v-if="state === 'loading'" class="state-spinner" aria-hidden="true" />
    <div class="base-state__text">
      <strong v-if="title">{{ title }}</strong>
      <p>{{ message }}</p>
    </div>
    <CommonBaseButton v-if="actionLabel" variant="secondary" size="small" @click="$emit('action')">{{ actionLabel }}</CommonBaseButton>
  </div>
</template>

<style scoped>
.base-state { display: flex; align-items: center; flex-wrap: wrap; gap: var(--section-gap); padding: var(--panel-padding); border: 1px solid var(--color-border); border-radius: var(--radius-medium); background: var(--color-surface-muted); color: var(--color-text-muted); }
.base-state__text { flex: 1; min-width: min(100%, 180px); overflow-wrap: anywhere; }
.base-state__text strong { display: block; margin-bottom: 4px; color: var(--color-text); }
.base-state__text p { margin: 0; overflow-wrap: anywhere; }
.base-state--error { background: var(--color-danger-soft); color: var(--color-danger); }
.base-state--success { background: var(--color-success-soft); color: var(--color-success); }
@media (max-width: 767px) {
  .base-state { padding: var(--mobile-panel-padding); }
}
</style>
