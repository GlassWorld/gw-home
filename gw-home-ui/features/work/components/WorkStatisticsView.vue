<script setup lang="ts">
import type { WorkStatistics } from '../types/work-insights.types'
import { workUnitStatusLabels } from '../utils/work-insights'
import WorkCompletionChart from './WorkCompletionChart.vue'
defineProps<{ data: WorkStatistics }>()
</script>

<template>
  <div class="page-stack">
    <section class="insight-metrics" aria-label="완료와 현재 현황">
      <article class="content-panel insight-metric"><span>선택 기간 완료</span><strong>{{ data.periodCompletedCount }}<small>개</small></strong><p>완료 시각이 기록된 최하위 할 일</p></article>
      <article class="content-panel insight-metric"><span>현재 완료율</span><strong>{{ data.summary.progressRate }}<small>%</small></strong><p>{{ data.summary.completedCount }} / {{ data.summary.totalCount }}개 · 기간 제한 없음</p></article>
      <article class="content-panel insight-metric"><span>현재 미완료</span><strong>{{ data.summary.totalCount - data.summary.completedCount }}<small>개</small></strong><p>전체 최하위 할 일 중 미완료</p></article>
      <article class="content-panel insight-metric insight-metric--danger"><span>현재 지연</span><strong>{{ data.summary.overdueCount }}<small>개</small></strong><p>마감일이 지난 미완료 할 일</p></article>
    </section>
    <div class="insight-current-groups" aria-label="현재 일정 분류">
      <span>오늘 {{ data.summary.todayCount }}개</span><span>예정 {{ data.summary.upcomingCount }}개</span>
      <span>일정 미지정 {{ data.summary.unscheduledCount }}개</span><span>완료일 미상 {{ data.summary.unknownCompletionCount }}개</span>
    </div>
    <p v-if="data.summary.unknownCompletionCount" class="insight-warning" role="status">완료일 미상 {{ data.summary.unknownCompletionCount }}개는 현재 완료율에 포함하고 기간별 추이에서는 제외합니다. 수정 시각으로 완료일을 추정하지 않습니다.</p>
    <CommonBaseState v-if="!data.summary.totalCount" state="empty" title="집계할 할 일이 없습니다." message="업무 필터를 바꾸거나 내 할 일에서 항목을 등록해주세요." />
    <WorkCompletionChart :points="data.trend" :bucket="data.bucket" :total="data.periodCompletedCount" />
    <section v-if="data.units.length" class="content-panel statistics-units">
      <h2>업무별 진행률</h2>
      <p class="insight-note">각 업무의 전체 최하위 TODO 기준 · 업무의 수동 상태는 따로 표시합니다.</p>
      <article v-for="unit in data.units" :key="unit.workUnitUuid" class="statistics-unit">
        <div><NuxtLink :to="'/work/todos/' + unit.workUnitUuid" class="insight-unit-title">{{ unit.title }}</NuxtLink>
          <p class="insight-note">업무 {{ workUnitStatusLabels[unit.status] ?? unit.status }} · {{ unit.active ? '활성' : '비활성' }} · 현재 지연 {{ unit.summary.overdueCount }}개</p></div>
        <div class="insight-unit-progress"><strong>{{ unit.summary.progressRate }}%</strong><progress :value="unit.summary.completedCount" :max="unit.summary.totalCount || 1" :aria-label="unit.title + ' 완료율'" />
          <p class="insight-note">전체 {{ unit.summary.totalCount }}개 중 {{ unit.summary.completedCount }}개 완료 · 선택 기간 {{ unit.periodCompletedCount }}개</p></div>
      </article>
    </section>
    <aside class="content-panel insight-explanation">
      <h2>집계 기준</h2>
      <p>현재 현황의 분모는 선택 업무·활성 조건에 해당하는 전체 최하위 할 일입니다. 부모 항목은 중복 집계하지 않습니다. 기간 필터는 완료 추이에만 적용합니다.</p>
      <p>현재 유효한 최종 완료 시각을 사용합니다. 완료 취소·삭제·하위 항목 추가 시 과거 표시 건수가 줄 수 있으며, 재완료하면 새 완료 날짜에 집계됩니다.</p>
    </aside>
  </div>
</template>

<style scoped>
.statistics-units, .insight-explanation { padding: var(--panel-padding); }
.statistics-units h2, .insight-explanation h2 { font-size: 1.1rem; margin: 0 0 8px; }
.statistics-unit { display: grid; grid-template-columns: minmax(0, 1fr) minmax(220px, 1fr); gap: var(--section-gap); padding: var(--panel-padding) 0; border-top: 1px solid var(--color-border); }
.statistics-unit:first-of-type { margin-top: 16px; }
.insight-explanation p { font-size: .85rem; color: var(--color-text-muted); line-height: 1.7; }
@media (max-width: 767px) { .statistics-unit { grid-template-columns: 1fr; gap: 12px; } .statistics-units, .insight-explanation { padding: 16px; } }
</style>
