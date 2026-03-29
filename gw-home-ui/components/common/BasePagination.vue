<script setup lang="ts">
const props = withDefaults(defineProps<{
  page?: number | null
  totalPages?: number | null
}>(), {
  page: 1,
  totalPages: 1
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
const isPreviousDisabled = computed(() => safePage.value <= 1)
const isNextDisabled = computed(() => safePage.value >= safeTotalPages.value)
</script>

<template>
  <div class="base-pagination">
    <CommonBaseButton variant="secondary" :disabled="isPreviousDisabled" @click="emit('previous')">
      이전
    </CommonBaseButton>
    <span>{{ safePage }} / {{ safeTotalPages }}</span>
    <CommonBaseButton variant="secondary" :disabled="isNextDisabled" @click="emit('next')">
      다음
    </CommonBaseButton>
  </div>
</template>

<style scoped>
.base-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}
</style>
