<script setup lang="ts">
import type { WorkInsightFilters, WorkInsightMode } from '../types/work-insights.types'
import type { WorkUnitOption } from '../types/work.types'
const filters = defineModel<WorkInsightFilters>({ required: true })
defineProps<{ mode: WorkInsightMode; workUnits: WorkUnitOption[]; loading: boolean; validationMessage: string }>()
defineEmits<{ apply: []; preset: [days: number] }>()
const expanded = ref(false)
onMounted(() => { expanded.value = window.matchMedia('(min-width: 768px)').matches })
function updateExpanded(event: Event) { expanded.value = (event.target as HTMLDetailsElement).open }
</script>

<template>
  <details class="content-panel insight-filters" :open="expanded" @toggle="updateExpanded">
    <summary class="insight-filter-toggle">조회 조건 · 업무 / 기간<span v-if="filters.timeZone !== 'Asia/Seoul'"> / 시간대</span></summary>
    <form @submit.prevent="$emit('apply')">
    <fieldset :disabled="loading" class="insight-filter-grid">
      <label><span>업무</span><select v-model="filters.workUnitUuid" class="input-field">
        <option value="">전체 업무</option>
        <option v-for="unit in workUnits.filter(item => filters.includeInactive || item.useYn === 'Y')" :key="unit.workUnitUuid" :value="unit.workUnitUuid">
          {{ unit.title }}{{ unit.useYn === 'N' ? ' (비활성)' : '' }}
        </option>
      </select></label>
      <label><span>{{ mode === 'gantt' ? '일정 시작' : '완료 기간 시작' }}</span><input v-model="filters.fromDate" class="input-field" type="date" min="1900-01-01" max="9998-12-31" required></label>
      <label><span>{{ mode === 'gantt' ? '일정 종료' : '완료 기간 종료' }}</span><input v-model="filters.toDate" class="input-field" type="date" :min="filters.fromDate || '1900-01-01'" max="9998-12-31" required></label>
      <label v-if="mode === 'statistics'"><span>완료 추이 단위</span><select v-model="filters.bucket" class="input-field">
        <option value="day">일별</option><option value="week">주별 · 월요일 시작</option><option value="month">월별</option>
      </select></label>
      <label v-if="filters.timeZone !== 'Asia/Seoul'"><span>시간대</span><input v-model="filters.timeZone" class="input-field" list="work-insight-time-zones" maxlength="80" required placeholder="Asia/Seoul"></label>
      <label class="insight-checkbox"><input v-model="filters.includeInactive" type="checkbox">비활성 업무 포함</label>
      <div class="insight-actions">
        <CommonBaseButton v-for="days in (mode === 'gantt' ? [7, 14, 30] : [7, 30, 90])" :key="days" variant="secondary" size="small" @click="$emit('preset', days)">
          {{ mode === 'gantt' ? '오늘부터' : '최근' }} {{ days }}일
        </CommonBaseButton>
        <CommonBaseButton type="submit">조회</CommonBaseButton>
      </div>
    </fieldset>
    <datalist id="work-insight-time-zones"><option>Asia/Seoul</option><option>UTC</option><option>America/New_York</option><option>Europe/London</option><option>Asia/Tokyo</option></datalist>
    <p class="insight-note">{{ mode === 'gantt' ? '선택 기간과 겹치는 일정과 미지정 항목을 표시합니다. 최대 93일.' : '기간은 완료 추이에 적용합니다. 현재 현황은 선택 업무의 전체 최하위 할 일 기준입니다. 최대 366일.' }}</p>
    <p v-if="validationMessage" class="insight-error" role="alert">{{ validationMessage }}</p>
    </form>
  </details>
</template>
