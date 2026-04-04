<script setup lang="ts">
import type { OpenWeeklyReport, OpenWeeklyReportMember } from '../types/work.types'

const props = defineProps<{
  members: OpenWeeklyReportMember[]
  reports: OpenWeeklyReport[]
  selectedMemberUuid: string
  selectedYearMonth: string
  selectedWeekOfMonth: string
  availableYearMonths: string[]
  isLoadingMembers: boolean
  isLoadingReports: boolean
  formatDate: (value: string) => string
  resolveWeekOfMonth: (value: string) => number
}>()

const emit = defineEmits<{
  'update:selectedMemberUuid': [memberUuid: string]
  'update:selectedYearMonth': [value: string]
  'update:selectedWeekOfMonth': [value: string]
  open: [reportUuid: string]
}>()

const memberOptions = computed(() => {
  return props.members.map((member) => ({
    value: member.memberUuid,
    label: `${member.nickname || member.loginId} · ${member.openReportCount}건`
  }))
})

const yearMonthOptions = computed(() => {
  return props.availableYearMonths.map((value) => ({
    value,
    label: value.replace('-', '년 ') + '월'
  }))
})

const weekOptions = [
  { value: '', label: '전체 주차' },
  { value: '1', label: '1주차' },
  { value: '2', label: '2주차' },
  { value: '3', label: '3주차' },
  { value: '4', label: '4주차' },
  { value: '5', label: '5주차' }
]

function getAuthorLabel(report: OpenWeeklyReport): string {
  return report.nickname || report.loginId
}

function getExcerpt(content: string): string {
  const normalized = content.replace(/\s+/g, ' ').trim()
  if (normalized.length <= 120) {
    return normalized
  }

  return `${normalized.slice(0, 120)}...`
}
</script>

<template>
  <section class="content-panel weekly-report-open-section">
    <div class="weekly-report-open-section__header">
      <div>
        <h2>인별 주간보고 조회</h2>
        <p class="message-muted">공개된 주간보고만 조회할 수 있습니다.</p>
      </div>
      <span v-if="props.isLoadingMembers || props.isLoadingReports">불러오는 중...</span>
    </div>

    <div class="weekly-report-open-section__filters">
      <label class="weekly-report-open-section__filter">
        <span>조회할 사용자</span>
        <CommonSearchableSelect
          :options="memberOptions"
          :model-value="props.selectedMemberUuid"
          placeholder="사용자를 선택하세요"
          @update:model-value="emit('update:selectedMemberUuid', $event)"
        />
      </label>

      <label class="weekly-report-open-section__filter">
        <span>조회 월</span>
        <CommonSearchableSelect
          :options="yearMonthOptions"
          :model-value="props.selectedYearMonth"
          placeholder="월을 선택하세요"
          @update:model-value="emit('update:selectedYearMonth', $event)"
        />
      </label>

      <label class="weekly-report-open-section__filter">
        <span>주차</span>
        <CommonSearchableSelect
          :options="weekOptions"
          :model-value="props.selectedWeekOfMonth"
          placeholder="주차를 선택하세요"
          @update:model-value="emit('update:selectedWeekOfMonth', $event)"
        />
      </label>
    </div>

    <p v-if="!props.members.length" class="weekly-report-open-section__empty">
      공개된 주간보고가 있는 사용자가 없습니다.
    </p>

    <div v-else-if="props.reports.length" class="weekly-report-open-section__list">
      <article
        v-for="report in props.reports"
        :key="report.uuid"
        class="weekly-report-open-section__card"
      >
        <div class="weekly-report-open-section__card-body">
          <div class="weekly-report-open-section__meta">
            <strong>{{ getAuthorLabel(report) }}</strong>
            <span>{{ props.formatDate(report.weekStartDate) }} ~ {{ props.formatDate(report.weekEndDate) }}</span>
            <span>{{ props.resolveWeekOfMonth(report.weekStartDate) }}주차</span>
          </div>
          <strong class="weekly-report-open-section__title">{{ report.title }}</strong>
          <p class="weekly-report-open-section__excerpt">{{ getExcerpt(report.content) }}</p>
        </div>

        <div class="weekly-report-open-section__actions">
          <span>{{ report.generationType === 'OPENAI' ? 'OpenAI' : report.generationType === 'RULE_BASED' ? '초안' : '수동' }}</span>
          <CommonBaseButton variant="secondary" size="small" @click="emit('open', report.uuid)">
            상세보기
          </CommonBaseButton>
        </div>
      </article>
    </div>

    <p v-else class="weekly-report-open-section__empty">
      선택한 사용자에게 공개된 주간보고가 없습니다.
    </p>
  </section>
</template>

<style scoped>
.weekly-report-open-section {
  display: grid;
  gap: 18px;
  padding: 22px;
}

.weekly-report-open-section__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.weekly-report-open-section__header h2,
.weekly-report-open-section__filter span,
.weekly-report-open-section__title,
.weekly-report-open-section__meta strong {
  margin: 0;
}

.weekly-report-open-section__filter {
  display: grid;
  gap: 8px;
}

.weekly-report-open-section__filters {
  display: grid;
  grid-template-columns: minmax(240px, 2fr) repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.weekly-report-open-section__filter span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.weekly-report-open-section__list {
  display: grid;
  gap: 14px;
}

.weekly-report-open-section__card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(147, 210, 255, 0.16);
}

.weekly-report-open-section__card-body {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.weekly-report-open-section__meta {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.weekly-report-open-section__title {
  font-size: 1rem;
}

.weekly-report-open-section__excerpt,
.weekly-report-open-section__empty {
  margin: 0;
  color: var(--color-text-muted);
  white-space: pre-wrap;
}

.weekly-report-open-section__actions {
  display: grid;
  gap: 10px;
  justify-items: end;
  align-content: start;
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

@media (max-width: 768px) {
  .weekly-report-open-section__filters,
  .weekly-report-open-section__header,
  .weekly-report-open-section__card {
    display: grid;
    grid-template-columns: 1fr;
  }

  .weekly-report-open-section__actions {
    justify-items: stretch;
  }
}
</style>
