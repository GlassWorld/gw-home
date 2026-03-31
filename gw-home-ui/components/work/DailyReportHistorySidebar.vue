<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { DailyReport } from '~/types/work'

const props = defineProps<{
  reports: DailyReport[]
  selectedReportUuid: string
  previewReport: DailyReport | null
  isLoading: boolean
}>()

const emit = defineEmits<{
  'select-report': [uuid: string]
  'use-as-draft': [report: DailyReport]
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

function extractSummary(report: DailyReport): string {
  const source = report.content || report.note || report.workUnits.map((workUnit) => workUnit.title).join(', ')
  return source.replace(/\s+/g, ' ').trim()
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

const previewContentHtml = computed(() => renderMarkdown(props.previewReport?.content))
</script>

<template>
  <section class="daily-report-history-sidebar">
    <header class="daily-report-history-sidebar__header">
      <p class="daily-report-history-sidebar__eyebrow">History</p>
      <h2>이전 일일보고</h2>
      <p class="daily-report-history-sidebar__description">
        최근 작성한 보고를 확인하고 현재 작성 중인 본문 초안으로 가져올 수 있습니다.
      </p>
    </header>

    <div class="daily-report-history-sidebar__body">
      <div v-if="isLoading" class="daily-report-history-sidebar__skeleton-list">
        <div v-for="index in 5" :key="index" class="daily-report-history-sidebar__skeleton" />
      </div>

      <template v-else-if="reports.length">
        <div class="daily-report-history-sidebar__list">
          <button
            v-for="report in reports"
            :key="report.uuid"
            type="button"
            class="daily-report-history-sidebar__item"
            :class="{ 'daily-report-history-sidebar__item--active': selectedReportUuid === report.uuid }"
            @click="emit('select-report', report.uuid)"
          >
            <strong>{{ formatDate(report.reportDate) }}</strong>
            <span>{{ extractSummary(report) || '요약 정보 없음' }}</span>
          </button>
        </div>

        <section v-if="previewReport" class="daily-report-history-sidebar__preview">
          <div class="daily-report-history-sidebar__preview-header">
            <strong>{{ formatDate(previewReport.reportDate) }}</strong>
            <CommonBaseButton size="small" variant="secondary" type="button" @click="emit('use-as-draft', previewReport)">
              초안으로 불러오기
            </CommonBaseButton>
          </div>

          <p class="daily-report-history-sidebar__preview-meta">
            업무: {{ previewReport.workUnits.length ? previewReport.workUnits.map((workUnit) => workUnit.title).join(', ') : '-' }}
          </p>

          <div class="daily-report-history-sidebar__preview-body" v-html="previewContentHtml" />
        </section>
      </template>

      <p v-else class="daily-report-history-sidebar__empty">
        이전 일일보고가 없습니다.
      </p>
    </div>
  </section>
</template>

<style scoped>
.daily-report-history-sidebar {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-large);
  background: rgba(255, 255, 255, 0.04);
}

.daily-report-history-sidebar__header {
  display: grid;
  gap: 6px;
  padding: 20px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.1);
}

.daily-report-history-sidebar__eyebrow {
  margin: 0;
  color: var(--color-accent);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.daily-report-history-sidebar__header h2 {
  margin: 0;
  font-size: 1.15rem;
}

.daily-report-history-sidebar__description,
.daily-report-history-sidebar__preview-meta,
.daily-report-history-sidebar__empty {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.daily-report-history-sidebar__body {
  display: grid;
  gap: 14px;
  min-height: 0;
  padding: 16px;
  overflow: hidden;
}

.daily-report-history-sidebar__list,
.daily-report-history-sidebar__skeleton-list {
  display: grid;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
}

.daily-report-history-sidebar__item {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid rgba(147, 210, 255, 0.1);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text);
  text-align: left;
  font: inherit;
}

.daily-report-history-sidebar__item span {
  color: var(--color-text-muted);
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.daily-report-history-sidebar__item--active {
  border-color: rgba(110, 193, 255, 0.48);
  background: rgba(110, 193, 255, 0.08);
}

.daily-report-history-sidebar__preview {
  display: grid;
  gap: 12px;
  min-height: 0;
  padding: 16px;
  border-radius: var(--radius-medium);
  border: 1px solid rgba(147, 210, 255, 0.12);
  background: rgba(5, 18, 34, 0.34);
}

.daily-report-history-sidebar__preview-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.daily-report-history-sidebar__preview-body {
  min-height: 0;
  overflow-y: auto;
  line-height: 1.6;
}

.daily-report-history-sidebar__preview-body:deep(p:first-child) {
  margin-top: 0;
}

.daily-report-history-sidebar__preview-body:deep(p:last-child) {
  margin-bottom: 0;
}

.daily-report-history-sidebar__skeleton {
  min-height: 72px;
  border-radius: var(--radius-medium);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.04), rgba(147, 210, 255, 0.12), rgba(255, 255, 255, 0.04));
  background-size: 220% 100%;
  animation: history-skeleton 1.4s ease infinite;
}

@keyframes history-skeleton {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
</style>
