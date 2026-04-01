<script setup lang="ts">
import type { DailyReport, DailyReportWorkUnit, SaveDailyReportPayload, UpdateDailyReportPayload, WorkUnit, WorkUnitGitCommit, WorkUnitOption } from '~/types/work'

const props = defineProps<{
  dailyReportUuid?: string
}>()

const isEditing = computed(() => Boolean(props.dailyReportUuid))

const { fetchDailyReport, fetchDailyReports, createDailyReport, updateDailyReport } = useDailyReportApi()
const { fetchWorkUnitOptions, fetchWorkUnit, fetchWorkUnitGitCommits } = useWorkUnitApi()
const { showToast } = useToast()

interface DailyReportFormState {
  reportDate: string
  workSummary: string
  issueNote: string
}

function formatDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getDaysAgoInput(days: number): string {
  const targetDate = new Date()
  targetDate.setDate(targetDate.getDate() - days)
  return formatDateInput(targetDate)
}

function extractSection(note: string | null | undefined, heading: string): string {
  if (!note) {
    return ''
  }

  const escapedHeading = heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const pattern = new RegExp(`\\[${escapedHeading}\\]\\n([\\s\\S]*?)(?=\\n\\n\\[[^\\]]+\\]\\n|$)`)
  const match = note.match(pattern)
  return match?.[1]?.trim() ?? ''
}

function removeSection(note: string | null | undefined, heading: string): string {
  if (!note) {
    return ''
  }

  const escapedHeading = heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const pattern = new RegExp(`\\n?\\[${escapedHeading}\\]\\n([\\s\\S]*?)(?=\\n\\n\\[[^\\]]+\\]\\n|$)`, 'g')
  return note.replace(pattern, '').trim()
}

function firstLine(value: string): string {
  return value.split('\n')[0]?.trim() ?? ''
}

function buildImportedMarkdown(
  selectedWorkUnits: DailyReportWorkUnit[],
  selectedGitCommitsByWorkUnit: Record<string, WorkUnitGitCommit[]>
): string {
  if (!selectedWorkUnits.length) {
    return ''
  }

  return selectedWorkUnits.map((workUnit) => {
    const lines = [`## ${workUnit.title}`]
    const selectedGitCommits = selectedGitCommitsByWorkUnit[workUnit.workUnitUuid] ?? []

    if (selectedGitCommits.length) {
      lines.push(...selectedGitCommits.map((commit) => `- ${firstLine(commit.message) || commit.commitSha.slice(0, 7)}`))
    }

    return lines.join('\n')
  }).join('\n\n').trim()
}

const formState = reactive<DailyReportFormState>({
  reportDate: formatDateInput(new Date()),
  workSummary: '',
  issueNote: ''
})

const isPageLoading = ref(false)
const isSubmitting = ref(false)
const isHistoryLoading = ref(false)
const isImportModalVisible = ref(false)
const isWorkUnitLoading = ref(false)
const isCommitLoading = ref(false)
const historyPreviewReportUuid = ref('')
const workUnitSearchKeyword = ref('')
const selectedWorkUnits = ref<DailyReportWorkUnit[]>([])
const recentDailyReports = ref<DailyReport[]>([])
const modalWorkUnitOptions = ref<WorkUnitOption[]>([])
const modalSelectedWorkUnitUuids = ref<string[]>([])
const modalActiveWorkUnitUuid = ref('')
const modalSelectedWorkUnit = ref<WorkUnit | null>(null)
const modalGitCommits = ref<WorkUnitGitCommit[]>([])
const modalSelectedCommitShas = ref<string[]>([])
const selectedGitCommits = ref<WorkUnitGitCommit[]>([])
const modalGitCommitsByWorkUnit = ref<Record<string, WorkUnitGitCommit[]>>({})
const modalSelectedCommitShasByWorkUnit = ref<Record<string, string[]>>({})

const filteredWorkUnitOptions = computed(() => {
  const keyword = workUnitSearchKeyword.value.trim().toLowerCase()

  if (!keyword) {
    return modalWorkUnitOptions.value
  }

  return modalWorkUnitOptions.value.filter((workUnit) =>
    [workUnit.title, workUnit.category ?? '', workUnit.status]
      .join(' ')
      .toLowerCase()
      .includes(keyword)
  )
})

const historyReports = computed(() =>
  recentDailyReports.value
    .filter((report) => report.uuid !== props.dailyReportUuid)
    .sort((first, second) => second.reportDate.localeCompare(first.reportDate))
)

