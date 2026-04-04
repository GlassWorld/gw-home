<script setup lang="ts">
import { useAdminDailyReportManagement } from '~/features/admin/composables/use-admin-daily-report-management'

definePageMeta({
  middleware: 'admin'
})

const {
  filters,
  dailyReportPage,
  missingMembers,
  isLoading,
  formatDate,
  resetFilters,
  reloadAll,
  handleSearch,
  movePage
} = useAdminDailyReportManagement()

await reloadAll()
</script>

<template>
  <main class="page-container admin-daily-report-page">
    <section class="content-panel admin-daily-report-page__panel page-panel-padding-md">
      <div class="admin-daily-report-page__header">
        <div>
          <p class="admin-daily-report-page__eyebrow">Admin</p>
          <h1 class="section-title">일일보고 관리</h1>
        </div>
      </div>

      <form class="admin-daily-report-page__filters" @submit.prevent="handleSearch">
        <label>
          <span>시작일</span>
          <input v-model="filters.dateFrom" class="input-field" type="date">
        </label>
        <label>
          <span>종료일</span>
          <input v-model="filters.dateTo" class="input-field" type="date">
        </label>
        <label>
          <span>검색</span>
          <input v-model="filters.keyword" class="input-field" type="search" placeholder="업무명, 로그인 ID, 닉네임">
        </label>
        <div class="admin-daily-report-page__filter-actions">
          <CommonBaseButton variant="secondary" type="button" @click="resetFilters(); void handleSearch()">
            초기화
          </CommonBaseButton>
          <CommonBaseButton type="submit">
            조회
          </CommonBaseButton>
        </div>
      </form>
    </section>

    <section class="content-panel admin-daily-report-page__panel page-panel-padding-md">
      <div class="admin-daily-report-page__section-header">
        <h2>누락 현황</h2>
        <strong>{{ missingMembers.length }}명</strong>
      </div>

      <div class="admin-daily-report-page__missing-list">
        <article v-for="missingMember in missingMembers" :key="missingMember.memberUuid" class="admin-daily-report-page__missing-card">
          <div>
            <strong>{{ missingMember.nickname || missingMember.loginId }}</strong>
            <p>{{ missingMember.loginId }}</p>
          </div>
          <div>
            <strong>{{ missingMember.missingCount }}일 누락</strong>
            <p>최근 작성일: {{ formatDate(missingMember.lastWrittenDate) }}</p>
          </div>
          <p class="admin-daily-report-page__missing-dates">
            {{ missingMember.missingDates.length ? missingMember.missingDates.map((missingDate) => formatDate(missingDate)).join(', ') : '누락 없음' }}
          </p>
        </article>
      </div>
    </section>

    <section class="content-panel admin-daily-report-page__panel page-panel-padding-md">
      <div class="admin-daily-report-page__section-header">
        <h2>전체 일일보고</h2>
        <span v-if="isLoading">불러오는 중...</span>
      </div>

      <div class="admin-daily-report-page__table-wrap">
        <table class="admin-daily-report-page__table">
          <thead>
            <tr>
              <th>작성자</th>
              <th>작성일자</th>
              <th>선택 업무</th>
              <th>특이사항</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="dailyReport in dailyReportPage.content" :key="dailyReport.uuid">
              <td>
                <strong>{{ dailyReport.nickname || dailyReport.loginId }}</strong>
                <p>{{ dailyReport.loginId }}</p>
              </td>
              <td>{{ formatDate(dailyReport.reportDate) }}</td>
              <td>{{ dailyReport.workUnits.length ? dailyReport.workUnits.map((workUnit) => workUnit.title).join(', ') : '-' }}</td>
              <td>{{ dailyReport.note || '-' }}</td>
            </tr>
            <tr v-if="!isLoading && dailyReportPage.content.length === 0">
              <td colspan="4" class="admin-daily-report-page__empty">
                조회된 보고가 없습니다.
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
  </main>
</template>

<style scoped>
.admin-daily-report-page {
  display: grid;
  gap: 24px;
}

.admin-daily-report-page__panel {
  display: grid;
  gap: 18px;
}

.admin-daily-report-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-daily-report-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-daily-report-page__filters {
  display: grid;
  grid-template-columns: minmax(160px, 0.9fr) minmax(160px, 0.9fr) minmax(260px, 1.4fr) auto;
  align-items: end;
  gap: 16px;
}

.admin-daily-report-page__filters label {
  display: grid;
  gap: 8px;
}

.admin-daily-report-page__filters label span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.admin-daily-report-page__filter-actions,
.admin-daily-report-page__section-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.admin-daily-report-page__missing-card {
  display: grid;
  gap: 10px;
  padding: 16px 18px;
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(147, 210, 255, 0.16);
}

.admin-daily-report-page__missing-card p,
.admin-daily-report-page__missing-dates {
  margin: 0;
  color: var(--color-text-muted);
}

.admin-daily-report-page__table {
  width: 100%;
  border-collapse: collapse;
}

.admin-daily-report-page__table th,
.admin-daily-report-page__table td {
  padding: 12px 10px;
  border-bottom: 1px solid rgba(143, 208, 255, 0.12);
  text-align: left;
  vertical-align: top;
}

.admin-daily-report-page__table td p {
  margin: 4px 0 0;
  color: var(--color-text-muted);
}

.admin-daily-report-page__missing-list {
  display: grid;
  gap: 14px;
}

.admin-daily-report-page__empty {
  text-align: center;
  color: var(--color-text-muted);
}

@media (max-width: 1024px) {
  .admin-daily-report-page__filters {
    grid-template-columns: 1fr;
  }
}
</style>
