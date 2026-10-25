import type { ApiResponse, PageResponse } from '~/types/api/common'
import type { SaveWorkTodoPayload, WorkTodo, WorkTodoStatus, WorkTodoSummary, WorkTodoTree, WorkTodoListItem, WorkTodoListParams, WorkTodoInboxSummary, WorkTodoGroup } from '../types/work.types'
import { getWorkTodoTimeZone } from '../utils/work-todo'

interface WorkTodoSummaryApi {
  status?: string
  progressRate?: number
  progress_rate?: number
  itemCount?: number
  item_count?: number
  leafCount?: number
  leaf_count?: number
  completedLeafCount?: number
  completed_leaf_count?: number
  delayedCount?: number
  delayed_count?: number
}

interface WorkTodoApi {
  todoUuid?: string
  todo_uuid?: string
  parentTodoUuid?: string | null
  parent_todo_uuid?: string | null
  title?: string
  description?: string | null
  status?: string
  progressRate?: number
  progress_rate?: number
  startDate?: string | null
  start_date?: string | null
  dueDate?: string | null
  due_date?: string | null
  sortOrder?: number
  sort_order?: number
  depth?: number
  leaf?: boolean
  completed?: boolean
  completedAt?: string | null
  completed_at?: string | null
  delayed?: boolean
  createdAt?: string
  created_at?: string
  updatedAt?: string
  updated_at?: string
  children?: WorkTodoApi[]
}

interface WorkTodoTreeApi {
  workUnitUuid?: string
  work_unit_uuid?: string
  summary?: WorkTodoSummaryApi
  todos?: WorkTodoApi[]
}

function normalizeStatus(status: string | undefined): WorkTodoStatus {
  if (status === 'IN_PROGRESS' || status === 'DONE' || status === 'DELAYED') {
    return status
  }

  return 'TODO'
}

function toSummary(summary: WorkTodoSummaryApi | undefined): WorkTodoSummary {
  return {
    status: normalizeStatus(summary?.status),
    progressRate: Number(summary?.progressRate ?? summary?.progress_rate ?? 0),
    itemCount: Number(summary?.itemCount ?? summary?.item_count ?? 0),
    leafCount: Number(summary?.leafCount ?? summary?.leaf_count ?? 0),
    completedLeafCount: Number(summary?.completedLeafCount ?? summary?.completed_leaf_count ?? 0),
    delayedCount: Number(summary?.delayedCount ?? summary?.delayed_count ?? 0)
  }
}

function toTodo(todo: WorkTodoApi): WorkTodo {
  return {
    todoUuid: todo.todoUuid ?? todo.todo_uuid ?? '',
    parentTodoUuid: todo.parentTodoUuid ?? todo.parent_todo_uuid ?? null,
    title: todo.title ?? '',
    description: todo.description ?? null,
    status: normalizeStatus(todo.status),
    progressRate: Number(todo.progressRate ?? todo.progress_rate ?? 0),
    startDate: todo.startDate ?? todo.start_date ?? null,
    dueDate: todo.dueDate ?? todo.due_date ?? null,
    sortOrder: Number(todo.sortOrder ?? todo.sort_order ?? 0),
    depth: Number(todo.depth ?? 0),
    leaf: Boolean(todo.leaf),
    completed: Boolean(todo.completed),
    completedAt: todo.completedAt ?? todo.completed_at ?? null,
    delayed: Boolean(todo.delayed),
    createdAt: todo.createdAt ?? todo.created_at ?? '',
    updatedAt: todo.updatedAt ?? todo.updated_at ?? '',
    children: (todo.children ?? []).map(toTodo)
  }
}

function toTree(tree: WorkTodoTreeApi): WorkTodoTree {
  return {
    workUnitUuid: tree.workUnitUuid ?? tree.work_unit_uuid ?? '',
    summary: toSummary(tree.summary),
    todos: (tree.todos ?? []).map(toTodo)
  }
}

function toRequestBody(payload: SaveWorkTodoPayload): Record<string, number | string | null> {
  return {
    parent_todo_uuid: payload.parentTodoUuid ?? null,
    title: payload.title,
    description: payload.description?.trim() || '',
    start_date: payload.startDate || null,
    due_date: payload.dueDate || null,
    sort_order: payload.sortOrder ?? null
  }
}

function readTodoResponse<T>(response: ApiResponse<T>): T {
  if (!response.success || response.data == null) throw new Error(response.message ?? '할 일 요청에 실패했습니다.')
  return response.data
}

