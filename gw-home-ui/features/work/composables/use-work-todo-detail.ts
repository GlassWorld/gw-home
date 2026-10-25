import type { SaveWorkTodoPayload, WorkTodo, WorkTodoTree, WorkUnit } from '../types/work.types'
import { useWorkTodoApi } from '../api/work-todo.api'
import { useWorkUnitApi } from '../api/work-unit.api'
import { toValue, type MaybeRefOrGetter } from 'vue'

function cloneTodoPayload(payload: SaveWorkTodoPayload): SaveWorkTodoPayload {
  return {
    parentTodoUuid: payload.parentTodoUuid ?? null,
    title: payload.title,
    description: payload.description ?? '',
    startDate: payload.startDate ?? null,
    dueDate: payload.dueDate ?? null,
    sortOrder: payload.sortOrder ?? null
  }
}

function createEmptyPayload(parentTodoUuid: string | null = null): SaveWorkTodoPayload {
  return {
    parentTodoUuid,
    title: '',
    description: '',
    startDate: null,
    dueDate: null,
    sortOrder: null
  }
}

function flattenTodos(todos: WorkTodo[]): WorkTodo[] {
  const flattened: WorkTodo[] = []

  for (const todo of todos) {
    flattened.push(todo)
    flattened.push(...flattenTodos(todo.children))
  }

  return flattened
}

