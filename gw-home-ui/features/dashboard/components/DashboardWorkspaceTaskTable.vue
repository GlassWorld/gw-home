<script setup lang="ts">
import type { DashboardTaskRow } from '../types/dashboard-workspace'
import { formatDate } from '~/utils/date'

defineProps<{
  rows: DashboardTaskRow[]
  isLoading: boolean
  errorMessage: string
}>()
</script>

<template>
  <section class="content-panel dashboard-task-table">
    <div class="dashboard-task-table__header">
      <div>
        <p class="dashboard-task-table__eyebrow">Work Overview</p>
        <h2 class="section-title">업무 현황</h2>
        <p class="section-description">진행, 완료, 리스크를 한 번에 확인하고 바로 상세 화면으로 이동합니다.</p>
      </div>

      <CommonBaseButton variant="secondary" to="/work/todos">
        TODO 보드 열기
      </CommonBaseButton>
    </div>

    <p v-if="errorMessage" class="message-error">
      {{ errorMessage }}
    </p>

    <div v-if="isLoading" class="dashboard-task-table__empty">
      <strong>업무 현황을 불러오는 중입니다.</strong>
      <p>최근 업무와 TODO 진행률을 정리하고 있습니다.</p>
    </div>

    <div v-else-if="rows.length" class="dashboard-task-table__table-wrap">
      <table class="dashboard-task-table__table">
        <thead>
          <tr>
            <th>업무</th>
            <th>상태</th>
            <th>진행률</th>
            <th>지연</th>
            <th>최근 보고</th>
            <th>최근 수정</th>
            <th aria-label="바로가기" />
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.workUnitUuid">
            <td>
              <div class="dashboard-task-table__task">
                <strong>{{ row.title }}</strong>
                <span>{{ row.category || '분류 없음' }}</span>
              </div>
            </td>
            <td>
              <span class="dashboard-task-table__status" :class="`dashboard-task-table__status--${row.statusTone}`">
                {{ row.statusLabel }}
              </span>
            </td>
            <td>
              <div class="dashboard-task-table__progress">
                <div class="dashboard-task-table__progress-bar">
                  <span :style="{ width: `${row.progressRate}%` }" />
                </div>
                <strong>{{ row.progressRate }}%</strong>
              </div>
            </td>
            <td>
              <span
                class="dashboard-task-table__delay"
                :class="{ 'dashboard-task-table__delay--danger': row.delayedCount > 0 }"
              >
                {{ row.delayedCount }}건
              </span>
            </td>
            <td>{{ row.latestReportDate ? formatDate(row.latestReportDate) : '-' }}</td>
            <td>{{ formatDate(row.updatedAt) }}</td>
            <td class="dashboard-task-table__action">
              <CommonBaseButton size="small" variant="secondary" :to="`/work/todos/${row.workUnitUuid}`">
                열기
              </CommonBaseButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="dashboard-task-table__empty">
      <strong>조건에 맞는 업무가 없습니다.</strong>
      <p>필터를 바꾸거나 업무관리 화면에서 새 업무를 등록해 주세요.</p>
    </div>
  </section>
</template>

<style scoped>
.dashboard-task-table {
  padding: 24px;
  display: grid;
  gap: 18px;
}

.dashboard-task-table__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.dashboard-task-table__eyebrow {
  margin: 0 0 8px;
  color: var(--color-accent);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dashboard-task-table__table-wrap {
  overflow-x: auto;
}

.dashboard-task-table__table {
  width: 100%;
  border-collapse: collapse;
  min-width: 780px;
}

.dashboard-task-table__table th,
.dashboard-task-table__table td {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.12);
  text-align: left;
  vertical-align: middle;
}

.dashboard-task-table__table th {
  color: var(--color-text-muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.dashboard-task-table__task {
  display: grid;
  gap: 6px;
}

.dashboard-task-table__task strong {
  font-size: 0.97rem;
}

.dashboard-task-table__task span {
  color: var(--color-text-muted);
  font-size: 0.86rem;
}

.dashboard-task-table__status,
.dashboard-task-table__delay {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 68px;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
}

.dashboard-task-table__status--primary {
  background: rgba(110, 193, 255, 0.16);
  color: #9ad8ff;
}

.dashboard-task-table__status--success {
  background: rgba(84, 214, 160, 0.16);
  color: #8ef0be;
}

.dashboard-task-table__status--warning {
  background: rgba(255, 188, 92, 0.14);
  color: #ffd089;
}

.dashboard-task-table__status--danger {
  background: rgba(224, 92, 92, 0.14);
  color: #ff9b9b;
}

.dashboard-task-table__progress {
  display: grid;
  gap: 8px;
}

.dashboard-task-table__progress-bar {
  width: min(180px, 100%);
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.dashboard-task-table__progress-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4aaef2 0%, #63e3d6 100%);
}

.dashboard-task-table__delay {
  background: rgba(255, 255, 255, 0.06);
  color: var(--color-text-muted);
}

.dashboard-task-table__delay--danger {
  background: rgba(224, 92, 92, 0.16);
  color: #ffaeae;
}

.dashboard-task-table__action {
  text-align: right;
}

.dashboard-task-table__empty {
  display: grid;
  gap: 6px;
  padding: 18px;
  border: 1px dashed rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
}

.dashboard-task-table__empty strong,
.dashboard-task-table__empty p {
  margin: 0;
}

.dashboard-task-table__empty p {
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .dashboard-task-table {
    padding: 18px;
  }

  .dashboard-task-table__header {
    flex-direction: column;
  }
}
</style>
