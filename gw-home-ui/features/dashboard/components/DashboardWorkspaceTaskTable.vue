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

    <CommonBaseState v-if="errorMessage" state="error" title="업무 현황을 불러오지 못했습니다." :message="errorMessage" />
    <CommonBaseState v-else-if="isLoading" state="loading" title="업무 현황을 불러오는 중입니다." message="최근 업무와 TODO 진행률을 정리하고 있습니다." />

    <div v-else-if="rows.length" class="table-scroll">
      <table class="dashboard-task-table__table" aria-label="업무 현황">
        <thead>
          <tr>
            <th>업무</th>
            <th>상태</th>
            <th>진행률</th>
            <th>지연</th>
            <th>최근 수정</th>
            <th aria-label="바로가기" />
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.workUnitUuid">
            <td class="dashboard-task-table__title">
              <div class="dashboard-task-table__task">
                <strong>{{ row.title }}</strong>
                <span>{{ row.category || '분류 없음' }}</span>
              </div>
            </td>
            <td data-label="상태">
              <span class="dashboard-task-table__status" :class="`dashboard-task-table__status--${row.statusTone}`">
                {{ row.statusLabel }}
              </span>
            </td>
            <td data-label="진행률">
              <div class="dashboard-task-table__progress">
                <div class="dashboard-task-table__progress-bar">
                  <span :style="{ width: `${row.progressRate}%` }" />
                </div>
                <strong>{{ row.progressRate }}%</strong>
              </div>
            </td>
            <td data-label="지연">
              <span
                class="dashboard-task-table__delay"
                :class="{ 'dashboard-task-table__delay--danger': row.delayedCount > 0 }"
              >
                {{ row.delayedCount }}건
              </span>
            </td>
            <td data-label="최근 수정">{{ formatDate(row.updatedAt) }}</td>
            <td class="dashboard-task-table__action">
              <CommonBaseButton size="small" variant="secondary" :to="`/work/todos/${row.workUnitUuid}`">
                열기
              </CommonBaseButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <CommonBaseState v-else state="empty" title="조건에 맞는 업무가 없습니다." message="필터를 바꾸거나 업무관리 화면에서 새 업무를 등록해 주세요." />
  </section>
</template>

<style scoped>
.dashboard-task-table {
  padding: var(--panel-padding);
  display: grid;
  gap: 18px;
}

.dashboard-task-table__header {
  display: flex;
  justify-content: space-between;
  gap: var(--section-gap);
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

.dashboard-task-table__table {
  width: 100%;
  border-collapse: collapse;
  min-width: 780px;
}

.dashboard-task-table__table th,
.dashboard-task-table__table td {
  padding: 14px 12px;
  border-bottom: 1px solid var(--color-border);
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
  overflow-wrap: anywhere;
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
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.dashboard-task-table__status--success {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.dashboard-task-table__status--warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.dashboard-task-table__status--danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.dashboard-task-table__progress {
  display: grid;
  gap: 8px;
}

.dashboard-task-table__progress-bar {
  width: min(180px, 100%);
  height: 8px;
  border-radius: 999px;
  background: var(--color-surface-strong);
  overflow: hidden;
}

.dashboard-task-table__progress-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary);
}

.dashboard-task-table__delay {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.dashboard-task-table__delay--danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.dashboard-task-table__action {
  text-align: right;
}

.dashboard-task-table__action :deep(.base-button) {
  white-space: nowrap;
}

@media (max-width: 768px) {
  .dashboard-task-table {
    padding: var(--mobile-panel-padding);
    gap: 14px;
  }

  .dashboard-task-table__header {
    flex-direction: column;
    gap: 12px;
  }

  .dashboard-task-table__header :deep(.base-button) {
    width: 100%;
  }

  .dashboard-task-table__table {
    display: block;
    min-width: 0;
  }
  .dashboard-task-table__table thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
  }
  .dashboard-task-table__table tbody { display: grid; }
  .dashboard-task-table__table tr {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    padding: 16px 0;
    border: 0;
    border-radius: 0;
  }
  .dashboard-task-table__table tr + tr { border-top: 1px solid var(--color-border); }
  .dashboard-task-table__table td {
    display: grid;
    gap: 6px;
    min-width: 0;
    padding: 0;
    border: 0;
    align-content: start;
  }
  .dashboard-task-table__table td[data-label]::before { content: attr(data-label); color: var(--color-text-muted); font-size: 0.8rem; }
  .dashboard-task-table__title, .dashboard-task-table__action { grid-column: 1 / -1; }
  .dashboard-task-table__status, .dashboard-task-table__delay { justify-self: start; }
}
</style>
