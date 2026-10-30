import { useLifeDailyApi } from '../api/life-daily.api'
import { lifeTodoErrorMessage } from '../api/life-todo.api'
import type { LifeDailyItem, LifeDailyMonth, LifeDailyDay } from '../types/life-daily.types'

// UTC 변환 없이 기기의 현지 달력 날짜를 사용한다.
function localDate(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

export function useLifeDaily() {
  const api = useLifeDailyApi()
  const { showToast } = useToast()
  const today = ref(localDate())
  const selectedDate = ref(today.value)
  const month = ref(today.value.slice(0, 7))
  const result = ref<LifeDailyMonth | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const errorMessage = ref('')
  const saveError = ref('')
  const title = ref('')
  const editingItem = ref<LifeDailyItem | null>(null)
  const editingTitle = ref('')
  const deleteTarget = ref<LifeDailyItem | null>(null)
  const busy = computed(() => isLoading.value || isSaving.value)
  let sequence = 0

  const itemsForDate = (date: string) => (result.value?.items ?? []).filter(item => item.startDate <= date && (!item.endDate || item.endDate >= date))
  const selectedItems = computed(() => itemsForDate(selectedDate.value))
  const isCompleted = (uuid: string, date = selectedDate.value) => result.value?.checks.some(check => check.itemUuid === uuid && check.date === date) ?? false
  const completedCount = computed(() => selectedItems.value.filter(item => isCompleted(item.itemUuid)).length)
  const days = computed<LifeDailyDay[]>(() => {
    const [year, monthNumber] = month.value.split('-').map(Number)
    const count = new Date(year!, monthNumber!, 0).getDate()
    return Array.from({ length: count }, (_, index) => {
      const date = `${month.value}-${String(index + 1).padStart(2, '0')}`
      const items = itemsForDate(date)
      return { date, day: index + 1, total: items.length, completed: items.filter(item => isCompleted(item.itemUuid, date)).length }
    })
  })

  // 늦게 도착한 이전 달 응답은 현재 화면을 덮어쓰지 않는다.
  async function refresh() {
    const request = ++sequence
    isLoading.value = true
    errorMessage.value = ''
    try {
      const data = await api.fetchMonth(month.value)
      if (request !== sequence) return
      result.value = data
      today.value = data.today
    } catch (error) {
      if (request === sequence) errorMessage.value = lifeTodoErrorMessage(error, '매일 체크를 불러오지 못했습니다.')
    } finally { if (request === sequence) isLoading.value = false }
  }

  function moveMonth(offset: number) {
    if (isSaving.value) return
    const [year, monthNumber] = month.value.split('-').map(Number)
    const next = new Date(year!, monthNumber! - 1 + offset, 1)
    if (next.getFullYear() < 100 || next.getFullYear() > 9999) return
    month.value = `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`
    selectedDate.value = `${month.value}-01`
    result.value = null
    saveError.value = ''
    void refresh()
  }

  function goToday() {
    if (isSaving.value) return
    today.value = localDate()
    month.value = today.value.slice(0, 7)
    selectedDate.value = today.value
    result.value = null
    saveError.value = ''
    void refresh()
  }

  // 저장 성공 후에만 체크 상태를 반영하여 실패 시 이전 값을 유지한다.
  async function toggle(item: LifeDailyItem) {
    if (busy.value || errorMessage.value || selectedDate.value > today.value) return
    const date = selectedDate.value
    const completed = !isCompleted(item.itemUuid, date)
    isSaving.value = true
    saveError.value = ''
    try {
      await api.saveCheck(item.itemUuid, date, completed)
      if (result.value) {
        result.value.checks = result.value.checks.filter(check => check.itemUuid !== item.itemUuid || check.date !== date)
        if (completed) result.value.checks.push({ itemUuid: item.itemUuid, date })
      }
      showToast(completed ? '완료로 체크했습니다.' : '완료 체크를 취소했습니다.')
    } catch (error) { saveError.value = lifeTodoErrorMessage(error, '체크를 저장하지 못했습니다. 다시 시도해주세요.') }
    finally { isSaving.value = false }
  }

  async function saveItem() {
    if (busy.value) return
    const value = (editingItem.value ? editingTitle.value : title.value).trim()
    if (!value || value.length > 200) { saveError.value = '제목을 1~200자로 입력해주세요.'; return }
    isSaving.value = true
    saveError.value = ''
    try {
      if (editingItem.value) await api.renameItem(editingItem.value.itemUuid, value)
      else await api.createItem(value)
      const created = !editingItem.value
      title.value = ''
      editingItem.value = null
      if (created) { today.value = localDate(); month.value = today.value.slice(0, 7); selectedDate.value = today.value }
      showToast(created ? '오늘부터 매일 체크할 일을 추가했습니다.' : '제목을 수정했습니다.')
      await refresh()
    } catch (error) { saveError.value = lifeTodoErrorMessage(error, '항목을 저장하지 못했습니다. 입력 내용을 확인하고 다시 시도해주세요.') }
    finally { isSaving.value = false }
  }

  async function deleteItem() {
    if (busy.value || !deleteTarget.value) return
    isSaving.value = true
    saveError.value = ''
    try {
      await api.deleteItem(deleteTarget.value.itemUuid)
      deleteTarget.value = null
      showToast('항목과 체크 기록을 삭제했습니다.')
      await refresh()
    } catch (error) { saveError.value = lifeTodoErrorMessage(error, '항목을 삭제하지 못했습니다.') }
    finally { isSaving.value = false }
  }

  function openEdit(item: LifeDailyItem) { editingItem.value = item; editingTitle.value = item.title; saveError.value = '' }
  onMounted(refresh)
  onBeforeUnmount(() => { sequence++ })
  return { today, selectedDate, month, result, days, selectedItems, completedCount, isCompleted, isLoading, isSaving, busy,
    errorMessage, saveError, title, editingItem, editingTitle, deleteTarget, refresh, moveMonth, goToday, toggle, saveItem, deleteItem, openEdit }
}
