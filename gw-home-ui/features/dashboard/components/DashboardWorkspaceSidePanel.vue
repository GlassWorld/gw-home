<script setup lang="ts">
import type {
  DashboardIssueItem,
  DashboardRecentReportItem,
  DashboardScheduleItem,
  DashboardUpdateItem
} from '../types/dashboard-workspace'

defineProps<{
  reports: DashboardRecentReportItem[]
  issues: DashboardIssueItem[]
  schedules: DashboardScheduleItem[]
  updates: DashboardUpdateItem[]
  reportErrorMessage: string
}>()

const emit = defineEmits<{
  openNotice: [noticeUuid: string]
}>()

function openUpdate(update: DashboardUpdateItem) {
  if (update.noticeUuid) {
    emit('openNotice', update.noticeUuid)
  }
}
</script>

<template>
  <div class="dashboard-side-panel">
    <section class="content-panel dashboard-side-panel__section">
      <div class="dashboard-side-panel__section-header">
        <div>
          <p class="dashboard-side-panel__eyebrow">Weekly Reports</p>
          <h2 class="section-title">최근 주간보고</h2>
        </div>
        <CommonBaseButton size="small" variant="secondary" to="/work/weekly-reports">
          전체 보기
        </CommonBaseButton>
      </div>

      <p v-if="reportErrorMessage" class="message-error">
        {{ reportErrorMessage }}
      </p>

      <div v-else-if="reports.length" class="dashboard-side-panel__list">
        <article v-for="report in reports" :key="report.uuid" class="dashboard-side-panel__item">
          <div class="dashboard-side-panel__item-head">
            <strong>{{ report.title }}</strong>
            <span>{{ report.weekLabel }}</span>
          </div>
          <p>{{ report.excerpt }}</p>
          <div class="dashboard-side-panel__item-meta">
            <span>{{ report.openLabel }}</span>
            <span>{{ report.publishedLabel }}</span>
          </div>
        </article>
      </div>

      <p v-else class="message-muted">
        최근 주간보고가 아직 없습니다.
      </p>
    </section>

    <section class="content-panel dashboard-side-panel__section">
      <div class="dashboard-side-panel__section-header">
        <div>
          <p class="dashboard-side-panel__eyebrow">Blockers</p>
          <h2 class="section-title">이슈 / Blocker</h2>
        </div>
      </div>

      <div v-if="issues.length" class="dashboard-side-panel__list">
        <article v-for="issue in issues" :key="issue.key" class="dashboard-side-panel__issue">
          <div class="dashboard-side-panel__issue-head">
            <strong>{{ issue.title }}</strong>
            <span :class="`dashboard-side-panel__badge dashboard-side-panel__badge--${issue.tone}`">{{ issue.badge }}</span>
          </div>
          <p>{{ issue.description }}</p>
          <div class="dashboard-side-panel__issue-foot">
            <span>{{ issue.meta }}</span>
            <NuxtLink :to="issue.to">확인하기</NuxtLink>
          </div>
        </article>
      </div>

      <p v-else class="message-muted">
        현재 강조할 blocker가 없습니다.
      </p>
    </section>

    <section class="content-panel dashboard-side-panel__section">
      <div class="dashboard-side-panel__section-header">
        <div>
          <p class="dashboard-side-panel__eyebrow">Upcoming</p>
          <h2 class="section-title">오늘 / 이번 주 일정</h2>
        </div>
      </div>

      <div v-if="schedules.length" class="dashboard-side-panel__list">
        <article v-for="schedule in schedules" :key="schedule.key" class="dashboard-side-panel__schedule">
          <div class="dashboard-side-panel__schedule-head">
            <strong>{{ schedule.title }}</strong>
            <span :class="`dashboard-side-panel__badge dashboard-side-panel__badge--${schedule.tone}`">{{ schedule.statusLabel }}</span>
          </div>
          <p>{{ schedule.workUnitTitle }}</p>
          <div class="dashboard-side-panel__issue-foot">
            <span>{{ schedule.dueDateLabel }}</span>
            <NuxtLink :to="schedule.to">열기</NuxtLink>
          </div>
        </article>
      </div>

      <p v-else class="message-muted">
        이번 주에 표시할 예정 일정이 없습니다.
      </p>
    </section>

    <section class="content-panel dashboard-side-panel__section">
      <div class="dashboard-side-panel__section-header">
        <div>
          <p class="dashboard-side-panel__eyebrow">Updates</p>
          <h2 class="section-title">최근 업데이트</h2>
        </div>
      </div>

      <div v-if="updates.length" class="dashboard-side-panel__list">
        <component
          :is="update.noticeUuid ? 'button' : 'div'"
          v-for="update in updates"
          :key="update.key"
          class="dashboard-side-panel__update"
          :class="{ 'dashboard-side-panel__update--button': Boolean(update.noticeUuid) }"
          type="button"
          @click="openUpdate(update)"
        >
          <div class="dashboard-side-panel__item-head">
            <strong>{{ update.title }}</strong>
            <span :class="`dashboard-side-panel__badge dashboard-side-panel__badge--${update.tone}`">{{ update.typeLabel }}</span>
          </div>
          <p>{{ update.meta }}</p>
          <NuxtLink v-if="update.to" :to="update.to" @click.stop>
            바로가기
          </NuxtLink>
        </component>
      </div>

      <p v-else class="message-muted">
        최근 업데이트가 없습니다.
      </p>
    </section>
  </div>
