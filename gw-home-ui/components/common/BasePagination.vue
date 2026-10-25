<script setup lang="ts">
const props = withDefaults(defineProps<{
  page?: number | null
  totalPages?: number | null
  disabled?: boolean
}>(), {
  page: 1,
  totalPages: 1,
  disabled: false
})

const emit = defineEmits<{
  previous: []
  next: []
}>()

function normalizePageValue(value: number | null | undefined, fallback: number): number {
  if (typeof value !== 'number' || Number.isNaN(value) || !Number.isFinite(value)) {
    return fallback
  }

  return Math.max(Math.trunc(value), fallback)
}

const safeTotalPages = computed(() => normalizePageValue(props.totalPages, 1))
const safePage = computed(() => Math.min(normalizePageValue(props.page, 1), safeTotalPages.value))
const isPreviousDisabled = computed(() => props.disabled || safePage.value <= 1)
const isNextDisabled = computed(() => props.disabled || safePage.value >= safeTotalPages.value)
</script>

<template>
  <nav class="base-pagination" aria-label="페이지 이동">
    <CommonBaseButton variant="secondary" :disabled="isPreviousDisabled" @click="emit('previous')">
      이전
    </CommonBaseButton>
    <span role="status" aria-live="polite">{{ safePage }} / {{ safeTotalPages }}</span>
    <CommonBaseButton variant="secondary" :disabled="isNextDisabled" @click="emit('next')">
      다음
    </CommonBaseButton>
  </nav>
</template>

<style scoped>
.base-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}
</style>
