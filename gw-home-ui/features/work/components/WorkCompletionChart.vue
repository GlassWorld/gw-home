<script setup lang="ts">
import type { WorkCompletionPoint, CompletionBucket } from '../types/work-insights.types'
const props = defineProps<{ points: WorkCompletionPoint[]; bucket: CompletionBucket; total: number }>()
const maximum = computed(() => Math.max(1, ...props.points.map(point => point.completedCount)))
const labels = { day: '일별', week: '주별', month: '월별' }
</script>

<template>
  <section class="content-panel completion-chart">
    <header class="insight-unit-heading"><div><h2>{{ labels[bucket] }} 완료 추이</h2><p class="insight-note">선택 기간 {{ total }}개 완료 · 0건 구간 포함{{ bucket === 'week' ? ' · 월요일 시작' : '' }}</p></div></header>
    <p v-if="!total" class="insight-note">이 기간에 완료 시각이 기록된 할 일이 없습니다.</p>
    <figure>
      <div class="completion-scroll" tabindex="0" role="region" aria-label="완료 추이 그래프. 좌우로 스크롤할 수 있습니다. 숫자는 아래 표에서도 확인할 수 있습니다.">
        <div class="completion-columns" :style="{ minWidth: Math.max(240, points.length * 44) + 'px', gridTemplateColumns: 'repeat(' + points.length + ', minmax(0, 1fr))' }" aria-hidden="true">
          <div v-for="point in points" :key="point.startDate" class="completion-column">
            <div class="completion-bar-space"><div class="completion-bar" :style="{ height: (point.completedCount / maximum * 100) + '%' }"><span>{{ point.completedCount }}</span></div></div>
            <span class="completion-label">{{ point.startDate.slice(5).replace('-', '/') }}</span>
          </div>
        </div>
      </div>
      <figcaption class="insight-note">현지 완료 날짜 기준. 주·월의 첫 구간과 마지막 구간은 선택 기간 안에서만 집계합니다.</figcaption>
    </figure>
    <details class="completion-table">
      <summary>완료 건수 표로 보기</summary>
      <div class="insight-table-scroll">
        <table><caption class="sr-only">{{ labels[bucket] }} 완료 건수</caption>
          <thead><tr><th scope="col">기간</th><th scope="col">완료 건수</th></tr></thead>
          <tbody><tr v-for="point in points" :key="point.startDate"><th scope="row">{{ point.startDate }}{{ point.endDate !== point.startDate ? ' ~ ' + point.endDate : '' }}</th><td>{{ point.completedCount }}개</td></tr></tbody>
        </table>
      </div>
    </details>
  </section>
</template>

<style scoped>
.completion-chart { padding: var(--panel-padding); min-width: 0; }
.completion-chart h2 { font-size: 1.1rem; margin: 0; }
figure { margin: 0; }
.completion-scroll { overflow-x: auto; padding-top: 24px; overscroll-behavior-inline: contain; }
.completion-columns { display: grid; gap: 2px; padding: 0 6px; }
.completion-bar-space { height: 160px; display: flex; align-items: flex-end; border-bottom: 1px solid var(--color-border-strong); padding: 0 8px; }
.completion-bar { position: relative; min-height: 2px; width: 100%; max-width: 48px; margin: 0 auto; border-radius: 4px 4px 0 0; background: var(--color-primary); }
.completion-bar span { position: absolute; top: -23px; left: 50%; transform: translateX(-50%); font-size: .75rem; color: var(--color-text); }
.completion-label { display: block; text-align: center; padding: 10px 0; font-size: .7rem; color: var(--color-text-muted); }
figcaption { margin-top: 12px; }
.completion-table { margin-top: 16px; border-top: 1px solid var(--color-border); }
.completion-table summary { padding: 12px 0; min-height: 44px; cursor: pointer; }
.completion-table table { width: 100%; border-collapse: collapse; text-align: left; font-size: .85rem; }
.completion-table th, .completion-table td { padding: 10px; border-bottom: 1px solid var(--color-border); }
.completion-table tbody th { font-weight: 400; }
@media (max-width: 767px) { .completion-chart { padding: 16px; } }
</style>
