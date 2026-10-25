<script setup lang="ts">
defineProps<{
  title: string
  eyebrow?: string
  description?: string
}>()
</script>

<template>
  <header class="base-page-header">
    <div class="base-page-header__copy">
      <p v-if="eyebrow" class="base-page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="section-title">{{ title }}</h1>
      <p v-if="description" class="section-description">{{ description }}</p>
      <slot />
    </div>
    <div v-if="$slots.actions || $slots.summary" class="base-page-header__side">
      <div v-if="$slots.actions" class="base-page-header__actions">
        <slot name="actions" />
      </div>
      <div v-if="$slots.summary" class="base-page-header__summary">
        <slot name="summary" />
      </div>
    </div>
  </header>
</template>

<style scoped>
.base-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--section-gap);
  min-width: 0;
}

.base-page-header__copy {
  flex: 1 1 280px;
  min-width: 0;
}

.base-page-header__eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-accent);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.base-page-header__side {
  display: grid;
  gap: var(--space-3);
  min-width: 0;
  max-width: 100%;
  margin-left: auto;
}

.base-page-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.base-page-header__summary {
  min-width: 0;
}

@media (max-width: 767px) {
  .base-page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .base-page-header__copy { flex-basis: auto; }
  .base-page-header__side { width: 100%; margin-left: 0; }
  .base-page-header__actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .base-page-header__actions > :deep(*) { min-width: 0; }
  .base-page-header__actions > :deep(:only-child) { grid-column: 1 / -1; }
}
</style>
