<script setup lang="ts">
import WeeklyReportFormModal from '~/components/work/WeeklyReportFormModal.vue'
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
          <CommonBaseButton variant="secondary" @click="openCreateModal">
            새 주간보고
          </CommonBaseButton>

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
        </div>
      </div>
    </section>

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
            <CommonBaseButton variant="secondary" size="small" @click="openEditModal(weeklyReport)">
              불러오기
            </CommonBaseButton>
          </div>
        </article>
      </div>
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

.weekly-report-page__section-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
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
  .weekly-report-page__saved-card,
  .weekly-report-page__section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .weekly-report-page__hero-side {
    justify-items: stretch;
  }

  .weekly-report-page__summary,
  .weekly-report-page__saved-list {
    min-width: 0;
  }
}
</style>
