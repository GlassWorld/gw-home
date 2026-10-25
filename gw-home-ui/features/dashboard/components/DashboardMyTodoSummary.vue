<script setup lang="ts">
import { useWorkTodoApi } from '~/composables/use-work-todo-api'
import { useLifeTodoApi } from '~/composables/use-life-todo-api'
import type { LifeTodoSummary } from '~/features/life/types/life-todo.types'

const props = withDefaults(defineProps<{ kind?: 'work' | 'life' }>(), { kind: 'work' })
const { fetchMyWorkTodoSummary } = useWorkTodoApi()
const { fetchSummary } = useLifeTodoApi()
const summary = ref<Pick<LifeTodoSummary, 'todayCount' | 'upcomingCount' | 'overdueCount' | 'progressRate'> | null>(null)
const title = computed(() => props.kind === 'life' ? '일상 할 일' : '업무 할 일')
const path = computed(() => props.kind === 'life' ? '/life/todos' : '/work/todos')
const isLoading = ref(true)
const hasError = ref(false)
async function load() {
  isLoading.value = true
  hasError.value = false
  try { summary.value = props.kind === 'life' ? await fetchSummary() : await fetchMyWorkTodoSummary() }
  catch { hasError.value = true }
  finally { isLoading.value = false }
}
onMounted(load)
</script>

<template>
  <section class="content-panel dashboard-todos" :aria-label="`${title} 요약`">
    <header><h2 class="section-title">{{ title }}</h2><CommonBaseButton :to="path" variant="secondary">관리</CommonBaseButton></header>
    <CommonBaseState v-if="isLoading" state="loading" :message="`${title}을 확인하는 중입니다.`" />
    <CommonBaseState v-else-if="hasError" state="error" message="할 일 요약을 불러오지 못했습니다." action-label="다시 시도" @action="load" />
    <div v-else-if="summary" class="dashboard-todos__metrics">
      <NuxtLink :to="{ path, query: { group: 'TODAY' } }" :aria-label="`오늘 ${title} ${summary.todayCount}건 보기`"><span>오늘 남은 일 →</span><strong>{{ summary.todayCount }}</strong></NuxtLink>
      <div><span>예정</span><strong>{{ summary.upcomingCount }}</strong></div>
      <div class="dashboard-todos__overdue"><span>지연</span><strong>{{ summary.overdueCount }}</strong></div>
      <div><span>현재 완료율</span><strong>{{ summary.progressRate }}%</strong></div>
    </div>
    <p class="section-description">{{ kind === 'life' ? '생활 카테고리의 일상 할 일 기준' : '활성 업무의 최하위 할 일 기준' }}</p>
  </section>
</template>

<style scoped>
.dashboard-todos { display: grid; gap: var(--section-gap); padding: var(--panel-padding); }
.dashboard-todos header { display: flex; align-items: center; justify-content: space-between; gap: var(--section-gap); }
.dashboard-todos__metrics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--section-gap); }
.dashboard-todos__metrics > :is(div, a) { padding: var(--card-padding); background: var(--color-surface-muted); border-radius: var(--radius-medium); }
.dashboard-todos__metrics > a { color: var(--color-accent); text-decoration: none; }
.dashboard-todos__metrics span { display: block; color: var(--color-text-muted); font-size: 0.85rem; }
.dashboard-todos__metrics strong { font-size: 1.8rem; }
.dashboard-todos__overdue strong { color: var(--color-danger); }
@media (max-width: 767px) {
  .dashboard-todos { padding: var(--mobile-panel-padding); }
  .dashboard-todos__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .dashboard-todos__metrics > :is(div, a) { padding: 12px; }
}
</style>
