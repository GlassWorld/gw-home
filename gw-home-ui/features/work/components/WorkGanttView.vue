<script setup lang="ts">
import type { WorkGantt } from '../types/work-insights.types'
import { calendarDates, schedulePosition, scheduleLabel, todoStatusLabel, todoDetailLink, workUnitStatusLabels } from '../utils/work-insights'
const props = defineProps<{ data: WorkGantt; view: 'list' | 'chart' }>()
const dates = computed(() => calendarDates(props.data.period.fromDate, props.data.period.toDate))
const groups = computed(() => props.data.units.map(unit => ({
  unit,
  rows: props.data.rows.filter(row => row.workUnitUuid === unit.workUnitUuid && (row.startDate || row.dueDate || !row.leaf)),
  unscheduled: props.data.rows.filter(row => row.workUnitUuid === unit.workUnitUuid && !row.startDate && !row.dueDate && row.leaf)
})))
function barStyle(start: string | null, due: string | null) {
  const position = schedulePosition(start, due, props.data.period.fromDate, props.data.period.toDate)
  return position ? { left: position.left + '%', width: position.width + '%' } : undefined
}
</script>

<template>
  <div class="insight-unit-list">
    <section v-for="{ unit, rows, unscheduled } in groups" :key="unit.workUnitUuid" class="content-panel gantt-unit">
      <header class="insight-unit-heading">
        <div>
          <NuxtLink :to="'/work/todos/' + unit.workUnitUuid" class="insight-unit-title">{{ unit.title }}</NuxtLink>
          <p class="insight-note">업무 {{ workUnitStatusLabels[unit.status] ?? unit.status }} · {{ unit.active ? '활성' : '비활성' }}</p>
          <p class="insight-note">전체 TODO에서 파생한 일정: {{ scheduleLabel(unit.startDate, unit.dueDate) }}{{ unit.partialSchedule ? ' · 미정 일정 포함' : '' }}</p>
        </div>
        <div class="insight-unit-progress"><strong>{{ unit.summary.progressRate }}%</strong>
          <progress :value="unit.summary.completedCount" :max="unit.summary.totalCount || 1" :aria-label="unit.title + ' 전체 할 일 완료율'" />
          <span class="insight-note">전체 {{ unit.summary.totalCount }}개 중 {{ unit.summary.completedCount }}개 완료</span>
        </div>
      </header>
      <div v-if="view === 'chart' && rows.length" class="gantt-scroll" tabindex="0" role="region" :aria-label="unit.title + ' 일정 차트. 좌우로 스크롤할 수 있습니다.'">
        <div class="gantt-grid" :style="{ minWidth: (220 + dates.length * 30) + 'px' }">
          <div class="gantt-label gantt-axis">할 일 · 상태</div>
          <div class="gantt-dates" :style="{ gridTemplateColumns: 'repeat(' + dates.length + ', 1fr)' }">
            <span v-for="date in dates" :key="date" :class="{ 'gantt-today': date === data.period.asOfDate }">{{ date.slice(5).replace('-', '/') }}</span>
          </div>
          <div class="gantt-label gantt-unit-label">
            <NuxtLink :to="'/work/todos/' + unit.workUnitUuid">업무 전체 일정</NuxtLink>
            <small>TODO에서 파생{{ unit.partialSchedule ? ' · 미정 일정 포함' : '' }}</small>
          </div>
          <div class="gantt-track" :style="{ backgroundSize: (100 / dates.length) + '% 100%' }">
            <NuxtLink v-if="barStyle(unit.startDate, unit.dueDate)" :to="'/work/todos/' + unit.workUnitUuid"
              class="gantt-bar gantt-unit-bar" :class="{ 'is-partial': unit.partialSchedule }"
              :style="barStyle(unit.startDate, unit.dueDate)"
              :aria-label="unit.title + ' 전체 파생 일정, ' + scheduleLabel(unit.startDate, unit.dueDate)">
              {{ unit.summary.progressRate }}%
            </NuxtLink>
            <span v-else class="gantt-no-bar">{{ unit.startDate || unit.dueDate ? '전체 일정이 조회 기간 밖에 있습니다.' : '일정 미지정' }}</span>
          </div>
          <template v-for="row in rows" :key="row.todoUuid">
            <div class="gantt-label" :style="{ paddingLeft: (12 + Math.min(row.depth, 3) * 12) + 'px' }">
              <NuxtLink :to="todoDetailLink(row)">{{ row.depth ? '↳ ' : '' }}{{ row.title }}</NuxtLink>
              <small>{{ row.leaf ? '' : '상위 · ' }}{{ todoStatusLabel(row) }} · {{ row.progressRate }}%{{ row.contextOnly ? ' · 계층 표시' : '' }}</small>
            </div>
            <div class="gantt-track" :style="{ backgroundSize: (100 / dates.length) + '% 100%' }">
              <NuxtLink v-if="barStyle(row.startDate, row.dueDate)" :to="todoDetailLink(row)" class="gantt-bar"
                :class="{ 'is-complete': row.completed, 'is-overdue': row.delayed, 'is-partial': !row.startDate || !row.dueDate, 'is-parent': !row.leaf }"
                :style="barStyle(row.startDate, row.dueDate)" :title="row.title + ' · ' + scheduleLabel(row.startDate, row.dueDate)"
                :aria-label="row.title + ', ' + todoStatusLabel(row) + ', ' + scheduleLabel(row.startDate, row.dueDate)">
                <span aria-hidden="true">{{ !row.startDate || !row.dueDate ? '◆' : row.progressRate + '%' }}</span>
              </NuxtLink>
              <span v-else class="gantt-no-bar">{{ row.startDate || row.dueDate ? '기간 밖 · 계층 표시' : '상위 일정 미지정' }}</span>
            </div>
          </template>
        </div>
      </div>
      <ul v-else-if="rows.length" class="schedule-list">
        <li v-for="row in rows" :key="row.todoUuid">
          <NuxtLink :to="todoDetailLink(row)" class="schedule-item" :style="{ paddingLeft: (16 + Math.min(row.depth, 3) * 10) + 'px' }">
            <span class="schedule-item-title">{{ row.depth ? '↳ ' : '' }}{{ row.title }}</span>
            <span class="insight-note">{{ scheduleLabel(row.startDate, row.dueDate) }}</span>
            <span class="schedule-status" :class="{ 'is-complete': row.completed, 'is-overdue': row.delayed }">{{ row.leaf ? '' : '상위 · ' }}{{ todoStatusLabel(row) }} · {{ row.progressRate }}%{{ row.contextOnly ? ' · 계층 표시' : '' }}</span>
          </NuxtLink>
        </li>
      </ul>
      <details v-if="unscheduled.length" class="gantt-unscheduled">
        <summary>일정 미지정 할 일 {{ unscheduled.length }}개</summary>
        <ul class="schedule-list">
          <li v-for="row in unscheduled" :key="row.todoUuid">
            <NuxtLink :to="todoDetailLink(row)" class="schedule-item">
              <span class="schedule-item-title">{{ row.title }}</span>
              <span class="insight-note">{{ row.depth ? '하위 할 일 · ' : '' }}일정 미지정 · {{ todoStatusLabel(row) }}</span>
            </NuxtLink>
          </li>
        </ul>
      </details>
    </section>
  </div>
