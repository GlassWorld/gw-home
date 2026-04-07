<script setup lang="ts">
import type { WorkTodo } from '../types/work.types'
import WorkTodoDetailModal from './WorkTodoDetailModal.vue'
import WorkTodoFormModal from './WorkTodoFormModal.vue'
import WorkTodoTreeItem from './WorkTodoTreeItem.vue'
import { useWorkTodoDetail } from '../composables/use-work-todo-detail'

const route = useRoute()
const router = useRouter()
const workUnitUuid = computed(() => String(route.params.workUnitUuid ?? ''))

const {
  workUnit,
  todoTree,
  filteredTodos,
  isLoading,
  isSaving,
  hideCompleted,
  collapsedTodoMap,
  isFormVisible,
  editingTodoUuid,
  formParentTitle,
  formState,
  startCreateRootTodo,
  startCreateChildTodo,
  startEditTodo,
  closeForm,
  toggleCollapsed,
  collapseAll,
  expandAll,
  loadPage,
  handleSubmit,
  handleToggleCompletion,
  handleDeleteTodo
} = useWorkTodoDetail(workUnitUuid.value)

const isDetailVisible = ref(false)
const selectedTodo = ref<WorkTodo | null>(null)

const statusLabelMap: Record<'TODO' | 'IN_PROGRESS' | 'DONE' | 'DELAYED', string> = {
  TODO: '대기',
  IN_PROGRESS: '진행중',
  DONE: '완료',
  DELAYED: '지연'
}

async function load() {
  try {
    await loadPage()
  } catch {
    router.push('/work/todos')
  }
}

function handleAddChild(todo: WorkTodo) {
  isDetailVisible.value = false
  startCreateChildTodo(todo)
}

function handleOpenDetail(todo: WorkTodo) {
  selectedTodo.value = todo
  isDetailVisible.value = true
}

function handleCloseDetail() {
  isDetailVisible.value = false
  selectedTodo.value = null
}

function handleEditFromDetail(todo: WorkTodo) {
  handleCloseDetail()
  startEditTodo(todo)
}

function handleDeleteFromDetail(todo: WorkTodo) {
  void handleDeleteTodo(todo)
  handleCloseDetail()
}

function handleToggleCompletionFromDetail(todo: WorkTodo) {
  void handleToggleCompletion(todo)
  handleCloseDetail()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <main class="work-todo-detail-page page-container">
    <section class="content-panel work-todo-detail-page__hero">
      <div class="work-todo-detail-page__hero-copy">
        <p class="work-todo-detail-page__eyebrow">Work Todo</p>
        <h1 class="section-title">{{ workUnit?.title ?? '업무 세부 작업' }}</h1>
        <p class="section-description">
          {{ workUnit?.description || '업무에 연결된 세부 작업 트리를 관리합니다.' }}
        </p>
      </div>

      <div class="work-todo-detail-page__hero-actions">
        <CommonBaseButton variant="secondary" to="/work/todos">
          목록으로
        </CommonBaseButton>
        <CommonBaseButton @click="startCreateRootTodo">
          루트 TODO 추가
        </CommonBaseButton>
      </div>
    </section>

    <section class="content-panel work-todo-detail-page__summary">
      <div>
        <span>전체 진행률</span>
        <strong>{{ todoTree?.summary.progressRate ?? 0 }}%</strong>
      </div>
      <div>
        <span>상태</span>
        <strong>{{ statusLabelMap[todoTree?.summary.status ?? 'TODO'] }}</strong>
      </div>
      <div>
        <span>리프 완료</span>
        <strong>{{ todoTree?.summary.completedLeafCount ?? 0 }} / {{ todoTree?.summary.leafCount ?? 0 }}</strong>
      </div>
      <div>
        <span>지연</span>
        <strong>{{ todoTree?.summary.delayedCount ?? 0 }}</strong>
      </div>
    </section>

    <section class="content-panel work-todo-detail-page__panel">
      <div class="work-todo-detail-page__panel-header">
        <div>
          <h2 class="section-title">연관 TODO 트리</h2>
          <p class="section-description">체크박스, 진행률, 상태, 일정 정보를 기준으로 업무 세부 작업을 관리합니다.</p>
        </div>

        <div class="work-todo-detail-page__controls">
          <label class="work-todo-detail-page__toggle">
            <input v-model="hideCompleted" type="checkbox">
            <span>완료 항목 숨기기</span>
          </label>
          <CommonBaseButton size="small" variant="secondary" @click="collapseAll">
            전체 접기
          </CommonBaseButton>
          <CommonBaseButton size="small" variant="secondary" @click="expandAll">
            전체 펼치기
          </CommonBaseButton>
        </div>
      </div>

      <p v-if="isLoading" class="message-muted">
        TODO 트리를 불러오는 중입니다.
      </p>

      <ul v-else-if="filteredTodos.length" class="work-todo-detail-page__tree">
        <WorkTodoTreeItem
          v-for="todo in filteredTodos"
          :key="todo.todoUuid"
          :todo="todo"
          :collapsed-todo-map="collapsedTodoMap"
          :is-saving="isSaving"
          @toggle-collapse="toggleCollapsed"
          @open-detail="handleOpenDetail"
        />
      </ul>

      <section v-else class="work-todo-detail-page__empty">
        <h3>등록된 세부 작업이 없습니다.</h3>
        <p class="section-description">루트 TODO를 추가해서 업무 상세 트리를 시작해보세요.</p>
        <CommonBaseButton @click="startCreateRootTodo">
          루트 TODO 추가
        </CommonBaseButton>
      </section>
    </section>

    <WorkTodoFormModal
      v-model="formState"
      :visible="isFormVisible"
      :is-submitting="isSaving"
      :is-editing="Boolean(editingTodoUuid)"
      :parent-title="formParentTitle"
      @submit="handleSubmit"
      @close="closeForm"
    />

    <WorkTodoDetailModal
      :visible="isDetailVisible"
      :todo="selectedTodo"
      :is-saving="isSaving"
      @close="handleCloseDetail"
      @add-child="handleAddChild"
      @edit="handleEditFromDetail"
      @delete="handleDeleteFromDetail"
      @toggle-completion="handleToggleCompletionFromDetail"
    />
  </main>
</template>

<style scoped>
.work-todo-detail-page {
  display: grid;
  gap: 24px;
}

.work-todo-detail-page__hero,
.work-todo-detail-page__summary,
.work-todo-detail-page__panel,
.work-todo-detail-page__empty {
  padding: 22px;
}

.work-todo-detail-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.work-todo-detail-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.work-todo-detail-page__hero-actions,
.work-todo-detail-page__controls {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.work-todo-detail-page__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.work-todo-detail-page__summary div {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.work-todo-detail-page__summary span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.work-todo-detail-page__summary strong {
  font-size: 1.16rem;
}

.work-todo-detail-page__panel {
  display: grid;
  gap: 18px;
}

.work-todo-detail-page__panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.work-todo-detail-page__toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-muted);
}

.work-todo-detail-page__tree {
  display: grid;
  gap: 10px;
}

.work-todo-detail-page__empty {
  display: grid;
  gap: 12px;
  justify-items: start;
  border: 1px dashed rgba(147, 210, 255, 0.22);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
}

@media (max-width: 960px) {
  .work-todo-detail-page__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .work-todo-detail-page__hero,
  .work-todo-detail-page__panel-header {
    flex-direction: column;
  }

  .work-todo-detail-page__summary {
    grid-template-columns: 1fr;
  }
}
</style>
