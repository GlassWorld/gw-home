<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

const props = withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'danger'
  size?: 'small' | 'medium'
  type?: 'button' | 'submit' | 'reset'
  disabled?: boolean
  loading?: boolean
  block?: boolean
  to?: RouteLocationRaw
  href?: string
  target?: string
  rel?: string
}>(), {
  variant: 'primary', size: 'medium', type: 'button',
  disabled: false, loading: false, block: false
})
const emit = defineEmits<{ click: [event: MouseEvent] }>()
const isDisabled = computed(() => props.disabled || props.loading)
const componentType = computed(() => {
  if ((props.to || props.href) && isDisabled.value) return 'a'
  if (props.to) return resolveComponent('NuxtLink')
  return props.href ? 'a' : 'button'
})
const componentProps = computed(() => {
  if ((props.to || props.href) && isDisabled.value) return { tabindex: -1, role: 'link' }
  if (props.to) return { to: props.to }
  if (props.href) return { href: props.href, target: props.target, rel: props.rel ?? (props.target === '_blank' ? 'noopener noreferrer' : undefined) }
  return { type: props.type, disabled: isDisabled.value }
})

// 비활성 링크와 저장 중 버튼의 중복 동작을 막는다.
function handleClick(event: MouseEvent) {
  if (isDisabled.value) {
    event.preventDefault()
    event.stopPropagation()
    return
  }
  emit('click', event)
}
</script>

<template>
  <component
    :is="componentType"
    v-bind="componentProps"
    :class="['base-button', `base-button--${variant}`, `base-button--${size}`, { 'base-button--block': block, 'base-button--disabled': isDisabled }]"
    :aria-disabled="isDisabled || undefined"
    :aria-busy="loading || undefined"
    @click="handleClick"
  >
    <span v-if="loading" class="base-button__spinner" aria-hidden="true" />
    <slot />
  </component>
</template>