</template>

<style scoped>
.gantt-unit { padding: 0; overflow: hidden; }
.insight-unit-heading { padding: var(--panel-padding); }
.gantt-scroll { overflow-x: auto; max-width: 100%; overscroll-behavior-inline: contain; }
.gantt-grid { display: grid; grid-template-columns: 220px minmax(0, 1fr); }
.gantt-label { position: sticky; left: 0; z-index: 1; background: var(--color-surface); padding: 12px; border-top: 1px solid var(--color-border); border-right: 1px solid var(--color-border); overflow-wrap: anywhere; min-width: 0; }
.gantt-label a { display: block; color: var(--color-text); min-height: 28px; }
.gantt-label small { display: block; color: var(--color-text-muted); }
.gantt-axis { font-size: .85rem; background: var(--color-surface-muted); }
.gantt-dates { display: grid; background: var(--color-surface-muted); border-top: 1px solid var(--color-border); }
.gantt-dates span { padding: 12px 0; text-align: center; font-size: .65rem; color: var(--color-text-muted); border-right: 1px solid var(--color-border); }
.gantt-dates .gantt-today { background: var(--color-primary-soft); color: var(--color-primary); font-weight: 700; }
.gantt-track { position: relative; min-height: 72px; border-top: 1px solid var(--color-border); background-image: linear-gradient(to right, var(--color-border) 1px, transparent 1px); }
.gantt-bar { position: absolute; top: 50%; transform: translateY(-50%); display: flex; align-items: center; justify-content: center; min-height: 44px; border: 1px solid var(--color-primary); border-radius: 6px; background: var(--color-primary-soft); color: var(--color-primary); font-size: .75rem; overflow: hidden; }
.gantt-bar.is-complete { background: var(--color-success-soft); border-color: var(--color-success); color: var(--color-success); }
.gantt-bar.is-overdue { background: var(--color-danger-soft); border-color: var(--color-danger); color: var(--color-danger); }
.gantt-bar.is-parent { border-bottom-width: 4px; }
.gantt-unit-label { background: var(--color-surface-muted); }
.gantt-unit-bar { border-width: 2px; background: var(--color-surface-strong); color: var(--color-text); }
.gantt-bar.is-partial { border-style: dashed; }
.gantt-no-bar { display: inline-block; padding: var(--panel-padding) 12px; color: var(--color-text-muted); font-size: .8rem; }
.gantt-unscheduled { border-top: 1px solid var(--color-border); }
.gantt-unscheduled summary { cursor: pointer; padding: 16px var(--panel-padding); min-height: 48px; color: var(--color-text-muted); }
.schedule-list { list-style: none; padding: 0; margin: 0; }
.schedule-item { display: grid; gap: 4px; padding: 16px; border-top: 1px solid var(--color-border); color: var(--color-text); overflow-wrap: anywhere; }
.schedule-item:hover { background: var(--color-surface-muted); }
.schedule-item-title { font-weight: 600; }
.schedule-status { font-size: .8rem; color: var(--color-primary); }
.schedule-status.is-complete { color: var(--color-success); }
.schedule-status.is-overdue { color: var(--color-danger); }
@media (max-width: 767px) { .insight-unit-heading { padding: 16px; } .gantt-grid { grid-template-columns: 160px minmax(0, 1fr); } }
</style>
