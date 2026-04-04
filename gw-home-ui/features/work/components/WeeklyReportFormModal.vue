<script setup lang="ts">
import type {
  SaveWeeklyReportPayload,
  WeeklyReport,
  WeeklyReportDailySource,
  WeeklyReportGenerationType
} from '~/types/work'

const props = withDefaults(defineProps<{
  visible: boolean
  weeklyReportUuid?: string
}>(), {
  weeklyReportUuid: ''
})

const emit = defineEmits<{
  close: []
  saved: [report: WeeklyReport]
}>()

const { fetchWeeklyReport, fetchWeeklyDailySources, createWeeklyReport, updateWeeklyReport, generateWeeklyAiDraft } = useWeeklyReportApi()
const { showToast } = useToast()
const { confirm } = useDialog()

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

function toLocalDate(value: string): Date {
  const [year = 1970, month = 1, day = 1] = value.split('-').map((item) => Number(item))
  return new Date(year, month - 1, day)
}

function formatDate(value: string): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(toLocalDate(value))
}

function formatWeekday(value: string): string {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    weekday: 'short'
  }).format(toLocalDate(value))
}

function formatDateWithWeekday(value: string): string {
  if (!value) {
    return '-'
  }

  return `${formatDate(value)} (${formatWeekday(value)})`
}

function buildWeekdayDates(weekStartDate: string): string[] {
  if (!weekStartDate) {
    return []
  }

  return Array.from({ length: 5 }, (_, index) => {
    const date = toLocalDate(weekStartDate)
    date.setDate(date.getDate() + index)
    return formatDateInput(date)
  })
}

const generationTypeLabelMap: Record<WeeklyReportGenerationType, string> = {
  MANUAL: '수동',
  OPENAI: 'OpenAI',
  RULE_BASED: '초안'
}

const isEditing = computed(() => Boolean(props.weeklyReportUuid))
const defaultWeekStartDate = getWeekStartDate()

const isInitializing = ref(false)
const isLoadingSources = ref(false)
const isSaving = ref(false)
const isGeneratingAi = ref(false)
const sourceDailyReports = ref<WeeklyReportDailySource[]>([])
const selectedDailySourceDate = ref('')
const contentTextarea = ref<HTMLTextAreaElement | null>(null)

const formState = reactive<SaveWeeklyReportPayload>({
  weekStartDate: defaultWeekStartDate,
  weekEndDate: getWeekEndDate(defaultWeekStartDate),
  title: '',
  content: '',
  openYn: 'N',
  generationType: 'MANUAL'
})

const weekdaySources = computed(() => {
  const sourceMap = new Map(
    sourceDailyReports.value.map((report) => [formatDateInput(toLocalDate(report.reportDate)), report])
  )

  return buildWeekdayDates(formState.weekStartDate).map((reportDate) => ({
    reportDate,
    source: sourceMap.get(reportDate) ?? null
  }))
})

const selectedSourceItem = computed(() =>
  weekdaySources.value.find((item) => item.reportDate === selectedDailySourceDate.value) ?? null
)

function resizeTextarea(element: HTMLTextAreaElement | null) {
  if (!element) {
    return
  }

  element.style.height = 'auto'
  element.style.height = `${element.scrollHeight}px`
}

function handleContentInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  nextTick(() => resizeTextarea(target))
}

function setScrollLock(locked: boolean) {
  if (!import.meta.client) {
    return
  }

  document.body.style.overflow = locked ? 'hidden' : ''
}

function resetFormState() {
  const nextWeekStartDate = getWeekStartDate()
  formState.weekStartDate = nextWeekStartDate
  formState.weekEndDate = getWeekEndDate(nextWeekStartDate)
  formState.title = ''
  formState.content = ''
  formState.openYn = 'N'
  formState.generationType = 'MANUAL'
  sourceDailyReports.value = []
  selectedDailySourceDate.value = ''
}

