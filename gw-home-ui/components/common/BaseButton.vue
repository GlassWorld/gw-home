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
  border-radius: 8px;
  font: inherit;
  font-weight: 600;
  white-space: nowrap;
  transition: box-shadow 0.18s ease, background-color 0.18s ease, border-color 0.18s ease;
  max-height: 34px;
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
  min-height: 44px;
  font-size: 0.94rem;
}

.base-button--small {
  min-height: 34px;
  padding: 0 13px;
  font-size: 0.84rem;
  letter-spacing: 0.02em;
}

.base-button--block {
  width: 100%;
}

.base-button--primary {
  color: #fdfdff;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  border-color: rgba(165, 188, 255, 0.24);
  background: linear-gradient(135deg, rgba(109, 103, 255, 0.98) 0%, rgba(63, 151, 255, 0.96) 58%, rgba(66, 219, 223, 0.92) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    inset 0 -10px 16px rgba(35, 58, 154, 0.24),
    0 20px 34px rgba(18, 34, 106, 0.34),
    0 0 28px rgba(79, 104, 255, 0.28);
}

.base-button--primary:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(129, 122, 255, 1) 0%, rgba(82, 170, 255, 0.98) 58%, rgba(90, 235, 232, 0.94) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.26),
    inset 0 -10px 16px rgba(35, 58, 154, 0.18),
    0 24px 38px rgba(10, 24, 84, 0.42),
    0 0 30px rgba(80, 124, 255, 0.34);
}

.base-button--secondary {
  color: rgba(236, 246, 255, 0.94);
  font-weight: 600;
  border-color: rgba(176, 195, 255, 0.26);
  background: linear-gradient(180deg, rgba(42, 54, 98, 0.92) 0%, rgba(25, 35, 68, 0.9) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    inset 0 -8px 14px rgba(56, 79, 162, 0.2),
    0 14px 28px rgba(6, 20, 54, 0.28);
}

.base-button--secondary:hover:not(:disabled) {
  border-color: rgba(196, 212, 255, 0.34);
  background: linear-gradient(180deg, rgba(56, 70, 120, 0.96) 0%, rgba(34, 47, 86, 0.94) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    inset 0 -8px 14px rgba(66, 91, 180, 0.24),
    0 16px 30px rgba(8, 22, 58, 0.32);
}

.base-button--danger {
  color: #fff;
  background: linear-gradient(135deg, #ea6b72 0%, #c93c52 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    0 16px 28px rgba(122, 20, 44, 0.3);
}
</style>