const historyPreviewReport = computed(() =>
  historyReports.value.find((report) => report.uuid === historyPreviewReportUuid.value) ?? historyReports.value.at(0) ?? null
)

const selectedWorkUnitSummary = computed(() => {
  if (!selectedWorkUnits.value.length) {
    return '선택된 업무 없음'
  }

  return selectedWorkUnits.value.map((workUnit) => workUnit.title).join(', ')
})

const selectedCommitSummary = computed(() => {
  if (!selectedGitCommits.value.length) {
    return '선택된 커밋 없음'
  }

  const repositoryNames = Array.from(new Set(selectedGitCommits.value.map((commit) => commit.repositoryName))).filter(Boolean)
  return `${selectedGitCommits.value.length}개 커밋${repositoryNames.length ? ` · ${repositoryNames.join(', ')}` : ''}`
})

const canApplyImport = computed(() => modalSelectedWorkUnitUuids.value.length > 0)
const isAllModalCommitsSelected = computed(() =>
  modalGitCommits.value.length > 0
  && modalGitCommits.value.every((commit) => modalSelectedCommitShas.value.includes(commit.commitSha))
)
const modalCommitQuerySummary = computed(() => {
  if (!formState.reportDate) {
    return ''
  }

  const authorNames = Array.from(new Set(
    modalGitCommits.value
      .map((commit) => commit.authorName.trim())
      .filter(Boolean)
  ))

  if (!authorNames.length) {
    return `${formState.reportDate} 기준`
  }

  return `${formState.reportDate} 기준 · ${authorNames.join(', ')}`
})

watch(historyReports, (nextReports) => {
  if (!nextReports.length) {
    historyPreviewReportUuid.value = ''
    return
  }

  if (!nextReports.some((report) => report.uuid === historyPreviewReportUuid.value)) {
    historyPreviewReportUuid.value = nextReports[0]?.uuid ?? ''
  }
})

watch(
  () => formState.reportDate,
  async () => {
    if (!isImportModalVisible.value || !modalActiveWorkUnitUuid.value) {
      return
    }

    await loadGitCommitsForSelectedWorkUnit()
  }
)

async function loadHistoryReports() {
  isHistoryLoading.value = true

  try {
    const response = await fetchDailyReports({
      dateTo: formatDateInput(new Date()),
      page: 1,
      size: 3
    })

    recentDailyReports.value = response.content
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    recentDailyReports.value = []
    showToast(fetchError.data?.message ?? '이전 일일보고를 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isHistoryLoading.value = false
  }
}

async function initializeWorkspace() {
  isPageLoading.value = true

  try {
    await loadHistoryReports()

    if (!props.dailyReportUuid) {
      return
    }

    const report = await fetchDailyReport(props.dailyReportUuid)
    formState.reportDate = report.reportDate
    formState.workSummary = removeSection(report.content, '이슈 / 특이사항')
    formState.issueNote = report.note ?? extractSection(report.content, '이슈 / 특이사항')
    selectedWorkUnits.value = report.workUnits
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '일일보고를 불러오지 못했습니다.', { variant: 'error' })
    await navigateTo('/work/daily-reports')
  } finally {
    isPageLoading.value = false
  }
}

