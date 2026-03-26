<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: string
  disabled?: boolean
}>(), {
  disabled: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  complete: [value: string]
}>()

function normalizeOtpCode(value: string): string {
  return value.replace(/\D/g, '').slice(0, 6)
}

function updateValue(value: string) {
  const normalizedValue = normalizeOtpCode(value)
  emit('update:modelValue', normalizedValue)

  if (normalizedValue.length === 6) {
    emit('complete', normalizedValue)
  }
}

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  updateValue(target.value)
}

function handlePaste(event: ClipboardEvent) {
  event.preventDefault()
  updateValue(event.clipboardData?.getData('text') ?? '')
}
</script>

<template>
  <input
    :value="props.modelValue"
    class="input-field otp-code-input"
    type="text"
    inputmode="numeric"
    autocomplete="one-time-code"
    maxlength="6"
    placeholder="000000"
    :disabled="props.disabled"
    @input="handleInput"
    @paste="handlePaste"
  >
</template>

<style scoped>
.otp-code-input {
  min-height: 56px;
  text-align: center;
  letter-spacing: 0.42em;
  font-size: 1.45rem;
  font-weight: 700;
}

.otp-code-input::placeholder {
  letter-spacing: 0.32em;
}
</style>
