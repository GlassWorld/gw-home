<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

const props = withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'danger'
  size?: 'small' | 'medium'
  type?: 'button' | 'submit' | 'reset'
  disabled?: boolean
  block?: boolean
  to?: RouteLocationRaw
  href?: string
  target?: string
  rel?: string
}>(), {
  variant: 'primary',
  size: 'medium',
  type: 'button',
  disabled: false,
  block: false,
  to: undefined,
  href: undefined,
  target: undefined,
  rel: undefined
})

const componentType = computed(() => {
  if (props.to) {
    return resolveComponent('NuxtLink')
  }

  if (props.href) {
    return 'a'
  }

  return 'button'
})

const componentProps = computed(() => {
  if (props.to) {
    return {
      to: props.to
    }
  }

  if (props.href) {
    return {
      href: props.href,
      target: props.target,
      rel: props.rel
    }
  }

  return {
    type: props.type,
    disabled: props.disabled
  }
})
</script>

<template>
  <component
    :is="componentType"
    v-bind="componentProps"
    :class="[
      'base-button',
      `base-button--${props.variant}`,
      `base-button--${props.size}`,
      { 'base-button--block': props.block, 'base-button--disabled': props.disabled }
    ]"
  >
    <slot />
  </component>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 999px;
  font: inherit;
  font-weight: 600;
  white-space: nowrap;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease, border-color 0.18s ease;
  max-height: 34px;
}

.base-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.base-button:focus-visible {
  outline: 2px solid rgba(110, 193, 255, 0.24);
  outline-offset: 2px;
}

.base-button:disabled,
.base-button--disabled {
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
}

.base-button--medium {
  min-height: 40px;
  font-size: 0.94rem;
}

.base-button--small {
  min-height: 34px;
  padding: 0 13px;
  font-size: 0.86rem;
}

.base-button--block {
  width: 100%;
}

.base-button--primary {
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-strong) 100%);
  box-shadow: 0 14px 26px rgba(29, 124, 184, 0.24);
}

.base-button--secondary {
  color: var(--color-text);
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(147, 210, 255, 0.2);
}

.base-button--danger {
  color: #fff;
  background: var(--color-danger);
  box-shadow: 0 14px 24px rgba(224, 92, 92, 0.18);
}
</style>
