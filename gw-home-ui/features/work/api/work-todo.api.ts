import type { ApiResponse } from '~/types/api/common'
import type { SaveWorkTodoPayload, WorkTodo, WorkTodoStatus, WorkTodoSummary, WorkTodoTree } from '../types/work.types'

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

export function useWorkTodoApi() {
  const { authorizedFetch } = useAuth()

  async function fetchWorkTodoTree(workUnitUuid: string): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos`, {
      method: 'GET'
    })

    return toTree(response.data)
  }

  async function createWorkTodo(workUnitUuid: string, payload: SaveWorkTodoPayload): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos`, {
      method: 'POST',
      body: toRequestBody(payload)
    })

    return toTree(response.data)
  }

  async function updateWorkTodo(workUnitUuid: string, todoUuid: string, payload: SaveWorkTodoPayload): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos/${todoUuid}`, {
      method: 'PUT',
      body: toRequestBody(payload)
    })

    return toTree(response.data)
  }

  async function updateWorkTodoCompletion(workUnitUuid: string, todoUuid: string, completed: boolean): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos/${todoUuid}/completion`, {
      method: 'PUT',
      body: { completed }
    })

    return toTree(response.data)
  }

  async function deleteWorkTodo(workUnitUuid: string, todoUuid: string): Promise<WorkTodoTree> {
    const response = await authorizedFetch<ApiResponse<WorkTodoTreeApi>>(`/api/v1/work-units/${workUnitUuid}/todos/${todoUuid}`, {
      method: 'DELETE'
    })

    return toTree(response.data)
  }

  return {
    fetchWorkTodoTree,
    createWorkTodo,
    updateWorkTodo,
    updateWorkTodoCompletion,
    deleteWorkTodo
  }
}
