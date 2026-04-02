<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { DailyReport } from '~/types/work'

interface SelectedWorkUnitItem {
  workUnitUuid: string
  title: string
  category: string | null
}

const props = defineProps<{
  activeTask: SelectedWorkUnitItem | null
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

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value))
}

function extractSummary(report: DailyReport): string {
  const source = report.content || report.note || report.workUnits.map((workUnit) => workUnit.title).join(', ')
  return source.replace(/\s+/g, ' ').trim()
}

function renderMarkdown(rawValue: string | null | undefined): string {
  const normalizedValue = rawValue?.trim()

  if (!normalizedValue) {
    return '<p>상세 내용이 없습니다.</p>'
  }

  const parsedHtml = marked.parse(normalizedValue, { async: false })

  if (!import.meta.client) {
    return parsedHtml
  }

  return DOMPurify.sanitize(parsedHtml)
}

const previewContentHtml = computed(() => renderMarkdown(props.previewReport?.content))
const previewNoteHtml = computed(() => renderMarkdown(props.previewReport?.note))
</script>

<template>
  <section class="report-history-panel">
    <header class="report-history-panel__header">
      <div>
        <p class="report-history-panel__eyebrow">History</p>
        <h3>지난 일일보고</h3>
        <p class="report-history-panel__description">
          {{ activeTask ? `${activeTask.title} 업무의 최근 보고를 빠르게 참고할 수 있습니다.` : '업무를 선택하면 이전 일일보고를 확인할 수 있습니다.' }}
        </p>
      </div>
    </header>

    <div class="report-history-panel__body">
      <div v-if="isLoading" class="report-history-panel__skeleton-list">
        <div v-for="index in 5" :key="index" class="report-history-panel__skeleton" />
      </div>

      <template v-else-if="activeTask">
        <div v-if="reports.length" class="report-history-panel__content">
          <div class="report-history-panel__list">
            <button
              v-for="report in reports"
              :key="report.uuid"
              type="button"
              class="report-history-panel__item"
              :class="{ 'report-history-panel__item--active': selectedReportUuid === report.uuid }"
              @click="emit('select-report', report.uuid)"
            >
              <strong>{{ formatDate(report.reportDate) }}</strong>
              <span>{{ extractSummary(report) || '요약 정보 없음' }}</span>
            </button>
          </div>

          <div v-if="previewReport" class="report-history-panel__preview">
            <div class="report-history-panel__preview-header">
              <strong>{{ formatDate(previewReport.reportDate) }} 작성분</strong>
              <CommonBaseButton size="small" variant="secondary" type="button" @click="emit('use-as-draft', previewReport)">
                초안으로 불러오기
              </CommonBaseButton>
            </div>

            <p class="report-history-panel__preview-meta">
              연결 업무: {{ previewReport.workUnits.map((workUnit) => workUnit.title).join(', ') || '-' }}
            </p>

            <div class="report-history-panel__preview-sections">
              <section class="report-history-panel__preview-section">
                <strong>오늘 수행 내용</strong>
                <div class="report-history-panel__preview-body" v-html="previewContentHtml" />
              </section>

              <section class="report-history-panel__preview-section">
                <strong>특이사항</strong>
                <div class="report-history-panel__preview-body" v-html="previewNoteHtml" />
              </section>
            </div>
          </div>
        </div>

        <p v-else class="report-history-panel__empty">
          이 업무에 대한 이전 보고가 없습니다.
        </p>
      </template>

      <p v-else class="report-history-panel__empty">
        업무를 선택하면 이전 일일보고를 확인할 수 있습니다.
      </p>
    </div>
  </section>
</template>

<style scoped>
.report-history-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  width: 100%;
  min-width: 0;
  max-width: none;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-large);
  background: rgba(255, 255, 255, 0.04);
}

.report-history-panel__header {
  padding: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.1);
}

.report-history-panel__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.report-history-panel__header h3 {
  margin: 0;
}

.report-history-panel__description {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  line-height: 1.5;
}

.report-history-panel__body {
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
}

.report-history-panel__content {
  display: grid;
  grid-template-rows: minmax(0, 240px) minmax(0, 1fr);
  gap: 14px;
  min-height: 0;
}

.report-history-panel__list {
  display: grid;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.report-history-panel__item {
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

.report-history-panel__item span {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.report-history-panel__item--active {
  border-color: rgba(110, 193, 255, 0.52);
  background: rgba(110, 193, 255, 0.08);
}

.report-history-panel__preview {
  display: grid;
  gap: 12px;
  min-height: 0;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: var(--radius-medium);
  background: rgba(5, 18, 34, 0.36);
}

.report-history-panel__preview-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.report-history-panel__preview-meta {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.report-history-panel__preview-sections {
  display: grid;
  gap: 12px;
  min-height: 0;
}

.report-history-panel__preview-section {
  display: grid;
  gap: 8px;
}

.report-history-panel__preview-section > strong {
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.report-history-panel__preview-body {
  min-height: 0;
  overflow-y: auto;
  color: var(--color-text);
  line-height: 1.6;
  word-break: break-word;
}

.report-history-panel__preview-body:deep(p:first-child) {
  margin-top: 0;
}

.report-history-panel__preview-body:deep(p:last-child) {
  margin-bottom: 0;
}

.report-history-panel__preview-body:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.92em;
}

.report-history-panel__preview-body:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.report-history-panel__preview-body:deep(pre code) {
  padding: 0;
  background: transparent;
}

.report-history-panel__preview-body:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.report-history-panel__preview-body:deep(ul),
.report-history-panel__preview-body:deep(ol) {
  padding-left: 20px;
}

.report-history-panel__empty {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.report-history-panel__skeleton-list {
  display: grid;
  gap: 10px;
}

.report-history-panel__skeleton {
  height: 72px;
  border-radius: var(--radius-medium);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.04), rgba(147, 210, 255, 0.12), rgba(255, 255, 255, 0.04));
  background-size: 220% 100%;
  animation: report-history-panel-skeleton 1.3s linear infinite;
}

@keyframes report-history-panel-skeleton {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: -100% 0;
  }
}

@media (max-width: 1100px) {
  .report-history-panel {
    width: 100%;
    min-width: 0;
    max-width: none;
  }
}
</style>
