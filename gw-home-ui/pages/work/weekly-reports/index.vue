<script setup lang="ts">
import type {
  SaveWeeklyReportPayload,
  WeeklyReport,
  WeeklyReportDailySource,
  WeeklyReportGenerationType
} from '~/types/work'

definePageMeta({
  middleware: 'auth'
})

const { fetchWeeklyReports, fetchWeeklyReport, fetchWeeklyDailySources, createWeeklyReport, updateWeeklyReport, generateWeeklyAiDraft } = useWeeklyReportApi()
const { showToast } = useToast()

function formatDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getWeekStartDate(): string {
  const currentDate = new Date()
  const day = currentDate.getDay()
  const diff = day === 0 ? -6 : 1 - day
  const weekStartDate = new Date(currentDate)
  weekStartDate.setDate(currentDate.getDate() + diff)
  return formatDateInput(weekStartDate)
}

function getWeekEndDate(weekStartDate: string): string {
  const date = new Date(weekStartDate)
  date.setDate(date.getDate() + 6)
  return formatDateInput(date)
}

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

const generationTypeLabelMap: Record<WeeklyReportGenerationType, string> = {
  MANUAL: '수동',
  OPENAI: 'OpenAI',
  RULE_BASED: '초안'
}

const defaultWeekStartDate = getWeekStartDate()

const weeklyReports = ref<WeeklyReport[]>([])
const sourceDailyReports = ref<WeeklyReportDailySource[]>([])
const editingWeeklyReportUuid = ref('')
const isLoadingWeeklyReports = ref(false)
const isLoadingSources = ref(false)
const isSaving = ref(false)
const isGeneratingAi = ref(false)
const aiAdditionalPrompt = ref('')

const formState = reactive<SaveWeeklyReportPayload>({
  weekStartDate: defaultWeekStartDate,
  weekEndDate: getWeekEndDate(defaultWeekStartDate),
  title: '',
  content: '',
  openYn: 'N',
  generationType: 'MANUAL'
})

function resetForm() {
  const nextWeekStartDate = getWeekStartDate()
  editingWeeklyReportUuid.value = ''
  formState.weekStartDate = nextWeekStartDate
  formState.weekEndDate = getWeekEndDate(nextWeekStartDate)
  formState.title = ''
  formState.content = ''
  formState.openYn = 'N'
  formState.generationType = 'MANUAL'
  aiAdditionalPrompt.value = ''
}

async function loadWeeklyReports() {
  isLoadingWeeklyReports.value = true

  try {
    weeklyReports.value = await fetchWeeklyReports()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    weeklyReports.value = []
    showToast(fetchError.data?.message ?? '주간보고 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoadingWeeklyReports.value = false
  }
}

async function loadSourceDailyReports() {
  isLoadingSources.value = true

  try {
    sourceDailyReports.value = await fetchWeeklyDailySources(formState.weekStartDate, formState.weekEndDate)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    sourceDailyReports.value = []
    showToast(fetchError.data?.message ?? '주간 참고용 일일보고를 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoadingSources.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadWeeklyReports(), loadSourceDailyReports()])
}

async function selectWeeklyReport(weeklyReport: WeeklyReport) {
  try {
    const detail = await fetchWeeklyReport(weeklyReport.uuid)
    editingWeeklyReportUuid.value = detail.uuid
    formState.weekStartDate = detail.weekStartDate
    formState.weekEndDate = detail.weekEndDate
    formState.title = detail.title
    formState.content = detail.content
    formState.openYn = detail.openYn
    formState.generationType = detail.generationType
    await loadSourceDailyReports()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '주간보고 정보를 불러오지 못했습니다.', { variant: 'error' })
  }
}

async function handleWeekChanged() {
  formState.weekEndDate = getWeekEndDate(formState.weekStartDate)
  await loadSourceDailyReports()
}

async function handleSave() {
  if (isSaving.value) {
    return
  }

  if (!formState.title.trim() || !formState.content.trim()) {
    showToast('제목과 본문을 입력해주세요.', { variant: 'error' })
    return
  }

  isSaving.value = true

  try {
    if (editingWeeklyReportUuid.value) {
      await updateWeeklyReport(editingWeeklyReportUuid.value, formState)
      showToast('주간보고를 수정했습니다.', { variant: 'success' })
    } else {
      await createWeeklyReport(formState)
      showToast('주간보고를 저장했습니다.', { variant: 'success' })
    }

    await loadWeeklyReports()
    resetForm()
    await loadSourceDailyReports()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '주간보고 저장에 실패했습니다.', { variant: 'error' })
  } finally {
    isSaving.value = false
  }
}

async function handleGenerateAiDraft() {
  if (isGeneratingAi.value) {
    return
  }

  isGeneratingAi.value = true

  try {
    const draft = await generateWeeklyAiDraft({
      weekStartDate: formState.weekStartDate,
      weekEndDate: formState.weekEndDate,
      additionalPrompt: aiAdditionalPrompt.value.trim() || undefined
    })
    formState.title = draft.title
    formState.content = draft.content
    formState.generationType = draft.generationType
    showToast(`${draft.modelName} 기반 초안을 생성했습니다. 게시 전 꼭 검토해주세요.`, { variant: 'success' })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? 'AI 초안 생성에 실패했습니다.', { variant: 'error' })
  } finally {
    isGeneratingAi.value = false
  }
}