async function loadDailySources() {
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

async function loadSelectedDailyReportDetail(reportDate: string) {
  if (selectedDailySourceDate.value === reportDate) {
    selectedDailySourceDate.value = ''
    return
  }

  selectedDailySourceDate.value = reportDate
}

async function syncSourcePanel() {
  await loadDailySources()

  const nextSelectedDate = weekdaySources.value.some((item) => item.reportDate === selectedDailySourceDate.value)
    ? selectedDailySourceDate.value
    : weekdaySources.value[0]?.reportDate ?? ''

  if (!nextSelectedDate) {
    selectedDailySourceDate.value = ''
    return
  }

  await loadSelectedDailyReportDetail(nextSelectedDate)
}

async function initializeModal() {
  if (!props.visible) {
    return
  }

  isInitializing.value = true

  try {
    resetFormState()

    if (props.weeklyReportUuid) {
      const detail = await fetchWeeklyReport(props.weeklyReportUuid)
      formState.weekStartDate = detail.weekStartDate
      formState.weekEndDate = detail.weekEndDate
      formState.title = detail.title
      formState.content = detail.content
      formState.openYn = detail.openYn
      formState.generationType = detail.generationType
    }

    await syncSourcePanel()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '주간보고 정보를 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isInitializing.value = false
    await nextTick()
    resizeTextarea(contentTextarea.value)
  }
}

async function handleWeekChanged() {
  formState.weekEndDate = getWeekEndDate(formState.weekStartDate)
  await syncSourcePanel()
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
    const savedReport = props.weeklyReportUuid
      ? await updateWeeklyReport(props.weeklyReportUuid, formState)
      : await createWeeklyReport(formState)

    showToast(props.weeklyReportUuid ? '주간보고를 수정했습니다.' : '주간보고를 저장했습니다.', { variant: 'success' })
    emit('saved', savedReport)
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

  const shouldGenerate = await confirm('AI 초안을 생성할까요?', {
    title: 'AI 초안 생성',
    confirmText: '생성',
    cancelText: '취소'
  })

  if (!shouldGenerate) {
    return
  }

  const availableSourceReports = weekdaySources.value
    .map((item) => item.source)
    .filter((source): source is WeeklyReportDailySource => Boolean(source))
    .slice(0, 5)

  isGeneratingAi.value = true

  try {
    const draft = await generateWeeklyAiDraft({
      weekStartDate: formState.weekStartDate,
      weekEndDate: formState.weekEndDate,
      sourceDailyReports: availableSourceReports
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

watch(
  () => [props.visible, props.weeklyReportUuid],
  async ([visible]) => {
    if (!visible) {
      return
    }

    await initializeModal()
  },
  { immediate: true }
)

watch(isGeneratingAi, (value) => {
  setScrollLock(value)
})

watch(
  () => props.visible,
  (visible) => {
    if (!visible && isGeneratingAi.value) {
      isGeneratingAi.value = false
    }

    if (!visible) {
      setScrollLock(false)
    }
  }
)

watch(
  () => formState.content,
  async () => {
    await nextTick()
    resizeTextarea(contentTextarea.value)
  },
  { immediate: true }
)

onMounted(async () => {
  await nextTick()
  resizeTextarea(contentTextarea.value)
})

onBeforeUnmount(() => {
  setScrollLock(false)
})
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    eyebrow="Weekly Report"
    :title="isEditing ? '주간보고 수정' : '주간보고 작성'"
    width="calc(100vw - 24px)"
    immersive
    :z-index="30"
    @close="emit('close')"
  >
    <section class="weekly-report-form-modal">
      <div class="weekly-report-form-modal__section-header">
        <div>
          <strong>{{ isEditing ? '수정 모드' : '작성 모드' }}</strong>
          <p>주간보고를 작성하면서 같은 주의 일일보고를 바로 참고할 수 있습니다.</p>
        </div>
        <span class="weekly-report-form-modal__generation-badge">
          생성 방식: {{ generationTypeLabelMap[formState.generationType] }}
        </span>
      </div>

      <div v-if="isInitializing" class="weekly-report-form-modal__loading">
        주간보고 정보를 불러오는 중입니다.
      </div>

      <template v-else>
        <div class="weekly-report-form-modal__layout">
          <section class="weekly-report-form-modal__source-panel">
            <div class="weekly-report-form-modal__source-heading">
              <h3>해당 주 일일보고</h3>
              <span v-if="isLoadingSources">불러오는 중...</span>
            </div>

            <div class="weekly-report-form-modal__source-list">
              <article
                v-for="item in weekdaySources"
                :key="item.reportDate"
                class="weekly-report-form-modal__source-entry"
                :class="{
                  'weekly-report-form-modal__source-entry--active': item.reportDate === selectedDailySourceDate,
                  'weekly-report-form-modal__source-entry--missing': !item.source
                }"
              >
                <button
                  type="button"
                  class="weekly-report-form-modal__source-item"
                  @click="loadSelectedDailyReportDetail(item.reportDate)"
                >
                  <strong>{{ formatDateWithWeekday(item.reportDate) }}</strong>
                  <p>
                    {{ item.source ? (item.source.workUnits.length ? item.source.workUnits.map((workUnit) => workUnit.title).join(', ') : '작성된 업무 없음') : '작성되지 않은 날짜입니다.' }}
                  </p>
                </button>

                <Transition name="weekly-report-form-modal-slide">
                  <div
                    v-if="item.reportDate === selectedDailySourceDate"
                    class="weekly-report-form-modal__source-detail-card"
                  >
                    <template v-if="item.source">
                      <section class="weekly-report-form-modal__source-section">
                        <span>업무</span>
                        <p>{{ item.source.workUnits.length ? item.source.workUnits.map((workUnit) => workUnit.title).join(', ') : '선택된 업무 없음' }}</p>
                      </section>

                      <section class="weekly-report-form-modal__source-section">
                        <span>오늘 수행 내용</span>
                        <pre>{{ item.source.content || '작성된 내용이 없습니다.' }}</pre>
                      </section>

                      <section class="weekly-report-form-modal__source-section">
                        <span>이슈 / 특이사항</span>
                        <pre>{{ item.source.note || '작성된 특이사항이 없습니다.' }}</pre>
                      </section>
                    </template>

                    <template v-else>
                      <p class="weekly-report-form-modal__empty">
                        해당 날짜에는 작성된 일일보고가 없습니다.
                      </p>
                    </template>
                  </div>
                </Transition>
              </article>
            </div>
          </section>

          <section class="weekly-report-form-modal__editor">
            <div class="weekly-report-form-modal__week-row">
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

            <div class="weekly-report-form-modal__editor-actions">
              <label class="weekly-report-form-modal__toggle">
                <input v-model="formState.openYn" type="checkbox" true-value="Y" false-value="N">
                <span>오픈 메뉴에 게시</span>
              </label>

              <div class="weekly-report-form-modal__button-row">
                <CommonBaseButton variant="secondary" :disabled="isGeneratingAi" @click="handleGenerateAiDraft">
                  AI 초안 생성
                </CommonBaseButton>
                <CommonBaseButton :disabled="isSaving" @click="handleSave">
                  {{ isEditing ? '수정 저장' : '저장' }}
                </CommonBaseButton>
              </div>
            </div>

            <label>
              <span>본문</span>
              <textarea
                ref="contentTextarea"
                v-model="formState.content"
                class="weekly-report-form-modal__textarea weekly-report-form-modal__textarea--large"
                rows="18"
                @input="handleContentInput"
              />
            </label>
          </section>
        </div>
      </template>

      <div
        v-if="isGeneratingAi"
        class="weekly-report-form-modal__ai-loading-mask"
        aria-live="polite"
        aria-busy="true"
        @wheel.prevent
        @touchmove.prevent
      >
        <div class="weekly-report-form-modal__ai-loading-card">
          <span class="weekly-report-form-modal__ai-loading-spinner" aria-hidden="true" />
          <strong>AI 초안 생성 중</strong>
          <p>왼쪽 일일보고를 취합해 초안을 만들고 있습니다. 잠시만 기다려주세요.</p>
        </div>
      </div>
    </section>

    <template #actions>
      <CommonBaseButton variant="secondary" @click="emit('close')">
        닫기
      </CommonBaseButton>
      <CommonBaseButton :disabled="isSaving" @click="handleSave">
        {{ isEditing ? '수정 저장' : '저장' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.weekly-report-form-modal {
  display: grid;
  gap: 18px;
}

.weekly-report-form-modal__section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.weekly-report-form-modal__section-header p,
.weekly-report-form-modal__empty {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.weekly-report-form-modal__generation-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
}

.weekly-report-form-modal__loading {
  padding: 24px;
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
  color: var(--color-text-muted);
}

.weekly-report-form-modal__ai-loading-mask {
  position: fixed;
  inset: 0;
  z-index: 35;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(12, 16, 24, 0.56);
  backdrop-filter: blur(4px);
}

.weekly-report-form-modal__ai-loading-card {
  display: grid;
  justify-items: center;
  gap: 10px;
  min-width: min(420px, calc(100vw - 48px));
  padding: 24px 28px;
  border-radius: var(--radius-large);
  border: 1px solid rgba(147, 210, 255, 0.22);
  background: rgba(18, 24, 36, 0.94);
  box-shadow: 0 28px 60px rgba(0, 0, 0, 0.28);
  text-align: center;
}

.weekly-report-form-modal__ai-loading-card p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.weekly-report-form-modal__ai-loading-spinner {
  width: 42px;
  height: 42px;
  border: 3px solid rgba(147, 210, 255, 0.2);
  border-top-color: var(--color-accent);
  border-radius: 999px;
  animation: weekly-report-form-modal-spin 0.9s linear infinite;
}

.weekly-report-form-modal__layout {
  display: grid;
  grid-template-columns: minmax(280px, 3fr) minmax(0, 7fr);
  gap: 18px;
  align-items: start;
}

.weekly-report-form-modal__editor,
.weekly-report-form-modal__source-panel {
  display: grid;
  gap: 16px;
  min-height: 0;
}

.weekly-report-form-modal__editor {
  padding: 18px;
  border-radius: var(--radius-large);
  border: 1px solid rgba(147, 210, 255, 0.14);
  background: rgba(255, 255, 255, 0.04);
}

.weekly-report-form-modal__week-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.weekly-report-form-modal__week-row label,
.weekly-report-form-modal__editor label {
  display: grid;
  gap: 8px;
}

.weekly-report-form-modal__week-row label span,
.weekly-report-form-modal__editor label span,
.weekly-report-form-modal__source-section span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.weekly-report-form-modal__editor-actions,
.weekly-report-form-modal__button-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.weekly-report-form-modal__toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.weekly-report-form-modal__textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 96px;
  padding: 14px 16px;
  border-radius: var(--radius-small);
  border: 1px solid rgba(143, 208, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text);
  overflow: hidden;
  resize: none;
  font: inherit;
}

.weekly-report-form-modal__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.22);
  border-color: var(--color-primary);
}

.weekly-report-form-modal__textarea--large {
  min-height: 400px;
}

.weekly-report-form-modal__source-panel {
  display: grid;
  gap: 12px;
  min-height: 0;
  padding: 18px;
  border-radius: var(--radius-large);
  border: 1px solid rgba(147, 210, 255, 0.14);
  background: rgba(255, 255, 255, 0.04);
}

.weekly-report-form-modal__source-list {
  display: grid;
  gap: 12px;
  min-height: 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.weekly-report-form-modal__source-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.weekly-report-form-modal__source-heading h3 {
  margin: 0;
}

.weekly-report-form-modal__source-entry {
  display: grid;
  gap: 10px;
  padding: 0 0 12px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.1);
}

.weekly-report-form-modal__source-item {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 0;
  border-radius: var(--radius-medium);
  background: transparent;
  color: var(--color-text);
  text-align: left;
  font: inherit;
  box-shadow: none;
  outline: none;
  appearance: none;
}

.weekly-report-form-modal__source-item p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.45;
}

.weekly-report-form-modal__source-item:focus,
.weekly-report-form-modal__source-item:focus-visible,
.weekly-report-form-modal__source-item:hover {
  outline: none;
  box-shadow: none;
}

.weekly-report-form-modal__source-entry--active {
  border-bottom-color: rgba(110, 193, 255, 0.24);
}

.weekly-report-form-modal__source-entry--missing {
  border-style: dashed;
}

.weekly-report-form-modal__source-entry--active .weekly-report-form-modal__source-item {
  background: rgba(110, 193, 255, 0.08);
}

.weekly-report-form-modal__source-detail-card {
  display: grid;
  gap: 12px;
  min-height: 0;
  padding: 4px 12px 0;
}

.weekly-report-form-modal__source-section {
  display: grid;
  gap: 8px;
  padding: 10px 0;
  border-radius: 0;
  background: transparent;
  border-top: 1px solid rgba(147, 210, 255, 0.08);
}

.weekly-report-form-modal__source-section p,
.weekly-report-form-modal__source-section pre {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.weekly-report-form-modal-slide-enter-active,
.weekly-report-form-modal-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.weekly-report-form-modal-slide-enter-from,
.weekly-report-form-modal-slide-leave-to {
  opacity: 0;
  transform: translateX(18px);
}

@keyframes weekly-report-form-modal-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1180px) {
  .weekly-report-form-modal__layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .weekly-report-form-modal__section-header,
  .weekly-report-form-modal__editor-actions,
  .weekly-report-form-modal__button-row,
  .weekly-report-form-modal__source-heading,
  .weekly-report-form-modal__week-row {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
