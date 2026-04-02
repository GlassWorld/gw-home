<script setup lang="ts">
import DailyReportDetailModal from './DailyReportDetailModal.vue'
import DailyReportFormModal from './DailyReportFormModal.vue'
import { useDailyReportList } from '../composables/use-daily-report-list'

const {
  closeDetailModal,
  closeFormModal,
  dailyReportPage,
  editingDailyReportUuid,
  extractSection,
  filters,
  formatDateTime,
  formatDateWithWeekday,
  formatMissingDateLabel,
  handleFormSaved,
  handleSearch,
  isDetailVisible,
  isFormVisible,
  isLoading,
  isMissingPanelVisible,
  movePage,
  openCreateModal,
  openDetailMode,
  openEditModal,
  resetFilters,
  selectedDetailReport,
  summary,
  visibleMissingDailyReports
} = useDailyReportList()
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
          <CommonBaseButton @click="openCreateModal">
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
        <label class="daily-report-page__search-field">
          <span>내용 검색</span>
          <input v-model="filters.keyword" class="input-field" type="search" placeholder="업무명 또는 특이사항">
        </label>
        <div class="daily-report-page__filter-actions">
          <CommonBaseButton variant="secondary" type="button" @click="resetFilters">
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
              <th>작업 업무</th>
              <th>특이사항</th>
              <th>수정일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="dailyReport in dailyReportPage.content" :key="dailyReport.uuid">
              <td>{{ formatDateWithWeekday(dailyReport.reportDate) }}</td>
              <td class="daily-report-page__content-cell">
                {{ dailyReport.workUnits.length ? dailyReport.workUnits.map((workUnit) => workUnit.title).join(', ') : '-' }}
              </td>
              <td class="daily-report-page__content-cell">
                {{ dailyReport.note || extractSection(dailyReport.content, '이슈 / 특이사항') || '-' }}
              </td>
              <td>{{ formatDateTime(dailyReport.updatedAt) }}</td>
              <td>
                <div class="daily-report-page__row-actions">
                  <CommonBaseButton variant="secondary" size="small" @click="openDetailMode(dailyReport)">
                    상세
                  </CommonBaseButton>
                  <CommonBaseButton variant="secondary" size="small" @click="openEditModal(dailyReport)">
                    수정
                  </CommonBaseButton>
                </div>
              </td>
            </tr>
            <tr v-if="!isLoading && dailyReportPage.content.length === 0">
              <td colspan="5" class="daily-report-page__empty">
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

    <DailyReportDetailModal
      :visible="isDetailVisible"
      :report="selectedDetailReport"
      @close="closeDetailModal"
      @edit="openEditModal"
    />

    <DailyReportFormModal
      :visible="isFormVisible"
      :daily-report-uuid="editingDailyReportUuid"
      @close="closeFormModal"
      @saved="handleFormSaved"
    />
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

.daily-report-page__filters {
  display: grid;
  grid-template-columns: minmax(150px, 0.9fr) minmax(150px, 0.9fr) minmax(260px, 1.6fr) auto;
  align-items: end;
  gap: 14px 16px;
}

.daily-report-page__filters label {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.daily-report-page__filters label span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.daily-report-page__filter-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  white-space: nowrap;
}

.daily-report-page__table-wrap {
  min-width: 0;
  overflow-x: auto;
}

.daily-report-page__table {
  width: 100%;
  border-collapse: collapse;
}

.daily-report-page__table th,
.daily-report-page__table td {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.08);
  text-align: left;
  vertical-align: top;
}

.daily-report-page__table th {
  color: var(--color-text-muted);
  font-size: 0.86rem;
}

.daily-report-page__content-cell {
  max-width: 280px;
  word-break: break-word;
  line-height: 1.5;
}

.daily-report-page__row-actions {
  display: flex;
  gap: 8px;
}

.daily-report-page__empty,
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
  gap: 14px;
}

.daily-report-page__floating-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.daily-report-page__floating-close {
  border: 0;
  background: transparent;
  color: var(--color-text-muted);
  font: inherit;
}

@media (max-width: 960px) {
  .daily-report-page__hero-header,
  .daily-report-page__filters {
    display: grid;
    grid-template-columns: 1fr;
  }

  .daily-report-page__hero-side {
    justify-items: start;
  }
}
</style>
