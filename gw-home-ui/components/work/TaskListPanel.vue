<script setup lang="ts">
import type { WorkUnitOption } from '~/types/work'

defineProps<{
  tasks: WorkUnitOption[]
  selectedTaskUuids: string[]
  activeTaskUuid: string
  searchKeyword: string
  isLoading: boolean
}>()

const emit = defineEmits<{
  'update:search-keyword': [value: string]
  'select-task': [workUnitUuid: string]
  'set-task-selection': [payload: { workUnitUuid: string, checked: boolean }]
}>()

function getStatusLabel(status: WorkUnitOption['status']): string {
  if (status === 'DONE') {
    return '완료'
  }

  if (status === 'ON_HOLD') {
    return '보류'
  }

  return '진행중'
}
</script>

<template>
  <section class="task-list-panel">
    <header class="task-list-panel__header">
      <div>
        <p class="task-list-panel__eyebrow">Task List</p>
        <h3>업무 목록</h3>
        <p class="task-list-panel__description">체크박스로 저장 대상을 고르고, 행 클릭으로 참고할 업무를 활성화할 수 있습니다.</p>
      </div>

      <input
        :value="searchKeyword"
        class="input-field task-list-panel__search"
        type="search"
        placeholder="업무 검색"
        @input="emit('update:search-keyword', ($event.target as HTMLInputElement).value)"
      >
    </header>

    <div class="task-list-panel__body">
      <div v-if="isLoading" class="task-list-panel__skeleton-list">
        <div v-for="index in 6" :key="index" class="task-list-panel__skeleton" />
      </div>

      <div v-else-if="tasks.length" class="task-list-panel__list">
        <article
          v-for="task in tasks"
          :key="task.workUnitUuid"
          class="task-list-panel__item"
          :class="{
            'task-list-panel__item--active': activeTaskUuid === task.workUnitUuid,
            'task-list-panel__item--selected': selectedTaskUuids.includes(task.workUnitUuid)
          }"
          @click="emit('select-task', task.workUnitUuid)"
        >
          <div class="task-list-panel__item-main">
            <label class="task-list-panel__check" @click.stop>
              <input
                type="checkbox"
                :checked="selectedTaskUuids.includes(task.workUnitUuid)"
                @change="emit('set-task-selection', { workUnitUuid: task.workUnitUuid, checked: ($event.target as HTMLInputElement).checked })"
              >
              <span />
            </label>
            <strong>{{ task.title }}</strong>
            <span class="task-list-panel__meta">{{ task.category || '프로젝트 미지정' }}</span>
          </div>

          <div class="task-list-panel__item-side">
            <span class="task-list-panel__status">
              {{ getStatusLabel(task.status) }}
            </span>
            <span class="task-list-panel__selection-text">
              {{ selectedTaskUuids.includes(task.workUnitUuid) ? '선택됨' : '미선택' }}
            </span>
          </div>
        </article>
      </div>

      <p v-else class="task-list-panel__empty">
        조건에 맞는 업무가 없습니다.
      </p>
    </div>
  </section>
</template>

<style scoped>
.task-list-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-large);
  background: rgba(255, 255, 255, 0.04);
}

.task-list-panel__header {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.1);
}

.task-list-panel__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.task-list-panel__header h3 {
  margin: 0;
}

.task-list-panel__description {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  line-height: 1.5;
}

.task-list-panel__search {
  width: 100%;
}

.task-list-panel__body {
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
}

.task-list-panel__list,
.task-list-panel__skeleton-list {
  display: grid;
  gap: 10px;
}

.task-list-panel__item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(147, 210, 255, 0.1);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: border-color 0.16s ease, transform 0.16s ease, background 0.16s ease;
}

.task-list-panel__item:hover {
  transform: translateY(-1px);
  border-color: rgba(147, 210, 255, 0.22);
}

.task-list-panel__item--active {
  border-color: rgba(110, 193, 255, 0.52);
  background: rgba(110, 193, 255, 0.1);
  box-shadow: 0 0 0 1px rgba(110, 193, 255, 0.14) inset;
}

.task-list-panel__item--selected {
  background: rgba(110, 193, 255, 0.08);
}

.task-list-panel__item-main,
.task-list-panel__item-side {
  display: grid;
  gap: 6px;
}

.task-list-panel__item-main {
  grid-template-columns: auto 1fr;
  align-items: start;
  column-gap: 10px;
}

.task-list-panel__item-main strong {
  line-height: 1.45;
}

.task-list-panel__meta {
  grid-column: 2;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.task-list-panel__item-side {
  justify-items: end;
}

.task-list-panel__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.8rem;
  font-weight: 700;
}

.task-list-panel__selection-text {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.task-list-panel__check {
  position: relative;
  display: inline-flex;
  width: 20px;
  height: 20px;
  margin-top: 1px;
}

.task-list-panel__check input {
  position: absolute;
  inset: 0;
  opacity: 0;
  margin: 0;
}

.task-list-panel__check span {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  border: 1px solid rgba(147, 210, 255, 0.3);
  background: rgba(255, 255, 255, 0.05);
}

.task-list-panel__check input:checked + span {
  border-color: rgba(110, 193, 255, 0.72);
  background: linear-gradient(135deg, rgba(58, 159, 214, 0.9), rgba(110, 193, 255, 0.95));
  box-shadow: 0 0 0 1px rgba(110, 193, 255, 0.15);
}

.task-list-panel__empty {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.task-list-panel__skeleton {
  height: 86px;
  border-radius: var(--radius-medium);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.04), rgba(147, 210, 255, 0.12), rgba(255, 255, 255, 0.04));
  background-size: 220% 100%;
  animation: task-list-panel-skeleton 1.3s linear infinite;
}

@keyframes task-list-panel-skeleton {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: -100% 0;
  }
}
</style>