</template>

<style scoped>
.dashboard-side-panel {
  display: grid;
  gap: 18px;
}

.dashboard-side-panel__section {
  padding: 20px;
  display: grid;
  gap: 16px;
}

.dashboard-side-panel__section-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.dashboard-side-panel__eyebrow {
  margin: 0 0 8px;
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dashboard-side-panel__list {
  display: grid;
  gap: 12px;
}

.dashboard-side-panel__item,
.dashboard-side-panel__issue,
.dashboard-side-panel__schedule,
.dashboard-side-panel__update {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
}

.dashboard-side-panel__update {
  text-align: left;
  color: inherit;
}

.dashboard-side-panel__update--button {
  width: 100%;
  border: 1px solid rgba(147, 210, 255, 0.12);
  cursor: pointer;
}

.dashboard-side-panel__item-head,
.dashboard-side-panel__issue-head,
.dashboard-side-panel__schedule-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.dashboard-side-panel__item-head strong,
.dashboard-side-panel__issue-head strong,
.dashboard-side-panel__schedule-head strong {
  font-size: 0.94rem;
}

.dashboard-side-panel__item-head span,
.dashboard-side-panel__item p,
.dashboard-side-panel__issue p,
.dashboard-side-panel__schedule p,
.dashboard-side-panel__update p {
  color: var(--color-text-muted);
}

.dashboard-side-panel__item p,
.dashboard-side-panel__issue p,
.dashboard-side-panel__schedule p,
.dashboard-side-panel__update p {
  margin: 0;
  line-height: 1.5;
}

.dashboard-side-panel__item-meta,
.dashboard-side-panel__issue-foot {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.dashboard-side-panel__badge {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
}

.dashboard-side-panel__badge--primary {
  background: rgba(110, 193, 255, 0.16);
  color: #9ad8ff;
}

.dashboard-side-panel__badge--success {
  background: rgba(84, 214, 160, 0.16);
  color: #8ef0be;
}

.dashboard-side-panel__badge--warning {
  background: rgba(255, 188, 92, 0.14);
  color: #ffd089;
}

.dashboard-side-panel__badge--danger {
  background: rgba(224, 92, 92, 0.14);
  color: #ffabab;
}

.dashboard-side-panel__badge--neutral {
  background: rgba(255, 255, 255, 0.07);
  color: rgba(236, 246, 255, 0.84);
}

@media (max-width: 768px) {
  .dashboard-side-panel__section {
    padding: 18px;
  }

  .dashboard-side-panel__section-header {
    flex-direction: column;
  }
}
</style>
