<script setup lang="ts">
import SearchableSelect from '~/components/common/SearchableSelect.vue'
import WeeklyReportFormModal from './WeeklyReportFormModal.vue'
import type { WeeklyReport } from '../types/work.types'
import { useWeeklyReportList } from '../composables/use-weekly-report-list'

const {
  closeComposerModal,
  editingWeeklyReportUuid,
  formatDate,
  generationTypeLabelMap,
  handleComposerSaved,
  isComposerVisible,
  isLoadingWeeklyReports,
  openCreateModal,
  openEditModal,
  summary,
  weeklyReports
} = useWeeklyReportList()

const selectedYear = ref('')
const selectedMonth = ref('')
const selectedOpenStatus = ref('')

const yearOptions = computed(() => {
  return Array.from(new Set(
    weeklyReports.value
      .map((weeklyReport: WeeklyReport) => weeklyReport.weekStartDate.slice(0, 4))
      .filter(Boolean)
  ))
    .sort((left, right) => Number(right) - Number(left))
    .map((year) => ({
      value: year,
      label: `${year}년`
    }))
})

const monthOptions = Array.from({ length: 12 }, (_, index) => {
  const month = String(index + 1).padStart(2, '0')
  return {
    value: month,
    label: `${index + 1}월`
  }
})

const filteredWeeklyReports = computed<WeeklyReport[]>(() => {
  return weeklyReports.value.filter((weeklyReport: WeeklyReport) => {
    const matchesYear = !selectedYear.value || weeklyReport.weekStartDate.slice(0, 4) === selectedYear.value
    const matchesMonth = !selectedMonth.value || weeklyReport.weekStartDate.slice(5, 7) === selectedMonth.value
    const matchesOpenStatus = !selectedOpenStatus.value || weeklyReport.openYn === selectedOpenStatus.value

    return matchesYear && matchesMonth && matchesOpenStatus
  })
})

function getGenerationTypeLabel(weeklyReport: WeeklyReport): string {
  return generationTypeLabelMap[weeklyReport.generationType]
}

const openStatusOptions = [
  { value: '', label: '전체' },
  { value: 'Y', label: '오픈' },
  { value: 'N', label: '비공개' }
]
</script>

