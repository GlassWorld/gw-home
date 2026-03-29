<script setup lang="ts">
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

defineProps<{
  reportDate: string
  selectedWorkUnits: SelectedWorkUnitItem[]
  workSummary: string
  issueNote: string
  suggestions: string[]
  isEditing: boolean
}>()

const emit = defineEmits<{
  'update:report-date': [value: string]
  'update:work-summary': [value: string]
  'update:issue-note': [value: string]
  'insert-suggestion': [value: string]
}>()
</script>

<template>
  <section class="report-editor-panel">
    <header class="report-editor-panel__header">
      <div>
        <p class="report-editor-panel__eyebrow">Editor</p>
        <h3>일일보고 작성</h3>
      </div>
    </header>

    <div class="report-editor-panel__body">
      <label class="report-editor-panel__field">
        <span>작성일자</span>
        <div class="report-editor-panel__date-field">
          <input
            :value="reportDate"
            class="input-field"
            type="date"
            :disabled="isEditing"
            @input="emit('update:report-date', ($event.target as HTMLInputElement).value)"
          >
          <span v-if="reportDate" class="report-editor-panel__date-label">
            {{ formatReportDateLabel(reportDate) }}
          </span>
        </div>
      </label>

      <div class="report-editor-panel__field">
        <span>선택된 업무</span>
        <div class="report-editor-panel__chips">
          <span
            v-for="workUnit in selectedWorkUnits"
            :key="workUnit.workUnitUuid"
            class="report-editor-panel__chip"
          >
            {{ workUnit.title }}
          </span>
          <p v-if="!selectedWorkUnits.length" class="report-editor-panel__empty">
            아직 선택된 업무가 없습니다.
          </p>
        </div>
      </div>

      <div v-if="suggestions.length" class="report-editor-panel__field">
        <span>이전 보고 기반 추천</span>
        <div class="report-editor-panel__suggestions">
          <button
            v-for="suggestion in suggestions"
            :key="suggestion"
            type="button"
            class="report-editor-panel__suggestion"
            @click="emit('insert-suggestion', suggestion)"
          >
            {{ suggestion }}
          </button>
        </div>
      </div>

      <label class="report-editor-panel__field">
        <span>오늘 수행 내용</span>
        <textarea
          :value="workSummary"
          class="report-editor-panel__textarea report-editor-panel__textarea--large"
          rows="8"
          placeholder="예: API 연동 완료, 예외 케이스 점검, 저장/조회 흐름 확인"
          @input="emit('update:work-summary', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>

      <label class="report-editor-panel__field">
        <span>이슈 / 특이사항</span>
        <textarea
          :value="issueNote"
          class="report-editor-panel__textarea"
          rows="4"
          placeholder="예: 인증 예외 재현 중, 디자인 QA 필요"
          @input="emit('update:issue-note', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>

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
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.1);
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

.report-editor-panel__body {
  display: grid;
  gap: 16px;
  min-height: 0;
  overflow-y: auto;
  padding: 18px;
}

.report-editor-panel__field {
  display: grid;
  gap: 8px;
}

.report-editor-panel__field > span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.report-editor-panel__date-field {
  display: grid;
  gap: 8px;
}

.report-editor-panel__date-label {
  color: var(--color-text-muted);
  font-size: 0.84rem;
  line-height: 1.5;
}

.report-editor-panel__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.report-editor-panel__chip {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.84rem;
  font-weight: 700;
}

.report-editor-panel__empty {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.report-editor-panel__suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.report-editor-panel__suggestion {
  padding: 8px 12px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--color-text-muted);
  font: inherit;
  font-size: 0.84rem;
}

.report-editor-panel__textarea {
  min-height: 112px;
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
  min-height: 180px;
}
</style>
