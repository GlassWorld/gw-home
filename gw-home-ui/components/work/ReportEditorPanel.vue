<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'

interface SelectedWorkUnitItem {
  workUnitUuid: string
  title: string
  category: string | null
}

function formatReportDateLabel(value: string): string {
  if (!value) {
    return ''
  }

  const [year = 1970, month = 1, day = 1] = value.split('-').map((item) => Number(item))
  const date = new Date(year, month - 1, day)

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  }).format(date)
}

const props = defineProps<{
  reportDate: string
  selectedWorkUnits: SelectedWorkUnitItem[]
  workSummary: string
  issueNote: string
  isEditing: boolean
}>()

const emit = defineEmits<{
  'update:report-date': [value: string]
  'update:work-summary': [value: string]
  'update:issue-note': [value: string]
}>()

function renderMarkdown(rawValue: string): string {
  const normalizedValue = rawValue.trim()

  if (!normalizedValue) {
    return '<p>아직 작성된 내용이 없습니다.</p>'
  }

  const parsedHtml = marked.parse(normalizedValue, { async: false })

  if (!import.meta.client) {
    return parsedHtml
  }

  return DOMPurify.sanitize(parsedHtml)
}

const renderedWorkSummary = computed(() => renderMarkdown(props.workSummary))
const renderedIssueNote = computed(() => renderMarkdown(props.issueNote))
const selectedWorkUnitSummary = computed(() => {
  if (!props.selectedWorkUnits.length) {
    return '선택된 업무 없음'
  }

  return props.selectedWorkUnits.map((workUnit) => workUnit.title).join(', ')
})
</script>

<template>
  <section class="report-editor-panel">
    <header class="report-editor-panel__header">
      <div class="report-editor-panel__header-copy">
        <p class="report-editor-panel__eyebrow">Editor</p>
        <h3>일일보고 작성</h3>
      </div>

      <label class="report-editor-panel__header-date">
        <span>작성일자</span>
        <div class="report-editor-panel__header-date-field">
          <input
            :value="reportDate"
            class="input-field report-editor-panel__header-date-input"
            type="date"
            :disabled="isEditing"
            @input="emit('update:report-date', ($event.target as HTMLInputElement).value)"
          >
        </div>
      </label>
    </header>

    <div class="report-editor-panel__body">
      <div class="report-editor-panel__meta-grid">
        <div class="report-editor-panel__inline-field">
          <span>선택된 업무</span>
          <p class="report-editor-panel__inline-value" :title="selectedWorkUnitSummary">
            {{ selectedWorkUnitSummary }}
          </p>
        </div>

      </div>

      <div class="report-editor-panel__workspace">
        <div class="report-editor-panel__workspace-column">
          <div class="report-editor-panel__workspace-header">
            <strong>작성</strong>
            <span>Markdown</span>
          </div>

          <label class="report-editor-panel__field">
            <span>오늘 수행 내용</span>
            <textarea
              :value="workSummary"
              class="report-editor-panel__textarea report-editor-panel__textarea--large"
              rows="8"
              placeholder="예: ## 완료한 작업, - 점검 항목, `코드`처럼 마크다운으로 작성할 수 있습니다."
              @input="emit('update:work-summary', ($event.target as HTMLTextAreaElement).value)"
            />
          </label>

          <label class="report-editor-panel__field">
            <span>이슈 / 특이사항</span>
            <textarea
              :value="issueNote"
              class="report-editor-panel__textarea"
              rows="4"
              placeholder="예: 재현 조건, 블로커, 공유가 필요한 특이사항을 마크다운으로 정리하세요."
              @input="emit('update:issue-note', ($event.target as HTMLTextAreaElement).value)"
            />
          </label>
        </div>

        <div class="report-editor-panel__workspace-column">
          <div class="report-editor-panel__workspace-header">
            <strong>미리보기</strong>
            <span>Live Preview</span>
          </div>

          <div class="report-editor-panel__preview-grid">
            <section class="report-editor-panel__preview-card">
              <div class="report-editor-panel__preview-header">
                <strong>오늘 수행 내용</strong>
                <span>Markdown Preview</span>
              </div>
              <div class="report-editor-panel__preview-body" v-html="renderedWorkSummary" />
            </section>

            <section class="report-editor-panel__preview-card">
              <div class="report-editor-panel__preview-header">
                <strong>특이사항</strong>
                <span>Markdown Preview</span>
              </div>
              <div class="report-editor-panel__preview-body" v-html="renderedIssueNote" />
            </section>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.report-editor-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-large);
  background: rgba(255, 255, 255, 0.04);
}

