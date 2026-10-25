<script setup lang="ts">
const props = withDefaults(defineProps<{
  label: string
  selectedLabel: string
  options: readonly string[]
  activeValue: string
  open: boolean
  disabled?: boolean
  menuDirection?: 'top' | 'bottom'
  allLabel?: string
  optionLabel?: (value: string) => string
}>(), {
  disabled: false,
  menuDirection: 'bottom',
  allLabel: '',
  optionLabel: (value: string) => value,
})

const emit = defineEmits<{
  toggle: []
  select: [value: string]
}>()
</script>

<template>
  <label class="admin-account-select-field">
    <span>{{ props.label }}</span>
    <div class="admin-account-select-field__dropdown" data-dropdown-root>
      <button
        type="button"
        class="admin-account-select-field__trigger"
        :class="{ 'admin-account-select-field__trigger--open': props.open }"
        :disabled="props.disabled"
        @click="emit('toggle')"
      >
        <span>{{ props.selectedLabel }}</span>
      </button>
      <div
        v-if="props.open"
        class="admin-account-select-field__menu"
        :class="{ 'admin-account-select-field__menu--top': props.menuDirection === 'top' }"
      >
        <button
          v-if="props.allLabel"
          type="button"
          class="admin-account-select-field__option"
          :class="{ 'admin-account-select-field__option--active': props.activeValue === '' }"
          @click="emit('select', '')"
        >
          {{ props.allLabel }}
        </button>
        <button
          v-for="option in props.options"
          :key="option"
          type="button"
          class="admin-account-select-field__option"
          :class="{ 'admin-account-select-field__option--active': props.activeValue === option }"
          @click="emit('select', option)"
        >
          {{ props.optionLabel(option) }}
        </button>
      </div>
    </div>
  </label>
</template>

<style scoped>
.admin-account-select-field {
  display: grid;
  gap: 8px;
}

.admin-account-select-field__dropdown {
  position: relative;
}

.admin-account-select-field__trigger {
  width: 100%;
  min-height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-small);
  background: var(--color-surface);
  color: var(--color-text);
  text-align: left;
}

.admin-account-select-field__trigger::after {
  content: '';
  width: 10px;
  height: 10px;
  border-right: 2px solid var(--color-border-strong);
  border-bottom: 2px solid var(--color-border-strong);
  transform: translateY(-2px) rotate(45deg);
  flex-shrink: 0;
  transition: transform 0.18s ease;
}

.admin-account-select-field__trigger--open::after {
  transform: translateY(2px) rotate(-135deg);
}

.admin-account-select-field__trigger:disabled {
  cursor: wait;
  opacity: 0.7;
}

.admin-account-select-field__menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 20;
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface);
  box-shadow:
    var(--shadow-soft);
}

.admin-account-select-field__menu--top {
  top: auto;
  bottom: calc(100% + 8px);
}

.admin-account-select-field__option {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-small);
  background: transparent;
  color: var(--color-text);
  text-align: left;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.admin-account-select-field__option:hover,
.admin-account-select-field__option--active {
  border-color: var(--color-border);
  background: var(--color-primary-soft);
  transform: translateY(-1px);
}
</style>
