<script setup lang="ts">
import type { OpenWeeklyReport } from '../types/work.types'
import { formatDateTime } from '~/utils/date'
import { renderMarkdown } from '~/utils/markdown'

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
const renderedContent = computed(() => renderMarkdown(props.report?.content))
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

      <article class="weekly-report-open-detail-modal__content" v-html="renderedContent" />
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
  min-height: 180px;
  line-height: 1.7;
  word-break: break-word;
}

.weekly-report-open-detail-modal__content:deep(p:first-child) {
  margin-top: 0;
}

.weekly-report-open-detail-modal__content:deep(p:last-child) {
  margin-bottom: 0;
}

.weekly-report-open-detail-modal__content:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.92em;
}

.weekly-report-open-detail-modal__content:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.weekly-report-open-detail-modal__content:deep(pre code) {
  padding: 0;
  background: transparent;
}

.weekly-report-open-detail-modal__content:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.weekly-report-open-detail-modal__content:deep(ul),
.weekly-report-open-detail-modal__content:deep(ol) {
  padding-left: 20px;
}
</style>