await reloadAll()
</script>

<template>
  <main class="page-container weekly-report-page">
    <section class="content-panel weekly-report-page__hero">
      <div class="weekly-report-page__hero-header">
        <div>
          <p class="weekly-report-page__eyebrow">Weekly Report</p>
          <h1 class="section-title">주간보고</h1>
          <p class="section-description">
            같은 주의 일일보고를 옆에서 보며 수동으로 정리하거나, 필요하면 OpenAI 초안을 먼저 받아 편집할 수 있습니다.
          </p>
        </div>

        <div class="weekly-report-page__hero-side">
          <CommonBaseButton variant="secondary" @click="resetForm">
            새 주간보고
          </CommonBaseButton>

          <div class="weekly-report-page__summary">
            <div>
              <span>저장된 보고</span>
              <strong>{{ weeklyReports.length }}</strong>
            </div>
            <div>
              <span>참고 일일보고</span>
              <strong>{{ sourceDailyReports.length }}</strong>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="weekly-report-page__layout">
      <section class="content-panel weekly-report-page__editor">
        <div class="weekly-report-page__section-header">
          <h2>편집 패널</h2>
          <span>{{ editingWeeklyReportUuid ? '수정 모드' : '작성 모드' }}</span>
        </div>

        <div class="weekly-report-page__week-row">
          <label>
            <span>주 시작일</span>
            <input v-model="formState.weekStartDate" class="input-field" type="date" @change="handleWeekChanged">
          </label>
          <label>
            <span>주 종료일</span>
            <input v-model="formState.weekEndDate" class="input-field" type="date" disabled>
          </label>
        </div>

        <label>
          <span>제목</span>
          <input v-model="formState.title" class="input-field" type="text" maxlength="200">
        </label>

        <label>
          <span>AI 초안 보조 메모</span>
          <textarea v-model="aiAdditionalPrompt" class="weekly-report-page__textarea" rows="3" placeholder="강조할 포인트나 표현 톤이 있으면 입력하세요." />
        </label>

        <div class="weekly-report-page__editor-actions">
          <label class="weekly-report-page__toggle">
            <input v-model="formState.openYn" type="checkbox" true-value="Y" false-value="N">
            <span>오픈 메뉴에 게시</span>
          </label>

          <div class="weekly-report-page__button-row">
            <CommonBaseButton variant="secondary" :disabled="isGeneratingAi" @click="handleGenerateAiDraft">
              AI 초안 생성
            </CommonBaseButton>
            <CommonBaseButton variant="secondary" @click="resetForm">
              초기화
            </CommonBaseButton>
            <CommonBaseButton :disabled="isSaving" @click="handleSave">
              {{ editingWeeklyReportUuid ? '수정 저장' : '저장' }}
            </CommonBaseButton>
          </div>
        </div>

        <div class="weekly-report-page__generation-badge">
          생성 방식: {{ generationTypeLabelMap[formState.generationType] }}
        </div>

        <label>
          <span>본문</span>
          <textarea v-model="formState.content" class="weekly-report-page__textarea weekly-report-page__textarea--large" rows="18" />
        </label>
      </section>

      <section class="content-panel weekly-report-page__source">
        <div class="weekly-report-page__section-header">
          <h2>해당 주 일일보고</h2>
          <span v-if="isLoadingSources">불러오는 중...</span>
        </div>

        <div v-if="sourceDailyReports.length" class="weekly-report-page__source-list">
          <article v-for="dailyReport in sourceDailyReports" :key="dailyReport.uuid" class="weekly-report-page__source-card">
            <header class="weekly-report-page__source-header">
              <strong>{{ formatDate(dailyReport.reportDate) }}</strong>
            </header>
            <p class="weekly-report-page__source-content">
              {{ dailyReport.workUnits.length ? dailyReport.workUnits.map((workUnit) => workUnit.title).join(', ') : '선택된 업무가 없습니다.' }}
            </p>
            <p v-if="dailyReport.note" class="weekly-report-page__source-note">
              특이사항: {{ dailyReport.note }}
            </p>
          </article>
        </div>
        <p v-else class="weekly-report-page__empty-text">
          선택한 주차의 일일보고가 없습니다.
        </p>
      </section>
    </div>

    <section class="content-panel weekly-report-page__saved">
      <div class="weekly-report-page__section-header">
        <h2>내 주간보고</h2>
        <span v-if="isLoadingWeeklyReports">불러오는 중...</span>
      </div>

      <div v-if="weeklyReports.length" class="weekly-report-page__saved-list">
        <article v-for="weeklyReport in weeklyReports" :key="weeklyReport.uuid" class="weekly-report-page__saved-card">
          <div>
            <strong>{{ weeklyReport.title }}</strong>
            <p>{{ formatDate(weeklyReport.weekStartDate) }} ~ {{ formatDate(weeklyReport.weekEndDate) }}</p>
            <p>공개 여부: {{ weeklyReport.openYn === 'Y' ? '오픈' : '비공개' }}</p>
          </div>
          <div class="weekly-report-page__saved-actions">
            <span>{{ generationTypeLabelMap[weeklyReport.generationType] }}</span>
            <CommonBaseButton variant="secondary" size="small" @click="selectWeeklyReport(weeklyReport)">
              불러오기
            </CommonBaseButton>
          </div>
        </article>
      </div>
      <p v-else class="weekly-report-page__empty-text">
        저장된 주간보고가 없습니다.
      </p>
    </section>
  </main>
