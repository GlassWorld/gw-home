<script setup lang="ts">
import type { WorkTodoListItem } from '../types/work.types'
import { workTodoGroups, formatTodoCompletion } from '../utils/work-todo'
defineProps<{ todo: WorkTodoListItem; disabled: boolean }>()
defineEmits<{ open: []; edit: []; complete: [] }>()
</script>

<template>
  <article class="todo-inbox-card" :class="{ 'todo-inbox-card--done': todo.completed }">
    <div class="todo-inbox-card__body">
      <div class="todo-inbox-card__meta">
        <NuxtLink :to="`/work/todos/${todo.workUnitUuid}`">{{ todo.workUnitTitle }}</NuxtLink>
        <span v-if="!todo.workUnitActive">비활성 업무</span>
        <span class="todo-inbox-card__badge" :class="`todo-inbox-card__badge--${todo.group.toLowerCase()}`">
          {{ workTodoGroups.find(item => item.value === todo.group)?.label }}
        </span>
      </div>
      <button type="button" class="todo-inbox-card__title" :disabled="disabled" @click="$emit('open')">{{ todo.title }}</button>
      <p v-if="todo.parentTitle" class="todo-inbox-card__parent">상위 할 일 · {{ todo.parentTitle }}</p>
      <p v-if="todo.description" class="todo-inbox-card__description">{{ todo.description }}</p>
      <div class="todo-inbox-card__schedule">
        <span v-if="todo.startDate">시작 {{ todo.startDate }}</span>
        <span v-if="todo.dueDate">마감 {{ todo.dueDate }}</span>
        <span v-if="!todo.startDate && !todo.dueDate">일정 없음</span>
        <span v-if="todo.completed">{{ formatTodoCompletion(todo.completedAt) }}</span>
      </div>
    </div>
    <div class="todo-inbox-card__actions">
      <CommonBaseButton :variant="todo.completed ? 'secondary' : 'primary'" :disabled="disabled"
        :aria-label="`${todo.title} ${todo.completed ? '완료 취소' : '완료 처리'}`" @click="$emit('complete')">
        {{ todo.completed ? '완료 취소' : '완료' }}
      </CommonBaseButton>
      <CommonBaseButton variant="secondary" :disabled="disabled" :aria-label="`${todo.title} 수정`" @click="$emit('edit')">수정</CommonBaseButton>
    </div>
  </article>
</template>

<style scoped>
.todo-inbox-card { display: flex; align-items: center; gap: var(--section-gap); padding: var(--panel-padding); border: 1px solid var(--color-border); border-radius: var(--radius-medium); background: var(--color-surface); }
.todo-inbox-card__body { flex: 1; min-width: 0; }
.todo-inbox-card__meta, .todo-inbox-card__schedule { display: flex; flex-wrap: wrap; align-items: center; gap: 8px var(--section-gap); font-size: 0.82rem; color: var(--color-text-muted); }
.todo-inbox-card__meta a { color: var(--color-primary); padding-block: 8px; overflow-wrap: anywhere; }
.todo-inbox-card__badge { padding: 4px 10px; border-radius: var(--radius-pill); background: var(--color-surface-muted); }
.todo-inbox-card__badge--overdue { color: var(--color-danger); background: var(--color-danger-soft); }
.todo-inbox-card__badge--completed { color: var(--color-success); background: var(--color-success-soft); }
.todo-inbox-card__badge--today { color: var(--color-primary); background: var(--color-primary-soft); }
.todo-inbox-card__title { display: block; min-height: 44px; padding: 4px 0; border: 0; background: none; font: inherit; font-size: 1.08rem; font-weight: 650; text-align: left; color: var(--color-text); cursor: pointer; overflow-wrap: anywhere; }
.todo-inbox-card--done .todo-inbox-card__title { color: var(--color-text-muted); }
.todo-inbox-card__parent, .todo-inbox-card__description { margin: 4px 0 10px; font-size: 0.9rem; color: var(--color-text-muted); overflow-wrap: anywhere; }
.todo-inbox-card__description { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; white-space: pre-wrap; }
.todo-inbox-card__actions { display: flex; gap: 8px; flex: none; }
@media (max-width: 767px) {
  .todo-inbox-card { flex-direction: column; align-items: stretch; gap: var(--section-gap); padding: var(--mobile-panel-padding); }
  .todo-inbox-card__actions > * { flex: 1; }
}
</style>