async function openImportModal() {
  isImportModalVisible.value = true
  isWorkUnitLoading.value = true
  workUnitSearchKeyword.value = ''

  try {
    modalWorkUnitOptions.value = await fetchWorkUnitOptions(true)
    modalSelectedWorkUnitUuids.value = selectedWorkUnits.value.map((workUnit) => workUnit.workUnitUuid)

    const selectedWorkUnitUuid = modalSelectedWorkUnitUuids.value[0] ?? modalWorkUnitOptions.value[0]?.workUnitUuid ?? ''
    if (selectedWorkUnitUuid) {
      await selectModalWorkUnit(selectedWorkUnitUuid)
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    modalWorkUnitOptions.value = []
    showToast(fetchError.data?.message ?? '업무 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isWorkUnitLoading.value = false
  }
}

function closeImportModal() {
  isImportModalVisible.value = false
  modalSelectedWorkUnitUuids.value = []
  modalActiveWorkUnitUuid.value = ''
  modalSelectedWorkUnit.value = null
  modalGitCommits.value = []
  modalSelectedCommitShas.value = []
  modalGitCommitsByWorkUnit.value = {}
  modalSelectedCommitShasByWorkUnit.value = {}
}

async function selectModalWorkUnit(workUnitUuid: string) {
  modalActiveWorkUnitUuid.value = workUnitUuid

  if (!modalSelectedWorkUnitUuids.value.includes(workUnitUuid)) {
    modalSelectedWorkUnitUuids.value = [...modalSelectedWorkUnitUuids.value, workUnitUuid]
  }

  try {
    modalSelectedWorkUnit.value = await fetchWorkUnit(workUnitUuid)
    await loadGitCommitsForSelectedWorkUnit()
    modalSelectedCommitShas.value = modalSelectedCommitShasByWorkUnit.value[workUnitUuid] ?? []
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    modalSelectedWorkUnit.value = null
    modalGitCommits.value = []
    modalSelectedCommitShas.value = []
    showToast(fetchError.data?.message ?? '업무 상세를 불러오지 못했습니다.', { variant: 'error' })
  }
}

async function loadGitCommitsForSelectedWorkUnit() {
  if (!modalActiveWorkUnitUuid.value || !formState.reportDate) {
    modalGitCommits.value = []
    return
  }

  isCommitLoading.value = true

  try {
    modalGitCommits.value = await fetchWorkUnitGitCommits(modalActiveWorkUnitUuid.value, formState.reportDate)
    modalGitCommitsByWorkUnit.value = {
      ...modalGitCommitsByWorkUnit.value,
      [modalActiveWorkUnitUuid.value]: modalGitCommits.value
    }
    modalSelectedCommitShas.value = (modalSelectedCommitShasByWorkUnit.value[modalActiveWorkUnitUuid.value] ?? [])
      .filter((commitSha) => modalGitCommits.value.some((commit) => commit.commitSha === commitSha))
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    modalGitCommits.value = []
    modalGitCommitsByWorkUnit.value = {
      ...modalGitCommitsByWorkUnit.value,
      [modalActiveWorkUnitUuid.value]: []
    }
    modalSelectedCommitShas.value = []
    showToast(fetchError.data?.message ?? '커밋 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isCommitLoading.value = false
  }
}

function toggleWorkUnitSelection(workUnitUuid: string, checked: boolean) {
  if (checked) {
    modalSelectedWorkUnitUuids.value = [...new Set([...modalSelectedWorkUnitUuids.value, workUnitUuid])]
    return
  }

  modalSelectedWorkUnitUuids.value = modalSelectedWorkUnitUuids.value.filter((value) => value !== workUnitUuid)
}

function toggleCommitSelection(commitSha: string, checked: boolean) {
  if (checked) {
    modalSelectedCommitShas.value = [...new Set([...modalSelectedCommitShas.value, commitSha])]
    modalSelectedCommitShasByWorkUnit.value = {
      ...modalSelectedCommitShasByWorkUnit.value,
      [modalActiveWorkUnitUuid.value]: modalSelectedCommitShas.value
    }
    return
  }

  modalSelectedCommitShas.value = modalSelectedCommitShas.value.filter((value) => value !== commitSha)
  modalSelectedCommitShasByWorkUnit.value = {
    ...modalSelectedCommitShasByWorkUnit.value,
    [modalActiveWorkUnitUuid.value]: modalSelectedCommitShas.value
  }
}

function toggleAllCommitSelections() {
  if (!modalGitCommits.value.length) {
    return
  }

  if (isAllModalCommitsSelected.value) {
    modalSelectedCommitShas.value = []
    modalSelectedCommitShasByWorkUnit.value = {
      ...modalSelectedCommitShasByWorkUnit.value,
      [modalActiveWorkUnitUuid.value]: []
    }
    return
  }

  modalSelectedCommitShas.value = modalGitCommits.value.map((commit) => commit.commitSha)
  modalSelectedCommitShasByWorkUnit.value = {
    ...modalSelectedCommitShasByWorkUnit.value,
    [modalActiveWorkUnitUuid.value]: modalSelectedCommitShas.value
  }
}

function applyWorkImport() {
  if (!modalSelectedWorkUnitUuids.value.length) {
    return
  }

  selectedWorkUnits.value = modalSelectedWorkUnitUuids.value
    .map((workUnitUuid) => modalWorkUnitOptions.value.find((workUnit) => workUnit.workUnitUuid === workUnitUuid))
    .filter((workUnit): workUnit is WorkUnitOption => Boolean(workUnit))
    .map((workUnit) => ({
      workUnitUuid: workUnit.workUnitUuid,
      title: workUnit.title,
      category: workUnit.category
    }))

  const selectedGitCommitsByWorkUnit = Object.fromEntries(
    modalSelectedWorkUnitUuids.value.map((workUnitUuid) => {
      const commits = modalGitCommitsByWorkUnit.value[workUnitUuid] ?? []
      const selectedCommitShas = modalSelectedCommitShasByWorkUnit.value[workUnitUuid] ?? []
      return [workUnitUuid, commits.filter((commit) => selectedCommitShas.includes(commit.commitSha))]
    })
  ) as Record<string, WorkUnitGitCommit[]>

  selectedGitCommits.value = Object.values(selectedGitCommitsByWorkUnit).flat()
  formState.workSummary = buildImportedMarkdown(selectedWorkUnits.value, selectedGitCommitsByWorkUnit)
  closeImportModal()
}

function applyHistoryDraft(report: DailyReport) {
  formState.workSummary = removeSection(report.content, '이슈 / 특이사항') || report.content || ''
  formState.issueNote = report.note ?? extractSection(report.content, '이슈 / 특이사항')
  historyPreviewReportUuid.value = report.uuid
}

async function handleSubmit() {
  if (isSubmitting.value) {
    return
  }

  if (!formState.reportDate) {
    showToast('작성일자를 선택해주세요.', { variant: 'error' })
    return
  }

  if (!formState.workSummary.trim() && !formState.issueNote.trim()) {
    showToast('오늘 수행 내용 또는 이슈를 입력해주세요.', { variant: 'error' })
    return
  }

  isSubmitting.value = true

  try {
    if (isEditing.value && props.dailyReportUuid) {
      const payload: UpdateDailyReportPayload = {
        workUnitUuids: selectedWorkUnits.value.map((workUnit) => workUnit.workUnitUuid),
        content: formState.workSummary.trim(),
        note: formState.issueNote.trim()
      }
      await updateDailyReport(props.dailyReportUuid, payload)
      showToast('일일보고를 수정했습니다.', { variant: 'success' })
    } else {
      const payload: SaveDailyReportPayload = {
        reportDate: formState.reportDate,
        workUnitUuids: selectedWorkUnits.value.map((workUnit) => workUnit.workUnitUuid),
        content: formState.workSummary.trim(),
        note: formState.issueNote.trim()
      }
      await createDailyReport(payload)
      showToast('일일보고를 저장했습니다.', { variant: 'success' })
    }

    await navigateTo('/work/daily-reports')
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '일일보고 저장에 실패했습니다.', { variant: 'error' })
  } finally {
    isSubmitting.value = false
  }
}

await initializeWorkspace()
</script>

<template>
  <main class="page-container daily-report-workspace-page">
    <section class="content-panel daily-report-workspace-page__hero">
      <div>
        <p class="daily-report-workspace-page__eyebrow">Daily Report</p>
        <h1 class="section-title">{{ isEditing ? '일일보고 수정' : '일일보고 작성' }}</h1>
        <p class="section-description">
          왼쪽에서 이전 일일보고를 참고하고, 오른쪽에서 현재 보고를 작성합니다.
        </p>
      </div>

      <div class="daily-report-workspace-page__hero-actions">
        <CommonBaseButton variant="secondary" to="/work/daily-reports">
          목록으로
        </CommonBaseButton>
      </div>
    </section>

    <section v-if="isPageLoading" class="content-panel daily-report-workspace-page__loading">
      불러오는 중입니다.
    </section>

    <section v-else class="daily-report-workspace-page__layout">
      <WorkDailyReportHistorySidebar
        :reports="historyReports"
        :selected-report-uuid="historyPreviewReportUuid"
        :preview-report="historyPreviewReport"
        :is-loading="isHistoryLoading"
        @select-report="historyPreviewReportUuid = $event"
        @use-as-draft="applyHistoryDraft"
      />

      <section class="daily-report-workspace-page__editor-shell">
        <WorkReportEditorPanel
          :report-date="formState.reportDate"
          :selected-work-units="selectedWorkUnits"
          :work-summary="formState.workSummary"
          :issue-note="formState.issueNote"
          :is-editing="isEditing"
          :selected-commit-summary="selectedCommitSummary"
          @update:report-date="formState.reportDate = $event"
          @update:work-summary="formState.workSummary = $event"
          @update:issue-note="formState.issueNote = $event"
          @open-work-import="openImportModal"
        />

        <div class="daily-report-workspace-page__actions">
          <div class="daily-report-workspace-page__selected-summary">
            <strong>{{ selectedWorkUnitSummary }}</strong>
            <span>{{ selectedCommitSummary }}</span>
          </div>

          <div class="daily-report-workspace-page__action-buttons">
            <CommonBaseButton variant="secondary" :disabled="isSubmitting" to="/work/daily-reports">
              취소
            </CommonBaseButton>
            <CommonBaseButton :disabled="isSubmitting" @click="handleSubmit">
              {{ isEditing ? '수정 저장' : '작성 완료' }}
            </CommonBaseButton>
          </div>
        </div>
      </section>
    </section>

    <CommonBaseModal
      :visible="isImportModalVisible"
      eyebrow="Work Import"
      title="업무불러오기"
      width="min(1180px, 94vw)"
      @close="closeImportModal"
    >
      <div class="daily-report-workspace-page__import-layout">
        <section class="daily-report-workspace-page__import-panel daily-report-workspace-page__import-panel--work-units">
          <div class="daily-report-workspace-page__import-header">
            <h3>업무 선택</h3>
            <input
              v-model="workUnitSearchKeyword"
              class="input-field"
              type="search"
              placeholder="업무명 검색"
            >
          </div>

          <div v-if="isWorkUnitLoading" class="daily-report-workspace-page__empty">
            업무 목록을 불러오는 중입니다.
          </div>

          <div v-else-if="filteredWorkUnitOptions.length" class="daily-report-workspace-page__import-list">
            <label
              v-for="workUnit in filteredWorkUnitOptions"
              :key="workUnit.workUnitUuid"
              class="daily-report-workspace-page__import-item"
              :class="{ 'daily-report-workspace-page__import-item--active': modalActiveWorkUnitUuid === workUnit.workUnitUuid }"
            >
              <input
                type="checkbox"
                :checked="modalSelectedWorkUnitUuids.includes(workUnit.workUnitUuid)"
                @change="toggleWorkUnitSelection(workUnit.workUnitUuid, ($event.target as HTMLInputElement).checked)"
              >
              <button
                type="button"
                class="daily-report-workspace-page__import-item-button"
                @click="selectModalWorkUnit(workUnit.workUnitUuid)"
              >
                <strong>{{ workUnit.title }}</strong>
                <span>{{ workUnit.category || '카테고리 없음' }}</span>
              </button>
            </label>
          </div>

          <p v-else class="daily-report-workspace-page__empty">
            검색 조건에 맞는 업무가 없습니다.
          </p>
        </section>

        <section class="daily-report-workspace-page__import-panel">
          <div class="daily-report-workspace-page__import-header">
            <div>
              <h3>커밋 선택</h3>
              <p class="daily-report-workspace-page__import-meta">
                {{ modalSelectedWorkUnit ? `${modalSelectedWorkUnit.title} · ${modalCommitQuerySummary}` : '업무를 먼저 선택하세요.' }}
              </p>
            </div>
            <CommonBaseButton
              v-if="modalGitCommits.length"
              variant="secondary"
              size="small"
              type="button"
              :disabled="isCommitLoading"
              @click="toggleAllCommitSelections"
            >
              {{ isAllModalCommitsSelected ? '전체해제' : '전체선택' }}
            </CommonBaseButton>
          </div>

          <div v-if="!modalSelectedWorkUnit" class="daily-report-workspace-page__empty">
            업무를 선택하면 작성일 기준 커밋을 조회합니다.
          </div>

          <div v-else-if="!modalSelectedWorkUnit.gitProjects.length" class="daily-report-workspace-page__empty">
            연결된 프로젝트가 없습니다. 적용 시 업무명만 헤더로 세팅됩니다.
          </div>

          <div v-else-if="isCommitLoading" class="daily-report-workspace-page__empty">
            커밋 목록을 불러오는 중입니다.
          </div>

          <div v-else-if="modalGitCommits.length" class="daily-report-workspace-page__commit-list">
            <label
              v-for="commit in modalGitCommits"
              :key="`${commit.gitConnectionUuid}-${commit.commitSha}`"
              class="daily-report-workspace-page__commit-item"
            >
              <input
                type="checkbox"
                :checked="modalSelectedCommitShas.includes(commit.commitSha)"
                @change="toggleCommitSelection(commit.commitSha, ($event.target as HTMLInputElement).checked)"
              >
              <div class="daily-report-workspace-page__commit-item-content">
                <strong class="daily-report-workspace-page__commit-message">{{ commit.message.split('\n')[0] || commit.commitSha.slice(0, 7) }}</strong>
                <span class="daily-report-workspace-page__commit-meta">{{ commit.repositoryName }} · {{ commit.authorName }} · {{ commit.authoredAt.slice(0, 16).replace('T', ' ') }}</span>
              </div>
            </label>
          </div>

          <p v-else class="daily-report-workspace-page__empty">
            작성일 기준으로 조회된 커밋이 없습니다. 적용 시 업무명만 헤더로 세팅됩니다.
          </p>
        </section>
      </div>

      <template #actions>
        <CommonBaseButton variant="secondary" type="button" @click="closeImportModal">
          닫기
        </CommonBaseButton>
        <CommonBaseButton type="button" :disabled="!canApplyImport" @click="applyWorkImport">
          적용
        </CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.daily-report-workspace-page {
  display: grid;
  gap: 24px;
}

.daily-report-workspace-page__hero,
.daily-report-workspace-page__loading {
  padding: 22px;
}

.daily-report-workspace-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.daily-report-workspace-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.daily-report-workspace-page__layout {
  display: grid;
  grid-template-columns: minmax(280px, 3fr) minmax(0, 7fr);
  gap: 20px;
  align-items: start;
}

.daily-report-workspace-page__editor-shell {
  display: grid;
  grid-template-rows: auto auto;
  gap: 16px;
  min-height: 0;
}

.daily-report-workspace-page__actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: var(--radius-large);
  background: rgba(255, 255, 255, 0.04);
}

.daily-report-workspace-page__selected-summary {
  display: grid;
  gap: 4px;
}

.daily-report-workspace-page__selected-summary span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.daily-report-workspace-page__action-buttons {
  display: flex;
  gap: 10px;
}

.daily-report-workspace-page__import-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 520px;
  align-items: start;
}

