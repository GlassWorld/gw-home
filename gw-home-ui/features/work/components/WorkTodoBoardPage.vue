<script setup lang="ts">
import WorkTodoInboxCard from './WorkTodoInboxCard.vue'
import WorkTodoFormModal from './WorkTodoFormModal.vue'
import WorkTodoDetailModal from './WorkTodoDetailModal.vue'
import { useMyWorkTodos } from '../composables/use-my-work-todos'
import { workTodoGroups } from '../utils/work-todo'
import type { WorkTodoGroup } from '../types/work.types'

const route = useRoute()
const router = useRouter()
const showRetiredReportNotice = computed(() => route.query.notice === 'reports-retired')

function dismissRetiredReportNotice() {
  return router.replace({ query: { ...route.query, notice: undefined } })
}

const {
  filters, activeFilterCount, group, page, result, summary, workUnits, availableWorkUnits, createWorkUnits,
  isListLoading, isBusy, isLoading, isSaving, errorMessage, optionsError,
  activeWorkUnitUuid, selectedTodoUuid, selectedTodo, isFormVisible, editingTodoUuid, formParentTitle, formState,
  refresh, loadOptions, applyFilters, selectGroup, changePage, startCreate, openTodo, editSelected, addChild,
  closeForm, handleSubmit, handleToggleCompletion, handleDeleteTodo
} = useMyWorkTodos()

const groupCounts = computed<Record<WorkTodoGroup, number>>(() => ({
  ALL: summary.value?.totalCount ?? 0, TODAY: summary.value?.todayCount ?? 0,
  UPCOMING: summary.value?.upcomingCount ?? 0, OVERDUE: summary.value?.overdueCount ?? 0,
  COMPLETED: summary.value?.completedCount ?? 0, UNSCHEDULED: summary.value?.unscheduledCount ?? 0
}))
</script>

<template>
  <main class="todo-inbox page-container page-stack">
    <CommonBasePageHeader title="내 할 일" eyebrow="My Workspace" description="여러 업무의 할 일을 한곳에서 확인하고 마무리하세요.">
      <template #actions>
        <CommonBaseButton variant="secondary" to="/work/gantt">간트차트</CommonBaseButton>
        <CommonBaseButton variant="secondary" to="/work/statistics">완료 통계</CommonBaseButton>
        <CommonBaseButton variant="secondary" to="/work">업무 관리</CommonBaseButton>
        <CommonBaseButton :disabled="isBusy || !workUnits.some(unit => unit.useYn === 'Y')" @click="startCreate">+ 할 일 추가</CommonBaseButton>
      </template>
    </CommonBasePageHeader>
    <CommonBaseState v-if="showRetiredReportNotice" state="empty" title="일일보고·주간보고 기능이 종료되었습니다."
      message="이제 내 할 일에서 업무를 정리해 주세요. 기존 보고서 데이터는 보관되어 있으며 이 화면에서는 조회할 수 없습니다."
      action-label="안내 닫기" @action="dismissRetiredReportNotice" />
    <section v-if="summary" class="content-panel todo-inbox__overview" aria-label="현재 완료 현황" :aria-busy="isListLoading">
      <div><span>현재 완료율</span><strong>{{ summary.progressRate }}<small>%</small></strong></div>
      <div class="todo-inbox__progress">
        <p>{{ summary.completedCount }}개 완료 <span>/ {{ summary.totalCount }}개</span></p>
        <progress :value="summary.completedCount" :max="summary.totalCount || 1" aria-label="최하위 할 일 완료율" />
        <p class="todo-inbox__hint">하위 항목이 없는 할 일 기준 · {{ summary.asOfDate }}</p>
      </div>
    </section>
    <details class="content-panel todo-inbox__filters">
      <summary>업무·검색·기간 필터 <span v-if="activeFilterCount">· {{ activeFilterCount }}개 적용 중</span></summary>
      <form @submit.prevent="applyFilters">
        <fieldset :disabled="isBusy" class="todo-inbox__filter-grid">
          <label><span>업무</span><select v-model="filters.workUnitUuid" class="input-field">
            <option value="">전체 업무</option>
            <option v-for="unit in availableWorkUnits" :key="unit.workUnitUuid" :value="unit.workUnitUuid">{{ unit.title }}{{ unit.useYn === 'N' ? ' (비활성)' : '' }}</option>
          </select></label>
          <label><span>검색</span><input v-model="filters.keyword" class="input-field" type="search" maxlength="200" placeholder="제목 또는 내용 검색"></label>
          <div class="todo-inbox__advanced">
            <div class="todo-inbox__dates">
              <label><span>일정 시작</span><input v-model="filters.fromDate" class="input-field" type="date"></label>
              <label><span>일정 종료</span><input v-model="filters.toDate" class="input-field" type="date" :min="filters.fromDate || undefined"></label>
              <label class="todo-inbox__check"><input v-model="filters.includeInactive" type="checkbox">비활성 업무 포함</label>
            </div>
            <p class="todo-inbox__hint">선택 기간과 겹치는 일정을 조회합니다. 일정 없는 항목은 기간 조회에서 제외됩니다.</p>
          </div>
          <CommonBaseButton type="submit" variant="secondary">조회</CommonBaseButton>
        </fieldset>
      </form>
    </details>
    <CommonBaseState v-if="optionsError" state="error" :message="optionsError" action-label="업무 다시 불러오기" @action="loadOptions" />
    <nav class="todo-inbox__groups" aria-label="할 일 분류">
      <button v-for="item in workTodoGroups" :key="item.value" type="button" :aria-pressed="group === item.value"
        :disabled="isBusy" @click="selectGroup(item.value)">
        {{ item.label }} <span v-if="summary">{{ groupCounts[item.value] }}</span>
      </button>
    </nav>
    <CommonBaseState v-if="isListLoading && !result" state="loading" message="내 할 일을 불러오는 중입니다." />
    <CommonBaseState v-else-if="errorMessage" state="error" :message="errorMessage" action-label="다시 시도" @action="refresh" />
    <section v-else-if="result?.content.length" class="todo-inbox__list" aria-label="내 할 일 목록" :aria-busy="isBusy">
      <WorkTodoInboxCard v-for="todo in result.content" :key="todo.todoUuid" :todo="todo" :disabled="isBusy"
        @open="openTodo(todo)" @edit="openTodo(todo, 'edit')" @complete="openTodo(todo, 'complete')" />
    </section>
    <CommonBaseState v-else state="empty" title="표시할 할 일이 없습니다."
      message="조회 조건을 바꾸거나 새 할 일을 추가해보세요. 업무가 없다면 업무 관리에서 먼저 등록해주세요." />
    <p v-if="isLoading" role="status" class="todo-inbox__hint">선택한 할 일을 확인하는 중입니다.</p>
    <CommonBasePagination v-if="result && result.totalPages > 1" :page="page" :total-pages="result.totalPages"
      :disabled="isBusy" @previous="changePage(page - 1)" @next="changePage(page + 1)" />
    <WorkTodoFormModal v-model="formState" v-model:work-unit-uuid="activeWorkUnitUuid" :visible="isFormVisible"
      :is-submitting="isSaving" :is-editing="!!editingTodoUuid" :parent-title="formParentTitle"
      :work-unit-options="createWorkUnits" @close="closeForm" @submit="handleSubmit" />
    <WorkTodoDetailModal :visible="!!selectedTodo" :todo="selectedTodo" :is-saving="isSaving"
      @close="selectedTodoUuid = ''" @edit="editSelected" @add-child="addChild"
      @toggle-completion="handleToggleCompletion" @delete="handleDeleteTodo" />
  </main>
