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
            :aria-expanded="!isCollapsed"
            @click.stop="emit('toggleCollapse', todo.todoUuid)"
          >
            {{ isCollapsed ? '펼치기' : '접기' }}
          </button>
          <span v-else class="work-todo-item__toggle work-todo-item__toggle--ghost">-</span>

          <div class="work-todo-item__title-box">
            <div class="work-todo-item__title-main">
              <button class="work-todo-item__title-button" type="button" :disabled="isSaving" @click.stop="emit('openDetail', todo)">{{ todo.title }}</button>
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
  padding: 12px var(--card-padding) 12px calc(var(--card-padding) + (var(--todo-depth) * var(--panel-padding)));
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-small);
  background: var(--color-primary-soft);
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

.work-todo-item__title-button {
  background: none;
  border: 0;
  padding: 0;
  min-height: 44px;
  color: inherit;
  font: inherit;
  font-weight: 650;
  text-align: left;
  cursor: pointer;
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
  background: var(--color-surface-strong);
  overflow: hidden;
}

.work-todo-item__progress-fill {
  position: absolute;
  inset: 0 auto 0 0;
  width: var(--todo-progress-rate);
  border-radius: inherit;
  background: var(--color-success);
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
  background: var(--color-success-soft);
  color: var(--color-success);
}

.work-todo-item__status--in_progress {
  background: var(--color-primary-soft);
  color: var(--color-accent);
}

.work-todo-item__status--todo {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.work-todo-item__status--delayed {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.work-todo-item__children {
  display: grid;
  gap: 10px;
}

@media (max-width: 768px) {
  .work-todo-item__card { padding-left: calc(12px + clamp(0px, var(--todo-depth) * 10px, 30px)); }
  .work-todo-item__toggle { min-height: 44px; }
  .work-todo-item__title-button { white-space: normal; overflow-wrap: anywhere; }
  .work-todo-item__card,
  .work-todo-item__main,
  .work-todo-item__title-row,
  .work-todo-item__title-main {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
