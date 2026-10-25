<script setup lang="ts">
import WorkInsightsFilters from './WorkInsightsFilters.vue'
import WorkStatisticsView from './WorkStatisticsView.vue'
import { useWorkInsights } from '../composables/use-work-insights'
const { filters, statistics, workUnits, loading, errorMessage, validationMessage, optionsError, refresh, loadOptions, applyFilters, preset } = useWorkInsights('statistics')
</script>

<template>
  <main class="page-container page-stack work-insights">
    <CommonBasePageHeader title="완료 통계" eyebrow="Work Insights" description="완료한 일의 흐름과 지금의 진행 상황을 살펴보세요.">
      <template #actions>
        <CommonBaseButton variant="secondary" to="/work/todos">내 할 일</CommonBaseButton>
        <CommonBaseButton variant="secondary" to="/work/gantt">간트차트</CommonBaseButton>
      </template>
    </CommonBasePageHeader>
    <WorkInsightsFilters v-model="filters" mode="statistics" :work-units="workUnits" :loading="loading" :validation-message="validationMessage" @apply="applyFilters" @preset="preset" />
    <CommonBaseState v-if="optionsError" state="error" :message="optionsError" action-label="업무 목록 다시 조회" @action="loadOptions" />
    <CommonBaseState v-if="loading" state="loading" message="완료 현황을 불러오는 중입니다." />
    <CommonBaseState v-else-if="errorMessage" state="error" :message="errorMessage" action-label="다시 조회" @action="refresh" />
    <template v-else-if="statistics">
      <p class="insight-note" role="status">완료 기간 {{ statistics.period.fromDate }} ~ {{ statistics.period.toDate }} · {{ statistics.period.timeZone }} · {{ statistics.period.includeInactive ? '비활성 업무 포함' : '활성 업무만' }} · 현재 현황 기준 {{ statistics.period.asOfDate }}</p>
      <WorkStatisticsView :data="statistics" />
    </template>
  </main>
</template>
