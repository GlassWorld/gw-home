<script setup lang="ts">
import WorkInsightsFilters from './WorkInsightsFilters.vue'
import WorkGanttView from './WorkGanttView.vue'
import { useWorkInsights } from '../composables/use-work-insights'
const { filters, gantt, workUnits, loading, errorMessage, validationMessage, optionsError, refresh, loadOptions, applyFilters, preset } = useWorkInsights('gantt')
const view = ref<'list' | 'chart'>('list')
onMounted(() => { view.value = window.matchMedia('(min-width: 768px)').matches ? 'chart' : 'list' })
</script>

<template>
  <main class="page-container page-stack work-insights">
    <CommonBasePageHeader title="간트차트" eyebrow="Work Schedule" description="업무의 흐름과 할 일 일정을 함께 확인하세요.">
      <template #actions>
        <CommonBaseButton variant="secondary" to="/work/todos">내 할 일</CommonBaseButton>
        <CommonBaseButton variant="secondary" to="/work/statistics">완료 통계</CommonBaseButton>
      </template>
    </CommonBasePageHeader>
    <WorkInsightsFilters v-model="filters" mode="gantt" :work-units="workUnits" :loading="loading" :validation-message="validationMessage" @apply="applyFilters" @preset="preset" />
    <CommonBaseState v-if="optionsError" state="error" :message="optionsError" action-label="업무 목록 다시 조회" @action="loadOptions" />
    <div class="insight-toolbar">
      <div class="insight-actions" aria-label="일정 보기 방식">
        <CommonBaseButton :variant="view === 'list' ? 'primary' : 'secondary'" :aria-pressed="view === 'list'" @click="view = 'list'">일정 목록</CommonBaseButton>
        <CommonBaseButton :variant="view === 'chart' ? 'primary' : 'secondary'" :aria-pressed="view === 'chart'" @click="view = 'chart'">간트 보기</CommonBaseButton>
      </div>
      <span class="insight-note">완료 · 지연 · 상위 진행률 / 점선 ◆ = 한쪽 날짜 미정</span>
    </div>
    <CommonBaseState v-if="loading" state="loading" message="일정을 불러오는 중입니다." />
    <CommonBaseState v-else-if="errorMessage" state="error" :message="errorMessage" action-label="다시 조회" @action="refresh" />
    <template v-else-if="gantt">
      <p class="insight-note" role="status">조회 {{ gantt.period.fromDate }} ~ {{ gantt.period.toDate }} · {{ gantt.period.timeZone }} · {{ gantt.period.includeInactive ? '비활성 업무 포함' : '활성 업무만' }} · 오늘 {{ gantt.period.asOfDate }}</p>
      <WorkGanttView v-if="gantt.rows.length" :data="gantt" :view="view" />
      <CommonBaseState v-else state="empty" title="표시할 일정이 없습니다." message="기간이나 업무 필터를 바꾸거나 내 할 일에서 일정을 등록해주세요." />
      <p class="insight-note">업무 일정·진행률은 해당 업무 전체 TODO 기준입니다. 상위 항목의 막대는 직접 지정한 일정이며, 기간 밖의 조상은 계층 표시로 남습니다. 항목을 선택하면 기존 상세 화면으로 이동합니다.</p>
    </template>
  </main>
</template>
