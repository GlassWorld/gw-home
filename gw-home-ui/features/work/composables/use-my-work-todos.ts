import type { PageResponse } from '~/types/api/common'
import type { WorkTodo, WorkTodoGroup, WorkTodoInboxSummary, WorkTodoListItem, WorkTodoListParams, WorkUnitOption } from '../types/work.types'
import { useWorkTodoApi } from '../api/work-todo.api'
import { useWorkUnitApi } from '../api/work-unit.api'
import { useWorkTodoDetail } from './use-work-todo-detail'
import { workTodoGroups } from '../utils/work-todo'

export function useMyWorkTodos() {
  const { fetchMyWorkTodos, fetchMyWorkTodoSummary } = useWorkTodoApi()
  const { fetchWorkUnitOptions } = useWorkUnitApi()
  const filters = reactive({ keyword: '', workUnitUuid: '', includeInactive: false, fromDate: '', toDate: '' })
  const appliedFilters = ref({ ...filters })
  const route = useRoute()
  const group = ref<WorkTodoGroup>(workTodoGroups.find(item => item.value === route.query.group)?.value ?? 'ALL')
  const page = ref(1)
  const result = ref<PageResponse<WorkTodoListItem> | null>(null)
  const summary = ref<WorkTodoInboxSummary | null>(null)
  const activeFilterCount = computed(() => Object.values(appliedFilters.value).filter(Boolean).length)
  const workUnits = ref<WorkUnitOption[]>([])
  const isLoading = ref(false)
  const errorMessage = ref('')
  const optionsError = ref('')
  const activeWorkUnitUuid = ref('')
  const selectedTodoUuid = ref('')
  let requestSequence = 0

  // 쓰기는 기존 트리 편집기를 공유하고 성공한 뒤 통합 목록만 다시 조회한다.
  const editor = useWorkTodoDetail(activeWorkUnitUuid, refresh)
  const selectedTodo = computed(() => editor.getTodoByUuid(selectedTodoUuid.value))
  const isBusy = computed(() => isLoading.value || editor.isLoading.value || editor.isSaving.value)
  const availableWorkUnits = computed(() => workUnits.value.filter(unit => filters.includeInactive || unit.useYn === 'Y'))
  const createWorkUnits = computed(() => workUnits.value.filter(unit => unit.useYn === 'Y' || unit.workUnitUuid === activeWorkUnitUuid.value))

  async function refresh(): Promise<void> {
    const sequence = ++requestSequence
    isLoading.value = true
    errorMessage.value = ''
    const params: WorkTodoListParams = {
      ...appliedFilters.value, workUnitUuid: appliedFilters.value.workUnitUuid || undefined,
      fromDate: appliedFilters.value.fromDate || undefined, toDate: appliedFilters.value.toDate || undefined,
      group: group.value, page: page.value, size: 20
    }
    try {
      const [nextResult, nextSummary] = await Promise.all([fetchMyWorkTodos(params), fetchMyWorkTodoSummary(params)])
      if (sequence !== requestSequence) return
      // 완료·취소로 마지막 페이지가 비면 유효한 페이지를 다시 조회한다.
      if (page.value > Math.max(1, nextResult.totalPages)) {
        page.value = Math.max(1, nextResult.totalPages)
        await refresh()
        return
      }
      result.value = nextResult
      summary.value = nextSummary
    } catch {
      if (sequence !== requestSequence) return
      result.value = null
      summary.value = null
      errorMessage.value = '할 일을 불러오지 못했습니다. 다시 시도해주세요.'
    } finally {
      if (sequence === requestSequence) isLoading.value = false
    }
  }

  async function loadOptions() {
    optionsError.value = ''
    try { workUnits.value = await fetchWorkUnitOptions(true) }
    catch { optionsError.value = '업무 선택 목록을 불러오지 못했습니다.' }
  }

  function applyFilters() {
    if (isBusy.value) return
    if (!filters.includeInactive && !workUnits.value.some(unit => unit.workUnitUuid === filters.workUnitUuid && unit.useYn === 'Y')) filters.workUnitUuid = ''
    appliedFilters.value = { ...filters }
    page.value = 1
    void refresh()
  }

  function selectGroup(value: WorkTodoGroup) {
    if (isBusy.value) return
    group.value = value
    page.value = 1
    void refresh()
  }

  function changePage(value: number) {
    if (isBusy.value) return
    page.value = value
    void refresh()
  }

  function startCreate() {
    if (isBusy.value) return
    selectedTodoUuid.value = ''
    activeWorkUnitUuid.value = workUnits.value.find(unit => unit.workUnitUuid === appliedFilters.value.workUnitUuid && unit.useYn === 'Y')?.workUnitUuid
      ?? workUnits.value.find(unit => unit.useYn === 'Y')?.workUnitUuid ?? ''
    editor.startCreateRootTodo()
  }

  async function openTodo(item: WorkTodoListItem, action: 'detail' | 'edit' | 'complete' = 'detail') {
    if (isBusy.value) return
    selectedTodoUuid.value = ''
    activeWorkUnitUuid.value = item.workUnitUuid
    try {
      // 사용자가 선택한 업무만 읽어 최신 계층과 완료 상태를 확인한다.
      await editor.loadPage()
      const todo = editor.getTodoByUuid(item.todoUuid)
      if (!todo) { await refresh(); return }
      if (action === 'edit') editor.startEditTodo(todo)
      else if (action === 'complete') await editor.handleToggleCompletion(todo, !item.completed)
      else selectedTodoUuid.value = todo.todoUuid
    } catch { /* 기존 편집기의 오류 안내를 유지한다. */ }
  }

  function editSelected(todo: WorkTodo) {
    selectedTodoUuid.value = ''
    editor.startEditTodo(todo)
  }

  function addChild(todo: WorkTodo) {
    selectedTodoUuid.value = ''
    editor.startCreateChildTodo(todo)
  }

  function refreshOnFocus() {
    if (!isBusy.value && !editor.isFormVisible.value && !selectedTodoUuid.value) void refresh()
  }

  onMounted(() => {
    void loadOptions()
    void refresh()
    window.addEventListener('focus', refreshOnFocus)
  })
  onBeforeUnmount(() => {
    requestSequence++
    window.removeEventListener('focus', refreshOnFocus)
  })

  return {
    ...editor, filters, activeFilterCount, group, page, result, summary, workUnits, availableWorkUnits, createWorkUnits,
    isListLoading: isLoading, isBusy, errorMessage, optionsError, activeWorkUnitUuid, selectedTodoUuid, selectedTodo,
    refresh, loadOptions, applyFilters, selectGroup, changePage, startCreate, openTodo, editSelected, addChild
  }
}
