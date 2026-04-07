<script setup lang="ts">
import type { WorkTodo } from '../types/work.types'

defineOptions({
  name: 'WorkTodoTreeItem'
})

const props = defineProps<{
  todo: WorkTodo
  collapsedTodoMap: Record<string, boolean>
  isSaving: boolean
}>()

const emit = defineEmits<{
  toggleCollapse: [todoUuid: string]
  openDetail: [todo: WorkTodo]
}>()

const statusLabelMap: Record<WorkTodo['status'], string> = {
  TODO: '대기',
  IN_PROGRESS: '진행중',
  DONE: '완료',
  DELAYED: '지연'
}

const isCollapsed = computed(() => Boolean(props.collapsedTodoMap[props.todo.todoUuid]))
</script>

<template>
  <li class="work-todo-item">
    <article
      class="work-todo-item__card"
      :style="{ '--todo-depth': String(todo.depth), '--todo-progress-rate': `${todo.progressRate}%` }"
      @click="emit('openDetail', todo)"
    >
      <div class="work-todo-item__main">
        <div class="work-todo-item__title-row">
          <button
            v-if="todo.children.length"
            type="button"
            class="work-todo-item__toggle"
            :disabled="isSaving"
            @click.stop="emit('toggleCollapse', todo.todoUuid)"
          >
            {{ isCollapsed ? '펼치기' : '접기' }}
          </button>
          <span v-else class="work-todo-item__toggle work-todo-item__toggle--ghost">-</span>

          <div class="work-todo-item__title-box">
            <div class="work-todo-item__title-main">
              <strong>{{ todo.title }}</strong>
              <span class="work-todo-item__progress-text">{{ todo.progressRate }}%</span>
            </div>
            <div class="work-todo-item__progress-track" aria-hidden="true">
              <span class="work-todo-item__progress-fill" />
            </div>
          </div>
          <span
            class="work-todo-item__status"
            :class="`work-todo-item__status--${todo.status.toLowerCase()}`"
          >
            {{ statusLabelMap[todo.status] }}
          </span>
        </div>
      </div>
    </article>

    <ul v-if="todo.children.length && !isCollapsed" class="work-todo-item__children">
      <WorkTodoTreeItem
        v-for="childTodo in todo.children"
        :key="childTodo.todoUuid"
        :todo="childTodo"
        :collapsed-todo-map="collapsedTodoMap"
        :is-saving="isSaving"
        @toggle-collapse="emit('toggleCollapse', $event)"
        @open-detail="emit('openDetail', $event)"
      />
    </ul>
  </li>
</template>

<style scoped>
.work-todo-item {
  display: grid;
  gap: 10px;
}

.work-todo-item__card {
  display: flex;
  gap: 12px;
  padding: 12px 16px 12px calc(16px + (var(--todo-depth) * 24px));
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
}

.work-todo-item__main {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
  width: 100%;
}

.work-todo-item__title-row {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  flex: 1;
}

.work-todo-item__toggle {
  min-width: 50px;
  padding: 4px 8px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 10px;
  background: rgba(110, 193, 255, 0.08);
  color: var(--color-text);
  cursor: pointer;
}

.work-todo-item__toggle--ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  cursor: default;
}

.work-todo-item__title-box {
  display: grid;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.work-todo-item__title-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.work-todo-item__title-main strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-todo-item__progress-text {
  color: var(--color-text-muted);
  font-size: 0.86rem;
  font-weight: 700;
}

.work-todo-item__progress-track {
  position: relative;
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.work-todo-item__progress-fill {
  position: absolute;
  inset: 0 auto 0 0;
  width: var(--todo-progress-rate);
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(110, 193, 255, 0.75), rgba(42, 176, 119, 0.82));
}

.work-todo-item__status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
}

.work-todo-item__status--done {
  background: rgba(42, 176, 119, 0.16);
  color: #6fd8a3;
}

.work-todo-item__status--in_progress {
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
}

.work-todo-item__status--todo {
  background: rgba(255, 255, 255, 0.06);
  color: var(--color-text-muted);
}

.work-todo-item__status--delayed {
  background: rgba(224, 92, 92, 0.16);
  color: #ff8b8b;
}

.work-todo-item__children {
  display: grid;
  gap: 10px;
}

@media (max-width: 768px) {
  .work-todo-item__card,
  .work-todo-item__main,
  .work-todo-item__title-row,
  .work-todo-item__title-main {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