</template>

<style scoped>
.weekly-report-page {
  display: grid;
  gap: 24px;
}

.weekly-report-page__hero,
.weekly-report-page__editor,
.weekly-report-page__source,
.weekly-report-page__saved {
  padding: 22px;
}

.weekly-report-page__hero {
  display: grid;
  gap: 16px;
}

.weekly-report-page__hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.weekly-report-page__hero-side {
  display: grid;
  justify-items: end;
  gap: 14px;
}

.weekly-report-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.weekly-report-page__layout {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 24px;
  align-items: start;
}

.weekly-report-page__editor,
.weekly-report-page__source,
.weekly-report-page__saved {
  display: grid;
  gap: 18px;
}

.weekly-report-page__summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(108px, 1fr));
  gap: 10px;
  min-width: 240px;
}

.weekly-report-page__summary div {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.weekly-report-page__summary span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.weekly-report-page__summary strong {
  font-size: 1.2rem;
}

.weekly-report-page__section-header,
.weekly-report-page__editor-actions,
.weekly-report-page__button-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.weekly-report-page__week-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.weekly-report-page__week-row label,
.weekly-report-page__editor label {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.weekly-report-page__week-row label span,
.weekly-report-page__editor label span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.weekly-report-page__toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.weekly-report-page__generation-badge,
.weekly-report-page__status-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
}

.weekly-report-page__textarea {
  min-height: 96px;
  padding: 14px 16px;
  border-radius: var(--radius-small);
  border: 1px solid rgba(143, 208, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text);
  resize: vertical;
  font: inherit;
}

.weekly-report-page__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.22);
  border-color: var(--color-primary);
}

.weekly-report-page__textarea--large {
  min-height: 380px;
}

.weekly-report-page__source-list,
.weekly-report-page__saved-list {
  display: grid;
  gap: 14px;
}

.weekly-report-page__source-card,
.weekly-report-page__saved-card {
  display: grid;
  gap: 10px;
  padding: 16px 18px;
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(147, 210, 255, 0.16);
}

.weekly-report-page__source-header,
.weekly-report-page__saved-actions {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
}

.weekly-report-page__saved-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.weekly-report-page__source-content,
.weekly-report-page__source-note,
.weekly-report-page__saved-card p,
.weekly-report-page__empty-text {
  margin: 0;
  color: var(--color-text-muted);
  white-space: pre-wrap;
}

@media (max-width: 1080px) {
  .weekly-report-page__layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .weekly-report-page__hero-header,
  .weekly-report-page__saved-card,
  .weekly-report-page__section-header,
  .weekly-report-page__editor-actions,
  .weekly-report-page__button-row {
    flex-direction: column;
    align-items: stretch;
  }

  .weekly-report-page__hero-side {
    justify-items: stretch;
  }

  .weekly-report-page__summary,
  .weekly-report-page__week-row {
    grid-template-columns: 1fr;
    min-width: 0;
  }
}
</style>