export function useWorkTodoApi() {
  const { authorizedFetch } = useAuth()

  async function fetchWorkTodoTree(workUnitUuid: string): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos`, {
      method: 'GET',
      query: { timeZone: getWorkTodoTimeZone() }
    })

    return toTree(readTodoResponse(response))
  }

  async function createWorkTodo(workUnitUuid: string, payload: SaveWorkTodoPayload): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos`, {
      method: 'POST',
      query: { timeZone: getWorkTodoTimeZone() },
      body: toRequestBody(payload)
    })

    return toTree(readTodoResponse(response))
  }

  async function updateWorkTodo(workUnitUuid: string, todoUuid: string, payload: SaveWorkTodoPayload): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos/${todoUuid}`, {
      method: 'PUT',
      query: { timeZone: getWorkTodoTimeZone() },
      body: toRequestBody(payload)
    })

    return toTree(readTodoResponse(response))
  }

  async function updateWorkTodoCompletion(workUnitUuid: string, todoUuid: string, completed: boolean): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos/${todoUuid}/completion`, {
      method: 'PUT',
      query: { timeZone: getWorkTodoTimeZone() },
      body: { completed }
    })

    return toTree(readTodoResponse(response))
  }

  async function deleteWorkTodo(workUnitUuid: string, todoUuid: string): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos/${todoUuid}`, {
      method: 'DELETE',
      query: { timeZone: getWorkTodoTimeZone() }
    })

    return toTree(readTodoResponse(response))
  }

  async function fetchMyWorkTodos(params: WorkTodoListParams): Promise<PageResponse<WorkTodoListItem>> {
    const response = await authorizedFetch<ApiResponse<TodoPageApi>>('/api/v1/work-todos', {
      method: 'GET', query: { ...params, includeInactive: String(params.includeInactive ?? false), timeZone: getWorkTodoTimeZone() }
    })
    const data = readTodoResponse(response)
    return {
      content: data.content.map(toListItem), page: data.page, size: data.size,
      totalCount: data.totalCount ?? data.total_count ?? 0,
      totalPages: data.totalPages ?? data.total_pages ?? 0
    }
  }

  async function fetchMyWorkTodoSummary(params: WorkTodoListParams = {}): Promise<WorkTodoInboxSummary> {
    const response = await authorizedFetch<ApiResponse<TodoInboxSummaryApi>>('/api/v1/work-todos/summary', {
      method: 'GET', query: { ...params, includeInactive: String(params.includeInactive ?? false), timeZone: getWorkTodoTimeZone() }
    })
    const data = readTodoResponse(response)
    return {
      totalCount: data.totalCount ?? data.total_count ?? 0,
      todayCount: data.todayCount ?? data.today_count ?? 0,
      upcomingCount: data.upcomingCount ?? data.upcoming_count ?? 0,
      overdueCount: data.overdueCount ?? data.overdue_count ?? 0,
      completedCount: data.completedCount ?? data.completed_count ?? 0,
      unscheduledCount: data.unscheduledCount ?? data.unscheduled_count ?? 0,
      unknownCompletionCount: data.unknownCompletionCount ?? data.unknown_completion_count ?? 0,
      progressRate: data.progressRate ?? data.progress_rate ?? 0,
      asOfDate: data.asOfDate ?? data.as_of_date ?? '',
      timeZone: data.timeZone ?? data.time_zone ?? getWorkTodoTimeZone()
    }
  }

  return {
    fetchMyWorkTodos,
    fetchMyWorkTodoSummary,
    fetchWorkTodoTree,
    createWorkTodo,
    updateWorkTodo,
    updateWorkTodoCompletion,
    deleteWorkTodo
  }
}

interface TodoListApi extends WorkTodoApi {
  workUnitUuid?: string
  work_unit_uuid?: string
  workUnitTitle?: string
  work_unit_title?: string
  workUnitActive?: boolean
  work_unit_active?: boolean
  parentTitle?: string | null
  parent_title?: string | null
  group: WorkTodoGroup
}

interface TodoPageApi {
  content: TodoListApi[]
  page: number
  size: number
  totalCount?: number
  total_count?: number
  totalPages?: number
  total_pages?: number
}

interface TodoInboxSummaryApi extends Partial<WorkTodoInboxSummary> {
  total_count?: number
  today_count?: number
  upcoming_count?: number
  overdue_count?: number
  completed_count?: number
  unscheduled_count?: number
  unknown_completion_count?: number
  progress_rate?: number
  as_of_date?: string
  time_zone?: string
}

function toListItem(todo: TodoListApi): WorkTodoListItem {
  const node = toTodo(todo)
  return {
    todoUuid: node.todoUuid, title: node.title, description: node.description,
    parentTodoUuid: node.parentTodoUuid, parentTitle: todo.parentTitle ?? todo.parent_title ?? null,
    workUnitUuid: todo.workUnitUuid ?? todo.work_unit_uuid ?? '',
    workUnitTitle: todo.workUnitTitle ?? todo.work_unit_title ?? '',
    workUnitActive: todo.workUnitActive ?? todo.work_unit_active ?? true,
    startDate: node.startDate, dueDate: node.dueDate, sortOrder: node.sortOrder,
    completed: node.completed, completedAt: node.completedAt, group: todo.group
  }
}