<template>
  <main class="page-container weekly-report-page">
    <section class="content-panel weekly-report-page__hero">
      <div class="weekly-report-page__hero-header">
        <div class="weekly-report-page__hero-copy">
          <h1 class="section-title">내주간보고</h1>
          <p class="weekly-report-page__description">
            같은 주의 일일보고를 옆에서 보며 수동으로 정리하거나, 필요하면 OpenAI 초안을 먼저 받아 편집할 수 있습니다.
          </p>
        </div>

        <div class="weekly-report-page__hero-side">
          <div class="weekly-report-page__summary">
            <div>
              <span>저장된 보고</span>
              <strong>{{ summary.total }}</strong>
            </div>
            <div>
              <span>오픈 보고</span>
              <strong>{{ summary.open }}</strong>
            </div>
          </div>

          <div class="weekly-report-page__hero-actions">
            <CommonBaseButton variant="secondary" to="/weekly-reports">
              주간보고 보기
            </CommonBaseButton>
            <CommonBaseButton @click="openCreateModal">
              새 주간보고
            </CommonBaseButton>
          </div>
        </div>
      </div>
    </section>

    <section class="content-panel weekly-report-page__saved">
      <p v-if="isLoadingWeeklyReports" class="message-muted">
        불러오는 중...
      </p>

      <div v-else-if="weeklyReports.length" class="weekly-report-page__filter-row">
        <label class="weekly-report-page__filter-field">
          <span>연도</span>
          <SearchableSelect
            :options="[{ value: '', label: '전체' }, ...yearOptions]"
            :model-value="selectedYear"
            placeholder="연도 선택"
            input-class="weekly-report-page__filter-select"
            @update:model-value="selectedYear = $event"
          />
        </label>

        <label class="weekly-report-page__filter-field">
          <span>월</span>
          <SearchableSelect
            :options="[{ value: '', label: '전체' }, ...monthOptions]"
            :model-value="selectedMonth"
            placeholder="월 선택"
            input-class="weekly-report-page__filter-select"
            @update:model-value="selectedMonth = $event"
          />
        </label>

        <label class="weekly-report-page__filter-field">
          <span>오픈 여부</span>
          <SearchableSelect
            :options="openStatusOptions"
            :model-value="selectedOpenStatus"
            placeholder="오픈 여부 선택"
            input-class="weekly-report-page__filter-select"
            @update:model-value="selectedOpenStatus = $event"
          />
        </label>
      </div>

      <div v-if="filteredWeeklyReports.length" class="weekly-report-page__saved-list">
        <article v-for="weeklyReport in filteredWeeklyReports" :key="weeklyReport.uuid" class="weekly-report-page__saved-card">
          <div>
            <strong>{{ weeklyReport.title }}</strong>
            <p>{{ formatDate(weeklyReport.weekStartDate) }} ~ {{ formatDate(weeklyReport.weekEndDate) }}</p>
            <p>공개 여부: {{ weeklyReport.openYn === 'Y' ? '오픈' : '비공개' }}</p>
          </div>
          <div class="weekly-report-page__saved-actions">
            <span>{{ getGenerationTypeLabel(weeklyReport) }}</span>
            <CommonBaseButton variant="secondary" size="small" @click="openEditModal(weeklyReport)">
              불러오기
            </CommonBaseButton>
          </div>
        </article>
      </div>
      <p v-else-if="weeklyReports.length" class="weekly-report-page__empty-text">
        검색 조건에 맞는 주간보고가 없습니다.
      </p>
      <p v-else class="weekly-report-page__empty-text">
        저장된 주간보고가 없습니다.
      </p>
    </section>

    <WeeklyReportFormModal
      :visible="isComposerVisible"
      :weekly-report-uuid="editingWeeklyReportUuid"
      @close="closeComposerModal"
      @saved="handleComposerSaved"
    />
  </main>
</template>

<style scoped>
.weekly-report-page {
  display: grid;
  gap: 24px;
}

.weekly-report-page__hero,
.weekly-report-page__saved {
  padding: 22px;
}

.weekly-report-page__hero {
  padding: 16px 18px;
}

.weekly-report-page__hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.weekly-report-page__hero-side {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.weekly-report-page__hero-copy {
  display: grid;
  gap: 4px;
}

.weekly-report-page__hero-copy :deep(.section-title) {
  font-size: 1.2rem;
}

.weekly-report-page__description {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.45;
}

.weekly-report-page__hero-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.weekly-report-page__saved {
  display: grid;
  gap: 18px;
}

.weekly-report-page__filter-row {
  display: flex;
  gap: 12px;
  align-items: end;
  flex-wrap: wrap;
}

.weekly-report-page__filter-field {
  display: grid;
  gap: 8px;
  min-width: 180px;
}

.weekly-report-page__filter-field span {
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.weekly-report-page__filter-select {
  min-height: 44px;
}

.weekly-report-page__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.weekly-report-page__summary div {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
}

.weekly-report-page__summary span {
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.weekly-report-page__summary strong {
  font-size: 0.96rem;
}

.weekly-report-page__saved-list {
  display: grid;
  gap: 14px;
}

.weekly-report-page__saved-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(147, 210, 255, 0.16);
}

.weekly-report-page__saved-actions {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
}

.weekly-report-page__saved-card p,
.weekly-report-page__empty-text {
  margin: 0;
  color: var(--color-text-muted);
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .weekly-report-page__hero-header,
  .weekly-report-page__saved-card {
    flex-direction: column;
    align-items: stretch;
  }

  .weekly-report-page__hero-side {
    justify-items: stretch;
  }

  .weekly-report-page__hero-actions,
  .weekly-report-page__summary {
    justify-content: flex-start;
  }

  .weekly-report-page__filter-row {
    align-items: stretch;
  }

  .weekly-report-page__saved-list {
    min-width: 0;
  }
}
</style>