.report-editor-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.1);
}

.report-editor-panel__header-copy {
  min-width: 0;
}

.report-editor-panel__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.report-editor-panel__header h3 {
  margin: 0;
}

.report-editor-panel__header-date {
  display: grid;
  gap: 6px;
  min-width: 220px;
}

.report-editor-panel__header-date > span {
  color: var(--color-text-muted);
  font-size: 0.84rem;
  font-weight: 600;
  text-align: right;
}

.report-editor-panel__header-date-field {
  display: grid;
  gap: 6px;
  justify-items: end;
}

.report-editor-panel__header-date-input {
  width: 100%;
  min-width: 0;
}

.report-editor-panel__body {
  display: grid;
  gap: 16px;
  min-height: 0;
  overflow-y: auto;
  padding: 18px;
  align-content: start;
  align-items: start;
}

.report-editor-panel__meta-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  align-self: start;
  align-items: start;
  align-content: start;
}

.report-editor-panel__field {
  display: grid;
  gap: 8px;
}

.report-editor-panel__field--full {
  grid-column: 1 / -1;
}

.report-editor-panel__inline-field {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  align-items: center;
  align-content: center;
  gap: 10px;
  min-height: 0;
  height: auto;
  align-self: start;
}

.report-editor-panel__inline-field > span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.report-editor-panel__inline-value {
  margin: 0;
  min-width: 0;
  color: var(--color-text);
  line-height: 1.4;
  min-height: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.report-editor-panel__field > span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.report-editor-panel__empty {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.report-editor-panel__textarea {
  min-height: 128px;
  padding: 14px 16px;
  border-radius: var(--radius-small);
  border: 1px solid rgba(143, 208, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text);
  resize: vertical;
  font: inherit;
}

.report-editor-panel__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.22);
  border-color: var(--color-primary);
}

.report-editor-panel__textarea--large {
  min-height: 260px;
}

.report-editor-panel__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  min-height: 0;
}

.report-editor-panel__workspace-column {
  display: grid;
  align-content: start;
  gap: 14px;
  min-height: 0;
}

.report-editor-panel__workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-medium);
  border: 1px solid rgba(147, 210, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
}

.report-editor-panel__workspace-header span {
  color: var(--color-text-muted);
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.report-editor-panel__preview-grid {
  display: grid;
  gap: 12px;
  align-content: start;
}

.report-editor-panel__preview-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: var(--radius-medium);
  background: rgba(5, 18, 34, 0.36);
}

.report-editor-panel__preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-editor-panel__preview-header span {
  color: var(--color-text-muted);
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.report-editor-panel__preview-body {
  color: var(--color-text);
  line-height: 1.6;
  word-break: break-word;
}

.report-editor-panel__preview-body:deep(p:first-child) {
  margin-top: 0;
}

.report-editor-panel__preview-body:deep(p:last-child) {
  margin-bottom: 0;
}

.report-editor-panel__preview-body:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.92em;
}

.report-editor-panel__preview-body:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.report-editor-panel__preview-body:deep(pre code) {
  padding: 0;
  background: transparent;
}

.report-editor-panel__preview-body:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.report-editor-panel__preview-body:deep(ul),
.report-editor-panel__preview-body:deep(ol) {
  padding-left: 20px;
}

@media (max-width: 1200px) {
  .report-editor-panel__workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .report-editor-panel__header {
    flex-direction: column;
  }

  .report-editor-panel__header-date {
    width: 100%;
    min-width: 0;
  }

  .report-editor-panel__header-date > span,
  .report-editor-panel__header-date-field {
    text-align: left;
    justify-items: stretch;
  }

  .report-editor-panel__meta-grid {
    grid-template-columns: 1fr;
  }

  .report-editor-panel__inline-field {
    grid-template-columns: 1fr;
    align-items: start;
    gap: 6px;
  }

  .report-editor-panel__field--full {
    grid-column: auto;
  }
}
</style>
