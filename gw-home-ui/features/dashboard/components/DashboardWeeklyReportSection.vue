<script setup lang="ts">
import type { WeeklyReport } from '~/features/work/types/work.types'
import { formatDate } from '~/utils/date'

defineProps<{
  reports: WeeklyReport[]
  errorMessage: string
  getReportExcerpt: (content: string | null | undefined) => string
}>()
</script>

<template>
  <article class="dashboard-section content-panel">
    <div class="dashboard-section__header">
      <div>
        <h2>주간보고</h2>
        <p class="message-muted">최근 작성 내역</p>
      </div>
      <CommonBaseButton variant="secondary" to="/work/weekly-reports">
        이동
      </CommonBaseButton>
    </div>

    <p v-if="errorMessage" class="message-error">
      {{ errorMessage }}
    </p>
    <div v-else-if="reports.length" class="dashboard-report-list">
      <article
        v-for="weeklyReport in reports"
        :key="weeklyReport.uuid"
        class="dashboard-report-list__card"
      >
        <div class="dashboard-report-list__header">
          <strong>{{ weeklyReport.title }}</strong>
          <span>{{ formatDate(weeklyReport.weekEndDate, { month: 'numeric', day: 'numeric' }) }}</span>
        </div>
        <p>{{ getReportExcerpt(weeklyReport.content) }}</p>
      </article>
    </div>
    <p v-else class="message-muted">
      주간보고가 없습니다.
    </p>
  </article>
</template>

<style scoped>
.dashboard-section {
  padding: 24px;
  background: linear-gradient(180deg, rgba(11, 24, 46, 0.54) 0%, rgba(9, 20, 38, 0.42) 100%);
  border-color: rgba(176, 210, 255, 0.14);
  box-shadow:
    0 16px 34px rgba(3, 12, 28, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18px) saturate(125%);
  -webkit-backdrop-filter: blur(18px) saturate(125%);
}

.dashboard-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-section__header h2 {
  margin: 0;
  font-size: 1.05rem;
}

.dashboard-section__header p {
  margin: 6px 0 0;
}

.dashboard-report-list {
  display: grid;
  gap: 12px;
}

.dashboard-report-list__card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(166, 214, 255, 0.1);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.045) 0%, rgba(255, 255, 255, 0.018) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.dashboard-report-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dashboard-report-list__card strong {
  font-size: 0.96rem;
  line-height: 1.4;
}

.dashboard-report-list__card span,
.dashboard-report-list__card p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .dashboard-section__header,
  .dashboard-report-list__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