export function useWorkTodoDetail(workUnitUuid: MaybeRefOrGetter<string>, onChanged?: () => Promise<void>) {
  const { fetchWorkUnit } = useWorkUnitApi()
  const { fetchWorkTodoTree, createWorkTodo, updateWorkTodo, updateWorkTodoCompletion, deleteWorkTodo } = useWorkTodoApi()
  const { showToast } = useToast()
  const { confirm } = useDialog()

  const workUnit = ref<WorkUnit | null>(null)
  const todoTree = ref<WorkTodoTree | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const hideCompleted = ref(false)
  const collapsedTodoMap = ref<Record<string, boolean>>({})
  const isFormVisible = ref(false)
  const editingTodoUuid = ref('')
  const formParentTitle = ref('')
  const formState = ref<SaveWorkTodoPayload>(createEmptyPayload())

  const filteredTodos = computed(() => {
    const applyFilter = (todos: WorkTodo[]): WorkTodo[] => {
      return todos.flatMap((todo) => {
        const filteredChildren = applyFilter(todo.children)

        if (hideCompleted.value && todo.completed && filteredChildren.length === 0) {
          return []
        }

        return [{ ...todo, children: filteredChildren }]
      })
    }

    return applyFilter(todoTree.value?.todos ?? [])
  })

  const allTodos = computed(() => flattenTodos(todoTree.value?.todos ?? []))

  function getTodoByUuid(todoUuid: string): WorkTodo | null {
    return allTodos.value.find((todo) => todo.todoUuid === todoUuid) ?? null
  }

  function resetForm(parentTodoUuid: string | null = null, parentTitle = '') {
    editingTodoUuid.value = ''
    formParentTitle.value = parentTitle
    formState.value = createEmptyPayload(parentTodoUuid)
  }

  function startCreateRootTodo() {
    resetForm()
    isFormVisible.value = true
  }

  function startCreateChildTodo(parentTodo: WorkTodo) {
    resetForm(parentTodo.todoUuid, parentTodo.title)
    isFormVisible.value = true
  }

  function startEditTodo(todo: WorkTodo) {
    editingTodoUuid.value = todo.todoUuid
    formParentTitle.value = todo.parentTodoUuid ? getTodoByUuid(todo.parentTodoUuid)?.title ?? '' : ''
    formState.value = cloneTodoPayload({
      parentTodoUuid: todo.parentTodoUuid,
      title: todo.title,
      description: todo.description ?? '',
      startDate: todo.startDate,
      dueDate: todo.dueDate,
      sortOrder: todo.sortOrder
    })
    isFormVisible.value = true
  }

  function closeForm() {
    resetForm()
    isFormVisible.value = false
  }

  function toggleCollapsed(todoUuid: string) {
    collapsedTodoMap.value = {
      ...collapsedTodoMap.value,
      [todoUuid]: !collapsedTodoMap.value[todoUuid]
    }
  }

  function collapseAll() {
    const nextState: Record<string, boolean> = {}
    for (const todo of allTodos.value) {
      if (todo.children.length) {
        nextState[todo.todoUuid] = true
      }
    }
    collapsedTodoMap.value = nextState
  }

  function expandAll() {
    collapsedTodoMap.value = {}
  }

  async function loadPage() {
    isLoading.value = true

    try {
      const [loadedWorkUnit, loadedTodoTree] = await Promise.all([
        fetchWorkUnit(toValue(workUnitUuid)),
        fetchWorkTodoTree(toValue(workUnitUuid))
      ])

      workUnit.value = loadedWorkUnit
      todoTree.value = loadedTodoTree
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '업무 세부 작업 정보를 불러오지 못했습니다.', { variant: 'error' })
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function handleSubmit() {
    if (isSaving.value) {
      return
    }

    if (!formState.value.title.trim()) {
      showToast('TODO 제목을 입력해주세요.', { variant: 'error' })
      return
    }

    if (!toValue(workUnitUuid)) {
      showToast('소속 업무를 선택해주세요.', { variant: 'error' })
      return
    }
    if (formState.value.startDate && formState.value.dueDate && formState.value.startDate > formState.value.dueDate) {
      showToast('마감일은 시작일보다 빠를 수 없습니다.', { variant: 'error' })
      return
    }

    isSaving.value = true

    const payload: SaveWorkTodoPayload = {
      parentTodoUuid: formState.value.parentTodoUuid ?? null,
      title: formState.value.title.trim(),
      description: formState.value.description?.trim() || '',
      startDate: formState.value.startDate || null,
      dueDate: formState.value.dueDate || null,
      sortOrder: formState.value.sortOrder ?? null
    }

    try {
      todoTree.value = editingTodoUuid.value
        ? await updateWorkTodo(toValue(workUnitUuid), editingTodoUuid.value, payload)
        : await createWorkTodo(toValue(workUnitUuid), payload)

      showToast(editingTodoUuid.value ? 'TODO를 수정했습니다.' : 'TODO를 추가했습니다.', { variant: 'success' })
      closeForm()
      await onChanged?.()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'TODO 저장에 실패했습니다.', { variant: 'error' })
    } finally {
      isSaving.value = false
    }
  }

  async function handleToggleCompletion(todo: WorkTodo, completed = !todo.completed) {
    if (isSaving.value || !todo.leaf) {
      return
    }

    isSaving.value = true

    try {
      todoTree.value = await updateWorkTodoCompletion(toValue(workUnitUuid), todo.todoUuid, completed)
      showToast(completed ? 'TODO를 완료 처리했습니다.' : 'TODO를 미완료로 되돌렸습니다.', { variant: 'success' })
      await onChanged?.()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'TODO 완료 상태 변경에 실패했습니다.', { variant: 'error' })
    } finally {
      isSaving.value = false
    }
  }

  async function handleDeleteTodo(todo: WorkTodo) {
    if (isSaving.value) {
      return
    }

    const shouldDelete = await confirm(
      todo.children.length ? '이 TODO를 삭제하면 하위 TODO도 함께 삭제됩니다. 계속할까요?' : '이 TODO를 삭제할까요?',
      {
        title: 'TODO 삭제',
        confirmText: '삭제',
        cancelText: '취소'
      }
    )

    if (!shouldDelete) {
      return
    }

    isSaving.value = true

    try {
      todoTree.value = await deleteWorkTodo(toValue(workUnitUuid), todo.todoUuid)
      showToast('TODO를 삭제했습니다.', { variant: 'success' })
      await onChanged?.()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'TODO 삭제에 실패했습니다.', { variant: 'error' })
    } finally {
      isSaving.value = false
    }
  }

  return {
    getTodoByUuid,
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
  }
}
