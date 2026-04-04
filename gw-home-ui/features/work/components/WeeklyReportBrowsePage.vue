<script setup lang="ts">
import WeeklyReportOpenBrowseSection from './WeeklyReportOpenBrowseSection.vue'
import WeeklyReportOpenDetailModal from './WeeklyReportOpenDetailModal.vue'
import { useOpenWeeklyReportBrowse } from '../composables/use-open-weekly-report-browse'

const {
  changeSelectedOpenMember,
  closeOpenWeeklyReportDetail,
  formatDate,
  isLoadingOpenMembers,
  isLoadingOpenWeeklyReportDetail,
  isLoadingOpenWeeklyReports,
  isOpenDetailVisible,
  openOpenWeeklyReportDetail,
  openWeeklyReportMembers,
  openWeeklyReports,
  selectedOpenMemberUuid,
  selectedOpenWeeklyReport
} = useOpenWeeklyReportBrowse()
</script>

<template>
  <main class="page-container weekly-report-browse-page">
    <section class="content-panel weekly-report-browse-page__hero">
      <div class="weekly-report-browse-page__hero-header">
        <div class="weekly-report-browse-page__hero-copy">
          <h1 class="section-title">주간보고</h1>
          <p class="weekly-report-browse-page__description">
            이번 주간보고 내용을 확인하거나, 사용자별로 공개된 주간보고를 찾아볼 수 있습니다.
          </p>
        </div>

        <div class="weekly-report-browse-page__hero-actions">
          <CommonBaseButton variant="secondary" to="/work/weekly-reports">
            내주간보고
          </CommonBaseButton>
        </div>
      </div>
    </section>

    <WeeklyReportOpenBrowseSection
      :members="openWeeklyReportMembers"
      :reports="openWeeklyReports"
      :selected-member-uuid="selectedOpenMemberUuid"
      :is-loading-members="isLoadingOpenMembers"
      :is-loading-reports="isLoadingOpenWeeklyReports"
      :format-date="formatDate"
      @update:selected-member-uuid="changeSelectedOpenMember"
      @open="openOpenWeeklyReportDetail"
    />

    <WeeklyReportOpenDetailModal
      :visible="isOpenDetailVisible"
      :report="selectedOpenWeeklyReport"
      :is-loading="isLoadingOpenWeeklyReportDetail"
      :format-date="formatDate"
      @close="closeOpenWeeklyReportDetail"
    />
  </main>
</template>

<style scoped>
.weekly-report-browse-page {
  display: grid;
  gap: 24px;
}

.weekly-report-browse-page__hero {
  padding: 16px 18px;
}

.weekly-report-browse-page__hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.weekly-report-browse-page__hero-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.weekly-report-browse-page__hero-copy {
  display: grid;
  gap: 4px;
}

.weekly-report-browse-page__hero-copy :deep(.section-title) {
  font-size: 1.2rem;
}

.weekly-report-browse-page__description {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  line-height: 1.45;
}

@media (max-width: 768px) {
  .weekly-report-browse-page__hero-header {
    flex-direction: column;
    align-items: stretch;
  }

  .weekly-report-browse-page__hero-actions {
    justify-content: flex-start;
  }
}
</style>
