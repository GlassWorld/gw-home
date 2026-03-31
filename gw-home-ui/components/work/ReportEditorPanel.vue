<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'

interface SelectedWorkUnitItem {
  workUnitUuid: string
  title: string
  category: string | null
}

const props = defineProps<{
  reportDate: string
  selectedWorkUnits: SelectedWorkUnitItem[]
  workSummary: string
  issueNote: string
  isEditing: boolean
  selectedCommitSummary?: string
}>()

const emit = defineEmits<{
  'update:report-date': [value: string]
  'update:work-summary': [value: string]
  'update:issue-note': [value: string]
  'open-work-import': []
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
        <p class="report-editor-panel__header-description">
          업무불러오기 모달에서 업무와 커밋을 선택하면 본문 초안을 빠르게 세팅할 수 있습니다.
        </p>
      </div>

      <div class="report-editor-panel__header-actions">
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

        <CommonBaseButton type="button" @click="emit('open-work-import')">
          업무불러오기
        </CommonBaseButton>
      </div>
    </header>

    <div class="report-editor-panel__body">
      <div class="report-editor-panel__meta-grid">
        <div class="report-editor-panel__inline-field">
          <span>선택된 업무</span>
          <p class="report-editor-panel__inline-value" :title="selectedWorkUnitSummary">
            {{ selectedWorkUnitSummary }}
          </p>
        </div>

        <div class="report-editor-panel__inline-field">
          <span>불러온 커밋</span>
          <p class="report-editor-panel__inline-value">
            {{ selectedCommitSummary || '선택된 커밋 없음' }}
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

.report-editor-panel__header-description {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.5;
}

.report-editor-panel__header-actions {
  display: flex;
  gap: 12px;
  align-items: flex-start;
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
}

.report-editor-panel__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.report-editor-panel__inline-field {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.report-editor-panel__inline-field > span,
.report-editor-panel__field > span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.report-editor-panel__inline-value {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.report-editor-panel__field {
  display: grid;
  gap: 8px;
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

.report-editor-panel__workspace-column,
.report-editor-panel__preview-grid {
  display: grid;
  gap: 14px;
  min-height: 0;
}

.report-editor-panel__workspace-header,
.report-editor-panel__preview-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.report-editor-panel__preview-card {
  display: grid;
  gap: 10px;
  min-height: 0;
  padding: 16px;
  border-radius: var(--radius-medium);
  border: 1px solid rgba(147, 210, 255, 0.14);
  background: rgba(5, 18, 34, 0.34);
}

.report-editor-panel__preview-body {
  min-height: 120px;
  line-height: 1.6;
}

.report-editor-panel__preview-body:deep(p:first-child) {
  margin-top: 0;
}

.report-editor-panel__preview-body:deep(p:last-child) {
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .report-editor-panel__header,
  .report-editor-panel__header-actions,
  .report-editor-panel__meta-grid,
  .report-editor-panel__workspace {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