.daily-report-workspace-page__import-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  min-height: 0;
  padding: 16px;
  border-radius: var(--radius-medium);
  border: 1px solid rgba(147, 210, 255, 0.12);
  background: rgba(255, 255, 255, 0.03);
}

.daily-report-workspace-page__import-panel--work-units {
  width: 280px;
  max-width: 280px;
}

.daily-report-workspace-page__import-header {
  display: grid;
  gap: 10px;
}

.daily-report-workspace-page__import-header h3 {
  margin: 0;
}

.daily-report-workspace-page__import-meta,
.daily-report-workspace-page__empty {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.daily-report-workspace-page__import-list,
.daily-report-workspace-page__commit-list {
  display: grid;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
}

.daily-report-workspace-page__import-item,
.daily-report-workspace-page__commit-item {
  display: grid;
  gap: 6px;
  padding: 8px 10px;
  border-radius: var(--radius-medium);
  border: 1px solid rgba(147, 210, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text);
  text-align: left;
  font: inherit;
}

.daily-report-workspace-page__import-item {
  grid-template-columns: 20px minmax(0, 1fr);
  align-items: center;
}

.daily-report-workspace-page__import-item--active {
  border-color: rgba(110, 193, 255, 0.52);
  background: rgba(110, 193, 255, 0.08);
}

.daily-report-workspace-page__import-item-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 22px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  font: inherit;
}

.daily-report-workspace-page__import-item span,
.daily-report-workspace-page__commit-item span {
  color: var(--color-text-muted);
  font-size: 0.85rem;
  line-height: 1.5;
}

.daily-report-workspace-page__commit-item-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.daily-report-workspace-page__commit-message {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.daily-report-workspace-page__commit-meta {
  text-align: right;
  white-space: nowrap;
}

.daily-report-workspace-page__import-item strong,
.daily-report-workspace-page__import-item span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.daily-report-workspace-page__import-item span {
  flex-shrink: 0;
  max-width: 92px;
  line-height: 1.2;
}

.daily-report-workspace-page__commit-item {
  grid-template-columns: 20px minmax(0, 1fr);
  align-items: flex-start;
}

@media (max-width: 1100px) {
  .daily-report-workspace-page__layout,
  .daily-report-workspace-page__import-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .daily-report-workspace-page__hero,
  .daily-report-workspace-page__actions {
    display: grid;
  }

  .daily-report-workspace-page__action-buttons {
    justify-content: stretch;
  }

  .daily-report-workspace-page__commit-item-content {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .daily-report-workspace-page__commit-meta {
    text-align: left;
  }
}
</style>
