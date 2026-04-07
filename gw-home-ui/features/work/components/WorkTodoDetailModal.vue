<script setup lang="ts">
import type { WorkTodo } from '../types/work.types'

const props = defineProps<{
  visible: boolean
  todo: WorkTodo | null
  isSaving: boolean
}>()

const emit = defineEmits<{
  close: []
  addChild: [todo: WorkTodo]
  edit: [todo: WorkTodo]
  delete: [todo: WorkTodo]
  toggleCompletion: [todo: WorkTodo]
}>()

const statusLabelMap: Record<WorkTodo['status'], string> = {
  TODO: '대기',
  IN_PROGRESS: '진행중',
  DONE: '완료',
  DELAYED: '지연'
}

const scheduleText = computed(() => {
  if (!props.todo?.startDate && !props.todo?.dueDate) {
    return '-'
  }

  return [props.todo?.startDate ?? '?', props.todo?.dueDate ?? '?'].join(' ~ ')
})
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    eyebrow="Work Todo"
    :title="todo?.title ?? '세부 작업 상세'"
    width="min(760px, 92vw)"
    @close="emit('close')"
  >
    <div v-if="todo" class="work-todo-detail-modal">
      <section class="work-todo-detail-modal__summary">
        <div>
          <span>진행률</span>
          <strong>{{ todo.progressRate }}%</strong>
        </div>
        <div>
          <span>상태</span>
          <strong>{{ statusLabelMap[todo.status] }}</strong>
        </div>
        <div>
          <span>일정</span>
          <strong>{{ scheduleText }}</strong>
        </div>
        <div>
          <span>정렬 순서</span>
          <strong>{{ todo.sortOrder }}</strong>
        </div>
      </section>

      <section class="work-todo-detail-modal__panel">
        <h3>설명</h3>
        <p>{{ todo.description || '설명 없음' }}</p>
      </section>

      <section class="work-todo-detail-modal__panel">
        <h3>트리 정보</h3>
        <div class="work-todo-detail-modal__meta">
          <span><em>깊이</em> {{ todo.depth }}</span>
          <span><em>하위 항목 수</em> {{ todo.children.length }}</span>
          <span><em>직접 완료 가능</em> {{ todo.leaf ? '가능' : '불가' }}</span>
          <span><em>지연 여부</em> {{ todo.delayed ? '지연' : '정상' }}</span>
        </div>
      </section>

      <section v-if="todo.children.length" class="work-todo-detail-modal__panel">
        <h3>하위 TODO</h3>
        <ul class="work-todo-detail-modal__children">
          <li v-for="childTodo in todo.children" :key="childTodo.todoUuid">
            <strong>{{ childTodo.title }}</strong>
            <span>{{ childTodo.progressRate }}%</span>
            <span>{{ statusLabelMap[childTodo.status] }}</span>
          </li>
        </ul>
      </section>
    </div>

    <template #actions>
      <CommonBaseButton
        v-if="todo?.leaf"
        variant="secondary"
        type="button"
        :disabled="isSaving"
        @click="todo && emit('toggleCompletion', todo)"
      >
        {{ todo?.completed ? '미완료로 변경' : '완료 처리' }}
      </CommonBaseButton>
      <CommonBaseButton
        variant="secondary"
        type="button"
        :disabled="isSaving || !todo"
        @click="todo && emit('addChild', todo)"
      >
        하위 추가
      </CommonBaseButton>
      <CommonBaseButton
        variant="secondary"
        type="button"
        :disabled="isSaving || !todo"
        @click="todo && emit('edit', todo)"
      >
        수정
      </CommonBaseButton>
      <CommonBaseButton
        variant="danger"
        type="button"
        :disabled="isSaving || !todo"
        @click="todo && emit('delete', todo)"
      >
        삭제
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.work-todo-detail-modal {
  display: grid;
  gap: 16px;
}

.work-todo-detail-modal__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.work-todo-detail-modal__summary div,
.work-todo-detail-modal__panel {
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.work-todo-detail-modal__summary span {
  display: block;
  color: rgba(147, 210, 255, 0.72);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.work-todo-detail-modal__summary strong {
  display: block;
  color: var(--color-text);
  font-size: 1.02rem;
  line-height: 1.35;
}

.work-todo-detail-modal__panel h3 {
  margin: 0 0 10px;
}

.work-todo-detail-modal__panel p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.work-todo-detail-modal__meta,
.work-todo-detail-modal__children li {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--color-text);
}

.work-todo-detail-modal__meta em {
  font-style: normal;
  color: rgba(147, 210, 255, 0.72);
  font-weight: 700;
  margin-right: 4px;
}

.work-todo-detail-modal__meta span,
.work-todo-detail-modal__children li {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(8, 23, 42, 0.42);
}

.work-todo-detail-modal__children li {
  justify-content: space-between;
  gap: 12px;
}

.work-todo-detail-modal__children {
  display: grid;
  gap: 8px;
}

@media (max-width: 768px) {
  .work-todo-detail-modal__summary {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
