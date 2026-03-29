<script setup lang="ts">
import type { DailyReport, DailyReportMissing, DailyReportStatus, SaveDailyReportPayload, UpdateDailyReportPayload } from '~/types/work'
import SearchableSelect from '~/components/common/SearchableSelect.vue'
import { applySelectableValueFromOptions } from '~/utils/selectable'

definePageMeta({
  middleware: 'auth'
})

const { fetchDailyReports, createDailyReport, updateDailyReport, fetchMissingDailyReports } = useDailyReportApi()
const { showToast } = useToast()

function formatDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getMonthStartInput(): string {
  const now = new Date()
  return formatDateInput(new Date(now.getFullYear(), now.getMonth(), 1))
}

function getDaysAgoInput(days: number): string {
  const targetDate = new Date()
  targetDate.setDate(targetDate.getDate() - days)
  return formatDateInput(targetDate)
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

function toLocalDate(value?: string | null): Date {
  if (!value) {
    return new Date(1970, 0, 1)
  }

  const [year = 1970, month = 1, day = 1] = value.split('-').map((item) => Number(item))
  return new Date(year, month - 1, day)
}

function isWeekend(value: string): boolean {
  const date = toLocalDate(value)
  const day = date.getDay()
  return day === 0 || day === 6
}

function isFutureDate(value: string): boolean {
  const date = toLocalDate(value)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() > today.getTime()
}

function formatMissingDateLabel(value: string): string {
  const weekday = new Intl.DateTimeFormat('ko-KR', { weekday: 'short' }).format(toLocalDate(value))
  return `${formatDate(value)} (${weekday})`
}

const statusOptions: Array<{ value: DailyReportStatus | ''; label: string }> = [
  { value: '', label: '전체' },
  { value: 'PLANNED', label: '예정' },
  { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'DONE', label: '완료' }
]

const statusLabelMap: Record<DailyReportStatus, string> = {
  PLANNED: '예정',
  IN_PROGRESS: '진행중',
  DONE: '완료'
}

interface DailyReportFormState {
  reportDate: string
  status: DailyReportStatus
  note: string
}

const filters = reactive({
  dateFrom: getMonthStartInput(),
  dateTo: formatDateInput(new Date()),
  status: '' as DailyReportStatus | '',
  keyword: '',
  page: 1,
  size: 10
})

const formState = reactive<DailyReportFormState>({
  reportDate: formatDateInput(new Date()),
  status: 'IN_PROGRESS',
  note: ''
})

const editingDailyReportUuid = ref('')
const isFormVisible = ref(false)
const isMissingPanelVisible = ref(true)
const isLoading = ref(false)
const isSubmitting = ref(false)
const isRecentReferenceLoading = ref(false)
const dailyReportPage = ref({
  content: [] as DailyReport[],
  page: 1,
  size: 10,
  totalCount: 0,
  totalPages: 0
})
const missingDailyReports = ref<DailyReportMissing[]>([])
const recentReferenceReports = ref<DailyReport[]>([])
const workItems = ref<string[]>([''])

const visibleMissingDailyReports = computed(() =>
  missingDailyReports.value.filter((item) => !isWeekend(item.reportDate) && !isFutureDate(item.reportDate))
)
const summary = computed(() => ({
  missingCount: visibleMissingDailyReports.value.length,
  totalCount: Number.isFinite(dailyReportPage.value.totalCount) ? dailyReportPage.value.totalCount : 0
}))

function resetForm() {
  editingDailyReportUuid.value = ''
  formState.reportDate = formatDateInput(new Date())
  formState.status = 'IN_PROGRESS'
  formState.note = ''
  workItems.value = ['']
}

function parseWorkItems(content: string): string[] {
  const normalized = content.replace(/\r\n/g, '\n').trim()

  if (!normalized) {
    return ['']
  }

  const parsedItems = normalized
    .split(/\n{2,}/)
    .map((item) => item.trim())
    .filter(Boolean)

  return parsedItems.length ? parsedItems : ['']
}

function buildContentFromWorkItems(): string {
  return workItems.value
    .map((item) => item.trim())
    .filter(Boolean)
    .join('\n\n')
}

function addWorkItem() {
  workItems.value.push('')
}

function removeWorkItem(index: number) {
  if (workItems.value.length === 1) {
    workItems.value[0] = ''
    return
  }

  workItems.value.splice(index, 1)
}

function appendReferenceWorkItems(report: DailyReport) {
  const referenceItems = parseWorkItems(report.content).filter((item) => item.trim())

  if (!referenceItems.length) {
    return
  }

  const normalizedCurrentItems = workItems.value.filter((item) => item.trim())
  workItems.value = [...normalizedCurrentItems, ...referenceItems]
}

function getReferenceWorkItems(report: DailyReport): string[] {
  return parseWorkItems(report.content).filter((item) => item.trim())
}

async function loadRecentReferenceReports() {
  isRecentReferenceLoading.value = true

  try {
    const response = await fetchDailyReports({
      dateFrom: getDaysAgoInput(90),
      dateTo: formatDateInput(new Date()),
      status: '',
      page: 1,
      size: 6
    })

    recentReferenceReports.value = response.content.filter((report) => report.uuid !== editingDailyReportUuid.value)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    recentReferenceReports.value = []
    showToast(fetchError.data?.message ?? '최근 작성 보고를 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isRecentReferenceLoading.value = false
  }
}

function startCreateMode() {
  resetForm()
  isFormVisible.value = true
  void loadRecentReferenceReports()
}

function startEditMode(report: DailyReport) {
  editingDailyReportUuid.value = report.uuid
  formState.reportDate = report.reportDate
  formState.status = report.status
  formState.note = report.note ?? ''
  workItems.value = parseWorkItems(report.content)
  isFormVisible.value = true
  void loadRecentReferenceReports()
}

function closeFormModal() {
  isFormVisible.value = false
  resetForm()
}

function updateFormStatus(status: string) {
  applySelectableValueFromOptions(status, statusOptions.filter((option) => option.value), (validStatus) => {
    formState.status = validStatus as DailyReportStatus
  })
}

function updateFilterStatus(status: string) {
  applySelectableValueFromOptions(status, statusOptions, (validStatus) => {
    filters.status = validStatus as DailyReportStatus | ''
  })
}

async function loadDailyReports() {
  isLoading.value = true

  try {
    dailyReportPage.value = await fetchDailyReports({
      dateFrom: filters.dateFrom,
      dateTo: filters.dateTo,
      status: filters.status,
      keyword: filters.keyword.trim() || undefined,
      page: filters.page,
      size: filters.size
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    dailyReportPage.value.content = []
    showToast(fetchError.data?.message ?? '일일보고 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoading.value = false
  }
}

async function loadMissingDailyReports() {
  try {
    missingDailyReports.value = await fetchMissingDailyReports({
      dateFrom: filters.dateFrom,
      dateTo: filters.dateTo
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    missingDailyReports.value = []
    showToast(fetchError.data?.message ?? '누락 현황을 불러오지 못했습니다.', { variant: 'error' })
  }
}

async function reloadAll() {
  await Promise.all([loadDailyReports(), loadMissingDailyReports()])
}

async function handleSubmit() {
  if (isSubmitting.value) {
    return
  }

  if (!formState.reportDate) {
    showToast('작성일자를 선택해주세요.', { variant: 'error' })
    return
  }

  const content = buildContentFromWorkItems()

  if (!content) {
    showToast('작업한 업무를 1개 이상 입력해주세요.', { variant: 'error' })
    return
  }

  isSubmitting.value = true

  try {
    if (editingDailyReportUuid.value) {
      const payload: UpdateDailyReportPayload = {
        content,
        status: formState.status,
        note: formState.note?.trim() || ''
      }
      await updateDailyReport(editingDailyReportUuid.value, payload)
      showToast('일일보고를 수정했습니다.', { variant: 'success' })
    } else {
      await createDailyReport({
        reportDate: formState.reportDate,
        content,
        status: formState.status,
        note: formState.note?.trim() || ''
      })
      showToast('일일보고를 저장했습니다.', { variant: 'success' })
    }

    isFormVisible.value = false
    resetForm()
    filters.page = 1
    await reloadAll()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '일일보고 저장에 실패했습니다.', { variant: 'error' })
  } finally {
    isSubmitting.value = false
  }
}

async function handleSearch() {
  filters.page = 1
  await reloadAll()
}

async function movePage(nextPage: number) {
  if (nextPage < 1 || nextPage > Math.max(dailyReportPage.value.totalPages, 1) || nextPage === filters.page) {
    return
  }

  filters.page = nextPage
  await loadDailyReports()
}

await reloadAll()
</script>

<template>
  <main class="page-container daily-report-page">
    <section class="content-panel daily-report-page__hero">
      <div class="daily-report-page__hero-header">
        <div>
          <p class="daily-report-page__eyebrow">Daily Report</p>
          <h1 class="section-title">일일보고</h1>
          <p class="section-description">
            개인용 일일보고를 날짜별로 기록하고, 누락 여부를 함께 확인합니다.
          </p>
        </div>

        <div class="daily-report-page__hero-side">
          <CommonBaseButton @click="startCreateMode">
            일일보고 작성
          </CommonBaseButton>

          <div class="daily-report-page__summary" role="status" aria-label="일일보고 요약">
            <span>조회 결과 <strong>{{ summary.totalCount }}</strong>건</span>
            <span class="daily-report-page__summary-divider">|</span>
            <span>누락 <strong>{{ summary.missingCount }}</strong>일</span>
          </div>
        </div>
      </div>
    </section>

    <section class="content-panel daily-report-page__panel">
      <div class="daily-report-page__panel-header">
        <h2>목록 조회</h2>
        <span v-if="isLoading">불러오는 중...</span>
      </div>

      <form class="daily-report-page__filters" @submit.prevent="handleSearch">
        <label>
          <span>시작일</span>
          <input v-model="filters.dateFrom" class="input-field" type="date">
        </label>
        <label>
          <span>종료일</span>
          <input v-model="filters.dateTo" class="input-field" type="date">
        </label>
        <label>
          <span>진행상태</span>
          <SearchableSelect
            :options="statusOptions"
            :model-value="filters.status"
            placeholder="진행상태"
            input-class="input-field"
            @update:modelValue="updateFilterStatus"
          />
        </label>
        <label class="daily-report-page__search-field">
          <span>내용 검색</span>
          <input v-model="filters.keyword" class="input-field" type="search" placeholder="업무내용 또는 특이사항">
        </label>
        <div class="daily-report-page__filter-actions">
          <CommonBaseButton variant="secondary" type="button" @click="filters.dateFrom = getMonthStartInput(); filters.dateTo = formatDateInput(new Date()); filters.status = ''; filters.keyword = ''; void handleSearch()">
            초기화
          </CommonBaseButton>
          <CommonBaseButton type="submit">
            조회
          </CommonBaseButton>
        </div>
      </form>

      <div class="daily-report-page__table-wrap">
        <table class="daily-report-page__table">
          <thead>
            <tr>
              <th>작성일자</th>
              <th>상태</th>
              <th>업무내용</th>
              <th>특이사항</th>
              <th>수정일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="dailyReport in dailyReportPage.content" :key="dailyReport.uuid">
              <td>{{ formatDate(dailyReport.reportDate) }}</td>
              <td>
                <span class="daily-report-page__status-badge">
                  {{ statusLabelMap[dailyReport.status] }}
                </span>
              </td>
              <td class="daily-report-page__content-cell">
                {{ dailyReport.content }}
              </td>
              <td class="daily-report-page__content-cell">
                {{ dailyReport.note || '-' }}
              </td>
              <td>{{ formatDateTime(dailyReport.updatedAt) }}</td>
              <td>
                <CommonBaseButton variant="secondary" size="small" @click="startEditMode(dailyReport)">
                  수정
                </CommonBaseButton>
              </td>
            </tr>
            <tr v-if="!isLoading && dailyReportPage.content.length === 0">
              <td colspan="6" class="daily-report-page__empty">
                조회된 일일보고가 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <CommonBasePagination
        :page="dailyReportPage.page"
        :total-pages="dailyReportPage.totalPages"
        @previous="movePage(filters.page - 1)"
        @next="movePage(filters.page + 1)"
      />
    </section>

    <button
      type="button"
      class="daily-report-page__missing-toggle"
      :class="{ 'daily-report-page__missing-toggle--active': isMissingPanelVisible }"
      @click="isMissingPanelVisible = !isMissingPanelVisible"
    >
      누락 {{ summary.missingCount }}일
    </button>

    <aside v-if="isMissingPanelVisible" class="daily-report-page__floating-panel">
      <section class="content-panel daily-report-page__panel daily-report-page__floating-panel-card">
        <div class="daily-report-page__panel-header">
          <h2>누락 현황</h2>
          <div class="daily-report-page__floating-header-actions">
            <strong>{{ summary.missingCount }}일 누락</strong>
            <button type="button" class="daily-report-page__floating-close" @click="isMissingPanelVisible = false">
              닫기
            </button>
          </div>
        </div>

        <p class="daily-report-page__empty-text">
          선택한 기간 안에서 아직 작성하지 않은 평일만 표시합니다.
        </p>

        <div v-if="visibleMissingDailyReports.length" class="daily-report-page__missing-list">
          <div
            v-for="missingDailyReport in visibleMissingDailyReports"
            :key="missingDailyReport.reportDate"
            class="daily-report-page__missing-item"
          >
            <span class="daily-report-page__missing-dot" />
            <strong>{{ formatMissingDateLabel(missingDailyReport.reportDate) }}</strong>
          </div>
        </div>
        <p v-else class="daily-report-page__empty-text">
          현재 조회 구간에는 누락된 평일이 없습니다.
        </p>
      </section>
    </aside>

    <CommonBaseModal
      :visible="isFormVisible"
      eyebrow="Daily Report"
      :title="editingDailyReportUuid ? '일일보고 수정' : '일일보고 작성'"
      width="1120px"
      @close="closeFormModal"
    >
      <div class="daily-report-page__modal-layout">
        <form class="daily-report-page__form" @submit.prevent="handleSubmit">
          <div class="daily-report-page__form-row">
            <label>
              <span>작성일자</span>
              <input
                v-model="formState.reportDate"
                class="input-field"
                type="date"
                :disabled="Boolean(editingDailyReportUuid)"
              >
            </label>

            <label>
              <span>진행상태</span>
              <SearchableSelect
                :options="statusOptions.filter((option) => option.value)"
                :model-value="formState.status"
                placeholder="진행상태"
                input-class="input-field"
                @update:modelValue="updateFormStatus"
              />
            </label>
          </div>

          <div class="daily-report-page__work-header">
            <div>
              <span class="daily-report-page__work-label">작성내용</span>
              <p class="daily-report-page__work-description">작업한 업무를 항목별로 나눠 기록합니다.</p>
            </div>
            <CommonBaseButton variant="secondary" type="button" @click="addWorkItem">
              업무 추가
            </CommonBaseButton>
          </div>

          <div class="daily-report-page__work-list">
            <article
              v-for="(workItem, workItemIndex) in workItems"
              :key="`work-item-${workItemIndex}`"
              class="daily-report-page__work-item"
            >
              <div class="daily-report-page__work-item-header">
                <strong>업무 {{ workItemIndex + 1 }}</strong>
                <CommonBaseButton variant="secondary" size="small" type="button" @click="removeWorkItem(workItemIndex)">
                  삭제
                </CommonBaseButton>
              </div>
              <textarea
                v-model="workItems[workItemIndex]"
                class="daily-report-page__textarea"
                rows="4"
                placeholder="수행한 업무 내용을 입력하세요."
              />
            </article>
          </div>

          <label>
            <span>특이사항</span>
            <textarea v-model="formState.note" class="daily-report-page__textarea" rows="3" />
          </label>
        </form>

        <aside class="daily-report-page__reference-panel">
          <div class="daily-report-page__reference-header">
            <div>
              <span class="daily-report-page__work-label">최근 작성 보고</span>
              <p class="daily-report-page__work-description">최근 보고의 업무 항목을 보면서 현재 보고에 필요한 내용만 가져올 수 있습니다.</p>
            </div>
          </div>

          <div v-if="isRecentReferenceLoading" class="daily-report-page__reference-empty">
            최근 보고를 불러오는 중입니다.
          </div>
          <div v-else-if="recentReferenceReports.length" class="daily-report-page__reference-list">
            <article
              v-for="referenceReport in recentReferenceReports"
              :key="referenceReport.uuid"
              class="daily-report-page__reference-item"
            >
              <div class="daily-report-page__reference-meta">
                <strong>{{ formatDate(referenceReport.reportDate) }}</strong>
                <span class="daily-report-page__status-badge">
                  {{ statusLabelMap[referenceReport.status] }}
                </span>
              </div>
              <ul class="daily-report-page__reference-work-list">
                <li
                  v-for="(referenceWorkItem, referenceWorkItemIndex) in getReferenceWorkItems(referenceReport)"
                  :key="`${referenceReport.uuid}-item-${referenceWorkItemIndex}`"
                  class="daily-report-page__reference-work-item"
                >
                  {{ referenceWorkItem }}
                </li>
              </ul>
              <CommonBaseButton variant="secondary" size="small" type="button" @click="appendReferenceWorkItems(referenceReport)">
                업무에 가져오기
              </CommonBaseButton>
            </article>
          </div>
          <div v-else class="daily-report-page__reference-empty">
            최근에 작성한 보고가 없습니다.
          </div>
        </aside>
      </div>

      <template #actions>
        <CommonBaseButton variant="secondary" type="button" :disabled="isSubmitting" @click="closeFormModal">
          취소
        </CommonBaseButton>
        <CommonBaseButton type="button" :disabled="isSubmitting" @click="handleSubmit">
          {{ editingDailyReportUuid ? '수정 저장' : '저장' }}
        </CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.daily-report-page {
  display: grid;
  gap: 24px;
}

.daily-report-page__hero,
.daily-report-page__panel {
  padding: 20px 22px;
}

.daily-report-page__hero {
  display: grid;
  gap: 12px;
}

.daily-report-page__hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.daily-report-page__hero-side {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.daily-report-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.daily-report-page__summary {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.daily-report-page__summary strong {
  color: var(--color-text);
  font-size: 1rem;
}

.daily-report-page__summary-divider {
  opacity: 0.45;
}

.daily-report-page__panel {
  display: grid;
  gap: 18px;
}

.daily-report-page__panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.daily-report-page__form-actions,
.daily-report-page__filter-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.daily-report-page__filters {
  display: grid;
  grid-template-columns: minmax(132px, 0.9fr) minmax(132px, 0.9fr) minmax(150px, 0.9fr) minmax(220px, 1.4fr) auto;
  align-items: end;
  gap: 14px 16px;
}

.daily-report-page__filter-actions {
  justify-content: flex-end;
  white-space: nowrap;
}

.daily-report-page__form label,
.daily-report-page__filters label {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.daily-report-page__form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.daily-report-page__form label span,
.daily-report-page__filters label span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.daily-report-page__search-field,
.daily-report-page__table-wrap {
  min-width: 0;
}

.daily-report-page__empty-text {
  color: var(--color-text-muted);
}

.daily-report-page__missing-list {
  display: grid;
  gap: 10px;
}

.daily-report-page__missing-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius-medium);
  background: rgba(255, 115, 115, 0.08);
  border: 1px solid rgba(255, 115, 115, 0.18);
}

.daily-report-page__missing-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #ff8b8b;
  flex-shrink: 0;
}

.daily-report-page__missing-more {
  margin: 2px 0 0;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.daily-report-page__missing-toggle {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 12;
  min-height: 48px;
  padding: 0 18px;
  border: 1px solid rgba(147, 210, 255, 0.22);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(17, 43, 74, 0.98), rgba(8, 23, 42, 0.94));
  color: rgba(232, 244, 255, 0.96);
  box-shadow: 0 18px 36px rgba(2, 8, 18, 0.32);
  font-weight: 700;
}

.daily-report-page__missing-toggle--active {
  opacity: 0.78;
}

.daily-report-page__floating-panel {
  position: fixed;
  right: 24px;
  top: 140px;
  z-index: 11;
  width: min(360px, calc(100vw - 32px));
}

.daily-report-page__floating-panel-card {
  max-height: calc(100vh - 164px);
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  box-shadow: 0 24px 50px rgba(2, 8, 18, 0.34);
}

.daily-report-page__floating-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.daily-report-page__floating-close {
  border: 0;
  background: transparent;
  color: var(--color-text-muted);
  font: inherit;
}

.daily-report-page__textarea {
  min-height: 120px;
  padding: 14px 16px;
  border-radius: var(--radius-small);
  border: 1px solid rgba(143, 208, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text);
  resize: vertical;
  font: inherit;
}

.daily-report-page__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.22);
  border-color: var(--color-primary);
}

.daily-report-page__form {
  display: grid;
  gap: 16px;
}

.daily-report-page__modal-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr);
  gap: 20px;
  min-height: 0;
}

.daily-report-page__work-header,
.daily-report-page__work-item-header,
.daily-report-page__reference-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.daily-report-page__work-label {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.daily-report-page__work-description {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  font-size: 0.86rem;
}

.daily-report-page__work-list,
.daily-report-page__reference-list {
  display: grid;
  gap: 12px;
}

.daily-report-page__work-item,
.daily-report-page__reference-item {
  display: grid;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.daily-report-page__reference-panel {
  display: grid;
  align-content: start;
  gap: 14px;
  min-height: 0;
}

.daily-report-page__reference-work-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}

.daily-report-page__reference-work-item,
.daily-report-page__reference-empty {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.daily-report-page__table {
  width: 100%;
  border-collapse: collapse;
}

.daily-report-page__table th,
.daily-report-page__table td {
  padding: 12px 10px;
  border-bottom: 1px solid rgba(143, 208, 255, 0.12);
  text-align: left;
  vertical-align: top;
}

.daily-report-page__content-cell {
  white-space: pre-wrap;
}

.daily-report-page__status-badge {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
}

.daily-report-page__empty {
  text-align: center;
  color: var(--color-text-muted);
}

:deep(.base-modal) {
  display: flex;
  justify-content: flex-end;
  align-items: stretch;
  padding: 0;
}

:deep(.base-modal__backdrop) {
  background: rgba(6, 14, 24, 0.44);
}

:deep(.base-modal__panel) {
  width: min(100%, 720px);
  max-width: min(100%, 720px) !important;
  height: 100vh;
  max-height: 100vh;
  margin-left: auto;
  border-radius: 0;
  border-left: 1px solid rgba(147, 210, 255, 0.16);
  box-shadow: -24px 0 60px rgba(2, 8, 18, 0.34);
}

:deep(.base-modal__body) {
  min-height: 0;
}

:deep(.base-modal__actions) {
  position: sticky;
  bottom: 0;
  padding-top: 12px;
  background: linear-gradient(180deg, rgba(6, 18, 34, 0), rgba(6, 18, 34, 0.96) 28%);
}

@media (max-width: 1080px) {
  .daily-report-page__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .daily-report-page__hero-header {
    flex-direction: column;
  }

  .daily-report-page__hero-side {
    justify-items: stretch;
  }

  .daily-report-page__filters,
  .daily-report-page__form-row,
  .daily-report-page__modal-layout {
    grid-template-columns: 1fr;
  }

  .daily-report-page__summary {
    width: 100%;
    min-width: 0;
    justify-content: center;
    flex-wrap: wrap;
  }

  .daily-report-page__floating-panel {
    right: 12px;
    left: 12px;
    top: auto;
    bottom: 84px;
    width: auto;
  }

  .daily-report-page__floating-panel-card {
    max-height: calc(100vh - 108px);
  }

  .daily-report-page__missing-toggle {
    right: 12px;
    bottom: 12px;
  }

  :deep(.base-modal__panel) {
    width: 100%;
    max-width: 100% !important;
  }

  .daily-report-page__filter-actions {
    justify-content: stretch;
  }

  .daily-report-page__filter-actions :deep(.base-button) {
    width: 100%;
  }
}
</style>