</template>

<style scoped>
.todo-inbox__overview { display: flex; align-items: center; gap: 24px; padding: var(--panel-padding); }
.todo-inbox__overview strong { display: block; font-size: 2.5rem; line-height: 1.3; color: var(--color-primary); }
.todo-inbox__overview small { font-size: 1rem; margin-left: 4px; }
.todo-inbox__overview span, .todo-inbox__hint { color: var(--color-text-muted); font-size: 0.82rem; }
.todo-inbox__progress { flex: 1; min-width: 0; }
.todo-inbox__progress p { margin: 4px 0 8px; }
.todo-inbox__progress progress { display: block; width: 100%; height: 6px; border: 0; border-radius: 8px; overflow: hidden; background: var(--color-surface-strong); accent-color: var(--color-primary); }
.todo-inbox__progress progress::-webkit-progress-bar { background: var(--color-surface-strong); }
.todo-inbox__progress progress::-webkit-progress-value { background: var(--color-primary); }
.todo-inbox__hint { margin: 8px 0 0; overflow-wrap: anywhere; }
.todo-inbox__filters { padding: var(--panel-padding); }
.todo-inbox__filters > summary { cursor: pointer; min-height: 44px; align-content: center; color: var(--color-text-muted); }
.todo-inbox__filters[open] > summary { margin-bottom: 16px; }
.todo-inbox__filter-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 2fr) auto; gap: var(--section-gap); border: 0; padding: 0; margin: 0; min-width: 0; align-items: end; }
.todo-inbox__filter-grid label { display: grid; gap: 8px; min-width: 0; font-size: 0.85rem; color: var(--color-text-muted); }
.todo-inbox__filter-grid > .base-button { grid-column: 3; grid-row: 1; }
.todo-inbox__advanced { grid-column: 1 / -1; }
.todo-inbox__dates { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: var(--section-gap); }
.todo-inbox__dates .todo-inbox__check { display: flex; align-items: center; min-height: 44px; }
.todo-inbox__check input { width: 20px; height: 20px; }
.todo-inbox__groups { display: flex; flex-wrap: wrap; gap: 8px; }
.todo-inbox__groups button { min-height: var(--button-height); padding: 6px 10px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-small); color: var(--color-text-muted); font: inherit; font-size: 0.875rem; cursor: pointer; }
.todo-inbox__groups button[aria-pressed="true"] { background: var(--color-primary-soft); color: var(--color-primary); border-color: var(--color-primary); }
.todo-inbox__groups span { margin-left: 6px; font-size: 0.8rem; }
.todo-inbox__list { display: grid; gap: 12px; }
@media (max-width: 767px) {
  .todo-inbox__overview { padding: var(--mobile-panel-padding); gap: var(--section-gap); }
  .todo-inbox__overview strong { font-size: 2rem; }
  .todo-inbox__filters { padding: var(--mobile-panel-padding); }
  .todo-inbox__filter-grid { grid-template-columns: minmax(0, 1fr) auto; gap: 12px; }
  .todo-inbox__filter-grid > label:first-child { grid-column: 1 / -1; }
  .todo-inbox__filter-grid > .base-button { grid-column: 2; grid-row: 2; }
  .todo-inbox__dates { grid-template-columns: minmax(0, 1fr); }
  .todo-inbox__groups button { padding-inline: 12px; font-size: 0.88rem; }
}
</style>
