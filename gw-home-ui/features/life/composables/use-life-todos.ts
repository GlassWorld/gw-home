import { useLifeTodoApi } from '~/composables/use-life-todo-api'
import { lifeTodoErrorMessage } from '../api/life-todo.api'
import { lifeTodoGroups } from '../types/life-todo.types'
import type { LifeCategory, LifeTodo, LifeTodoForm, LifeTodoGroup, LifeTodoParams, LifeTodoSummary } from '../types/life-todo.types'
import type { PageResponse } from '~/types/api/common'

export function useLifeTodos() {
  const api = useLifeTodoApi()
  const { showToast } = useToast()
  const route = useRoute()
  const initialGroup = lifeTodoGroups.find(item => item.value === route.query.group)?.value ?? 'ALL'
  const group = ref<LifeTodoGroup>(initialGroup)
  const filters = reactive({ keyword: '', category: '' })
  const appliedFilters = ref({ ...filters })
  const page = ref(1)
  const result = ref<PageResponse<LifeTodo> | null>(null)
  const summary = ref<LifeTodoSummary | null>(null)
  const categories = ref<LifeCategory[]>([])
  const isLoading = ref(false)
  const isSaving = ref(false)
  const isCategoriesLoading = ref(false)
  const errorMessage = ref('')
  const categoriesError = ref('')
  const formError = ref('')
  const formVisible = ref(false)
  const categoriesVisible = ref(false)
  const editingUuid = ref('')
  const deleteTarget = ref<LifeTodo | null>(null)
  const form = ref<LifeTodoForm>({ title: '', memo: '', categoryUuid: '', dueDate: '' })
  const isBusy = computed(() => isLoading.value || isSaving.value)
  let sequence = 0

  // 검색·카테고리 조건을 날짜별 분류와 동일하게 목록·집계에 적용한다.
  async function refresh() {
    const request = ++sequence
    isLoading.value = true
    errorMessage.value = ''
    const category = appliedFilters.value.category
    const params: LifeTodoParams = { keyword: appliedFilters.value.keyword || undefined,
      categoryUuid: category && category !== 'UNCATEGORIZED' ? category : undefined,
      uncategorized: category === 'UNCATEGORIZED', group: group.value, page: page.value, size: 20 }
    try {
      const [nextResult, nextSummary] = await Promise.all([api.fetchTodos(params), api.fetchSummary(params)])
      if (request !== sequence) return
      if (page.value > Math.max(1, nextResult.totalPages)) {
        page.value = Math.max(1, nextResult.totalPages)
        await refresh()
        return
      }
      result.value = nextResult
      summary.value = nextSummary
    } catch {
      if (request !== sequence) return
      result.value = null
      summary.value = null
      errorMessage.value = '일상 할 일을 불러오지 못했습니다. 다시 시도해주세요.'
    } finally { if (request === sequence) isLoading.value = false }
  }

  async function loadCategories() {
    isCategoriesLoading.value = true
    categoriesError.value = ''
    try { categories.value = await api.fetchCategories() }
    catch { categoriesError.value = '생활 카테고리를 불러오지 못했습니다.' }
    finally { isCategoriesLoading.value = false }
  }

  function applyFilters() {
    if (isBusy.value) return
    appliedFilters.value = { ...filters }
    page.value = 1
    void refresh()
  }

  function selectGroup(value: LifeTodoGroup) {
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

  function openForm(todo?: LifeTodo) {
    if (isBusy.value) return
    editingUuid.value = todo?.todoUuid ?? ''
    formError.value = ''
    const selectedCategory = categories.value.find(category => category.categoryUuid === appliedFilters.value.category)?.categoryUuid ?? ''
    form.value = todo ? { title: todo.title, memo: todo.memo ?? '', categoryUuid: todo.categoryUuid ?? '', dueDate: todo.dueDate ?? '' }
      : { title: '', memo: '', categoryUuid: selectedCategory, dueDate: '' }
    formVisible.value = true
  }

  function closeForm() { if (!isSaving.value) formVisible.value = false }

  async function saveTodo() {
    if (isSaving.value) return
    if (!form.value.title.trim()) { formError.value = '제목을 입력해주세요.'; return }
    isSaving.value = true
    formError.value = ''
    try {
      await api.saveTodo(form.value, editingUuid.value || undefined)
      formVisible.value = false
      showToast(editingUuid.value ? '일상 할 일을 수정했습니다.' : '일상 할 일을 추가했습니다.', { variant: 'success' })
      await refresh()
    } catch (error) { formError.value = lifeTodoErrorMessage(error, '저장하지 못했습니다. 입력 내용은 유지됩니다.') }
    finally { isSaving.value = false }
  }

  async function toggleCompletion(todo: LifeTodo) {
    if (isBusy.value) return
    isSaving.value = true
    try {
      await api.completeTodo(todo)
      showToast(todo.completed ? '완료를 취소했습니다.' : '일상 할 일을 완료했습니다.', { variant: 'success' })
      await refresh()
    } catch (error) { showToast(lifeTodoErrorMessage(error, '완료 상태를 변경하지 못했습니다.'), { variant: 'error' }) }
    finally { isSaving.value = false }
  }

  async function removeTodo() {
    if (isSaving.value || !deleteTarget.value) return
    isSaving.value = true
    try {
      await api.deleteTodo(deleteTarget.value.todoUuid)
      deleteTarget.value = null
      showToast('일상 할 일을 삭제했습니다.', { variant: 'success' })
      await refresh()
    } catch (error) { showToast(lifeTodoErrorMessage(error, '일상 할 일을 삭제하지 못했습니다.'), { variant: 'error' }) }
    finally { isSaving.value = false }
  }

  // 삭제된 카테고리 필터는 전체로 되돌리고 기존 항목의 미분류 전환을 반영한다.
  async function categoriesChanged() {
    await loadCategories()
    if (!categoriesError.value && appliedFilters.value.category && appliedFilters.value.category !== 'UNCATEGORIZED'
      && !categories.value.some(category => category.categoryUuid === appliedFilters.value.category)) {
      filters.category = ''
      appliedFilters.value.category = ''
      page.value = 1
    }
    await refresh()
  }

  function refreshOnFocus() {
    if (!isBusy.value && !formVisible.value && !categoriesVisible.value && !deleteTarget.value) {
      void loadCategories()
      void refresh()
    }
  }
  onMounted(() => { void refresh(); void loadCategories(); window.addEventListener('focus', refreshOnFocus) })
  onBeforeUnmount(() => { sequence++; window.removeEventListener('focus', refreshOnFocus) })

  return { group, filters, page, result, summary, categories, isLoading, isSaving, isBusy, isCategoriesLoading,
    errorMessage, categoriesError, formError, formVisible, categoriesVisible, editingUuid, deleteTarget, form,
    refresh, loadCategories, applyFilters, selectGroup, changePage, openForm, closeForm, saveTodo, toggleCompletion, removeTodo, categoriesChanged }
}
