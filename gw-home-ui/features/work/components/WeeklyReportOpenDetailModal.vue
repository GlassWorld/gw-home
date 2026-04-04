<script setup lang="ts">
import type { OpenWeeklyReport } from '../types/work.types'
import { formatDateTime } from '~/utils/date'

const props = defineProps<{
  visible: boolean
  report: OpenWeeklyReport | null
  isLoading: boolean
  formatDate: (value: string) => string
}>()

const emit = defineEmits<{
  close: []
}>()

const title = computed(() => props.report?.title ?? '공개 주간보고 상세')
const authorLabel = computed(() => props.report ? (props.report.nickname || props.report.loginId) : '-')
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    :title="title"
    eyebrow="Weekly Report"
    width="840px"
    @close="emit('close')"
  >
    <p v-if="props.isLoading" class="message-muted">
      공개 주간보고를 불러오는 중입니다.
    </p>

    <template v-else-if="props.report">
      <div class="weekly-report-open-detail-modal__meta">
        <span>작성자 {{ authorLabel }}</span>
        <span>기간 {{ props.formatDate(props.report.weekStartDate) }} ~ {{ props.formatDate(props.report.weekEndDate) }}</span>
        <span>공개 {{ props.report.publishedAt ? formatDateTime(props.report.publishedAt) : '-' }}</span>
      </div>

      <article class="weekly-report-open-detail-modal__content">
        {{ props.report.content }}
      </article>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.weekly-report-open-detail-modal__meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--color-text-muted);
  font-size: 0.86rem;
}

.weekly-report-open-detail-modal__content {
  margin: 0;
  padding: 16px 18px;
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(147, 210, 255, 0.12);
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
