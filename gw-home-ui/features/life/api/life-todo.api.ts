import type { ApiResponse, PageResponse } from '~/types/api/common'
import type { LifeCategory, LifeTodo, LifeTodoForm, LifeTodoParams, LifeTodoSummary } from '../types/life-todo.types'

interface CategoryResponse { category_uuid: string; name: string }
interface TodoResponse {
  todo_uuid: string; title: string; memo: string | null; category_uuid: string | null; category_name: string | null
  due_date: string | null; completed: boolean; completed_at: string | null; group: LifeTodo['group']; created_at: string; updated_at: string
}
interface SummaryResponse {
  total_count: number; today_count: number; upcoming_count: number; overdue_count: number
  completed_count: number; unscheduled_count: number; progress_rate: number; as_of_date: string
}
interface TodoPageResponse { content: TodoResponse[]; page: number; size: number; total_count: number; total_pages: number }

// 일상 일정의 오늘 분류는 기기의 시간대로 서버에서 계산한다.
function timeZone(): string { return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Seoul' }

// 실패 응답을 빈 목록이나 저장 성공으로 취급하지 않는다.
function readResponse<T>(response: ApiResponse<T>): T {
  if (!response.success || response.data == null) throw new Error(response.message || '일상 할 일 요청에 실패했습니다.')
  return response.data
}

function toTodo(todo: TodoResponse): LifeTodo {
  return { todoUuid: todo.todo_uuid, title: todo.title, memo: todo.memo, categoryUuid: todo.category_uuid,
    categoryName: todo.category_name, dueDate: todo.due_date, completed: todo.completed, completedAt: todo.completed_at,
    group: todo.group, createdAt: todo.created_at, updatedAt: todo.updated_at }
}

export function lifeTodoErrorMessage(error: unknown, fallback: string): string {
  const response = error as { data?: { message?: string } }
  return response?.data?.message || fallback
}

export function useLifeTodoApi() {
  const { authorizedFetch } = useAuth()
  const base = '/api/v1/life-todos'

  async function fetchTodos(params: LifeTodoParams = {}): Promise<PageResponse<LifeTodo>> {
    const data = readResponse(await authorizedFetch<ApiResponse<TodoPageResponse>>(base, { method: 'GET',
      query: { ...params, uncategorized: String(params.uncategorized ?? false), timeZone: timeZone() } }))
    return { content: data.content.map(toTodo), page: data.page, size: data.size, totalCount: data.total_count, totalPages: data.total_pages }
  }

  async function fetchSummary(params: LifeTodoParams = {}): Promise<LifeTodoSummary> {
    const data = readResponse(await authorizedFetch<ApiResponse<SummaryResponse>>(`${base}/summary`, { method: 'GET',
      query: { ...params, uncategorized: String(params.uncategorized ?? false), timeZone: timeZone() } }))
    return { totalCount: data.total_count, todayCount: data.today_count, upcomingCount: data.upcoming_count,
      overdueCount: data.overdue_count, completedCount: data.completed_count, unscheduledCount: data.unscheduled_count,
      progressRate: data.progress_rate, asOfDate: data.as_of_date }
  }

  async function saveTodo(form: LifeTodoForm, uuid?: string): Promise<LifeTodo> {
    return toTodo(readResponse(await authorizedFetch<ApiResponse<TodoResponse>>(uuid ? `${base}/${uuid}` : base, {
      method: uuid ? 'PUT' : 'POST', query: { timeZone: timeZone() },
      body: { title: form.title.trim(), memo: form.memo.trim() || null, category_uuid: form.categoryUuid || null, due_date: form.dueDate || null }
    })))
  }

  async function completeTodo(todo: LifeTodo): Promise<LifeTodo> {
    return toTodo(readResponse(await authorizedFetch<ApiResponse<TodoResponse>>(`${base}/${todo.todoUuid}/completion`, {
      method: 'PUT', query: { timeZone: timeZone() }, body: { completed: !todo.completed }
    })))
  }

  async function remove(path: string): Promise<void> {
    const response = await authorizedFetch<ApiResponse<null>>(path, { method: 'DELETE' })
    if (!response.success) throw new Error(response.message || '삭제하지 못했습니다.')
  }

  async function fetchCategories(): Promise<LifeCategory[]> {
    return readResponse(await authorizedFetch<ApiResponse<CategoryResponse[]>>(`${base}/categories`, { method: 'GET' }))
      .map(category => ({ categoryUuid: category.category_uuid, name: category.name }))
  }

  async function saveCategory(name: string, uuid?: string): Promise<LifeCategory> {
    const category = readResponse(await authorizedFetch<ApiResponse<CategoryResponse>>(`${base}/categories${uuid ? `/${uuid}` : ''}`, {
      method: uuid ? 'PUT' : 'POST', body: { name: name.trim() }
    }))
    return { categoryUuid: category.category_uuid, name: category.name }
  }

  return { fetchTodos, fetchSummary, saveTodo, completeTodo, fetchCategories, saveCategory,
    deleteTodo: (uuid: string) => remove(`${base}/${uuid}`), deleteCategory: (uuid: string) => remove(`${base}/categories/${uuid}`) }
}
