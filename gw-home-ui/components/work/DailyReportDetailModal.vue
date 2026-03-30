<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { DailyReport } from '~/types/work'

const props = defineProps<{
  visible: boolean
  report: DailyReport | null
}>()

const emit = defineEmits<{
  close: []
  edit: [report: DailyReport]
}>()

function formatDate(value: string): string {
  if (!value) {
    return '-'
  }

  const [year = 1970, month = 1, day = 1] = value.split('-').map((item) => Number(item))
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  }).format(new Date(year, month - 1, day))
}

function formatDateTime(value: string): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function renderMarkdown(rawValue: string | null | undefined): string {
  const normalizedValue = rawValue?.trim()

  if (!normalizedValue) {
    return '<p>내용이 없습니다.</p>'
  }

  const parsedHtml = marked.parse(normalizedValue, { async: false })

  if (!import.meta.client) {
    return parsedHtml
  }

  return DOMPurify.sanitize(parsedHtml)
}

const renderedContent = computed(() => renderMarkdown(props.report?.content))
const renderedNote = computed(() => renderMarkdown(props.report?.note))
</script>

<template>
  <CommonBaseModal
    v-if="report"
    :visible="visible"
    eyebrow="Daily Report"
    title="일일보고 상세"
    width="min(1120px, 92vw)"
    @close="emit('close')"
  >
    <section class="daily-report-detail">
      <div class="daily-report-detail__meta-grid">
        <div class="daily-report-detail__meta-card">
          <span>작성일자</span>
          <strong>{{ formatDate(report.reportDate) }}</strong>
        </div>
        <div class="daily-report-detail__meta-card">
          <span>수정일</span>
          <strong>{{ formatDateTime(report.updatedAt) }}</strong>
        </div>
      </div>

      <div class="daily-report-detail__meta-card">
        <span>업무</span>
        <p class="daily-report-detail__task-summary">
          {{ report.workUnits.length ? report.workUnits.map((workUnit) => workUnit.title).join(', ') : '선택된 업무 없음' }}
        </p>
      </div>

      <div class="daily-report-detail__content-grid">
        <section class="daily-report-detail__section">
          <div class="daily-report-detail__section-header">
            <strong>오늘 수행 내용</strong>
            <span>Markdown</span>
          </div>
          <div class="daily-report-detail__markdown" v-html="renderedContent" />
        </section>

        <section class="daily-report-detail__section">
          <div class="daily-report-detail__section-header">
            <strong>이슈 / 특이사항</strong>
            <span>Markdown</span>
          </div>
          <div class="daily-report-detail__markdown" v-html="renderedNote" />
        </section>
      </div>
    </section>

    <template #actions>
      <CommonBaseButton variant="secondary" @click="emit('close')">
        닫기
      </CommonBaseButton>
      <CommonBaseButton @click="emit('edit', report)">
        수정
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.daily-report-detail {
  display: grid;
  gap: 16px;
}

.daily-report-detail__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.daily-report-detail__meta-card,
.daily-report-detail__section {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: var(--radius-medium);
  border: 1px solid rgba(147, 210, 255, 0.14);
  background: rgba(255, 255, 255, 0.04);
}

.daily-report-detail__meta-card span,
.daily-report-detail__section-header span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
  font-weight: 600;
}

.daily-report-detail__meta-card strong {
  font-size: 1rem;
}

.daily-report-detail__task-summary {
  margin: 0;
  line-height: 1.5;
  word-break: break-word;
}

.daily-report-detail__content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 0.9fr);
  gap: 16px;
}

.daily-report-detail__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.daily-report-detail__markdown {
  min-height: 180px;
  line-height: 1.6;
  word-break: break-word;
}

.daily-report-detail__markdown:deep(p:first-child) {
  margin-top: 0;
}

.daily-report-detail__markdown:deep(p:last-child) {
  margin-bottom: 0;
}

.daily-report-detail__markdown:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.92em;
}

.daily-report-detail__markdown:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.daily-report-detail__markdown:deep(pre code) {
  padding: 0;
  background: transparent;
}

.daily-report-detail__markdown:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.daily-report-detail__markdown:deep(ul),
.daily-report-detail__markdown:deep(ol) {
  padding-left: 20px;
}

@media (max-width: 900px) {
  .daily-report-detail__meta-grid,
  .daily-report-detail__content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
