<script setup lang="ts">
import type { LifeDailyDay } from '../types/life-daily.types'
const props = defineProps<{ month: string; selectedDate: string; today: string; days: LifeDailyDay[]; disabled: boolean; loading: boolean }>()
defineEmits<{ select: [date: string]; move: [offset: number]; today: [] }>()
const leadingDays = computed(() => new Date(`${props.month}-01T12:00:00`).getDay())
</script>

<template>
  <section class="daily-calendar" aria-label="매일 체크 캘린더" :aria-busy="loading">
    <div class="daily-calendar__toolbar">
      <CommonBaseButton variant="secondary" :disabled="disabled" aria-label="이전 달" @click="$emit('move', -1)">‹</CommonBaseButton>
      <h2 aria-live="polite">{{ month.replace('-', '년 ') }}월</h2>
      <CommonBaseButton variant="secondary" :disabled="disabled" aria-label="다음 달" @click="$emit('move', 1)">›</CommonBaseButton>
      <CommonBaseButton variant="secondary" :disabled="disabled" @click="$emit('today')">오늘</CommonBaseButton>
    </div>
    <div class="daily-calendar__grid">
      <span v-for="weekday in ['일', '월', '화', '수', '목', '금', '토']" :key="weekday" class="daily-calendar__weekday">{{ weekday }}</span>
      <span v-for="blank in leadingDays" :key="`blank-${blank}`" aria-hidden="true" />
      <button v-for="day in days" :key="day.date" type="button" :disabled="disabled || loading"
        :aria-pressed="day.date === selectedDate" :aria-current="day.date === today ? 'date' : undefined"
        :aria-label="`${day.date}${day.date === today ? ' 오늘' : ''}, ${day.date > today ? `예정 ${day.total}개` : `${day.total}개 중 ${day.completed}개 완료`}`"
        :class="{ 'daily-calendar__done': day.total > 0 && day.completed === day.total, 'daily-calendar__future': day.date > today }"
        @click="$emit('select', day.date)">
        <span>{{ day.day }}</span><small>{{ day.date > today ? (day.total ? '예정' : '—') : day.total ? `${day.completed}/${day.total}` : '—' }}</small>
      </button>
    </div>
    <p class="section-description">날짜를 누르면 체크리스트가 표시됩니다. 숫자는 완료 / 전체 개수입니다.</p>
  </section>
</template>

<style scoped>
.daily-calendar { min-width: 0; }
.daily-calendar__toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.daily-calendar__toolbar h2 { flex: 1; margin: 0; font-size: 1.1rem; text-align: center; }
.daily-calendar__grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 8px; }
.daily-calendar__weekday { text-align: center; color: var(--color-text-muted); font-size: 0.85rem; }
.daily-calendar__grid button { min-width: 0; min-height: 64px; display: grid; place-content: center; gap: 2px; padding: 4px 0; font: inherit; color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-small); cursor: pointer; }
.daily-calendar__grid button small { font-size: 0.7rem; color: var(--color-text-muted); }
.daily-calendar__grid button[aria-current="date"] > span { text-decoration: underline; text-underline-offset: 4px; font-weight: 700; }
.daily-calendar__grid button.daily-calendar__done { background: color-mix(in srgb, var(--color-accent) 15%, var(--color-surface)); }
.daily-calendar__grid button[aria-pressed="true"] { outline: 2px solid var(--color-accent); outline-offset: -2px; }
.daily-calendar__grid button.daily-calendar__future { color: var(--color-text-muted); }
.daily-calendar__grid button:disabled { cursor: default; }
</style>
