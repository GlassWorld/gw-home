export type WorkUnitStatus = 'IN_PROGRESS' | 'DONE' | 'ON_HOLD'
export type WorkUnitSort = 'updated'
export type WorkGitProvider = 'GITLAB'
export type WorkTodoStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'DELAYED'

export interface WorkGitAccount {
  gitAccountUuid: string
  provider: WorkGitProvider
  accountLabel: string
  authorName: string
  hasAccessToken: boolean
  useYn: 'Y' | 'N'
  createdAt: string
  updatedAt: string
}

export interface SaveWorkGitAccountPayload {
  accountLabel: string
  provider: WorkGitProvider
  authorName: string
  accessToken?: string
  useYn?: 'Y' | 'N'
}

export interface WorkGitProject {
  gitProjectUuid: string
  gitAccountUuid: string
  gitAccountLabel: string
  provider: WorkGitProvider
  projectName: string
  repositoryUrl: string
  useYn: 'Y' | 'N'
  createdAt: string
  updatedAt: string
}

export interface WorkGitConnectionTestResult {
  gitProjectUuid: string
  provider: WorkGitProvider
  projectName: string
  repositoryUrl: string
  connected: boolean
  message: string
  checkedAt: string
}

export interface SaveWorkGitProjectPayload {
  gitAccountUuid: string
  projectName: string
  repositoryUrl: string
  useYn?: 'Y' | 'N'
}

export interface WorkUnit {
  workUnitUuid: string
  title: string
  description: string | null
  category: string | null
  status: WorkUnitStatus
  useYn: 'Y' | 'N'
  useCount: number
  lastUsedAt: string | null
  createdAt: string
  updatedAt: string
  gitProjects: WorkGitProject[]
}

export interface WorkUnitOption {
  workUnitUuid: string
  title: string
  category: string | null
  status: WorkUnitStatus
  useYn: 'Y' | 'N'
}

export interface SaveWorkUnitPayload {
  title: string
  category?: string
  description?: string
  status: WorkUnitStatus
  gitProjectUuids: string[]
}

export interface WorkTodoSummary {
  status: WorkTodoStatus
  progressRate: number
  itemCount: number
  leafCount: number
  completedLeafCount: number
  delayedCount: number
}

export interface WorkTodo {
  todoUuid: string
  parentTodoUuid: string | null
  title: string
  description: string | null
  status: WorkTodoStatus
  progressRate: number
  startDate: string | null
  dueDate: string | null
  sortOrder: number
  depth: number
  leaf: boolean
  completed: boolean
  completedAt: string | null
  delayed: boolean
  createdAt: string
  updatedAt: string
  children: WorkTodo[]
}

export interface WorkTodoTree {
  workUnitUuid: string
  summary: WorkTodoSummary
  todos: WorkTodo[]
}

export type WorkTodoGroup = 'ALL' | 'TODAY' | 'UPCOMING' | 'OVERDUE' | 'COMPLETED' | 'UNSCHEDULED'

export interface WorkTodoListItem {
  todoUuid: string
  workUnitUuid: string
  workUnitTitle: string
  workUnitActive: boolean
  parentTodoUuid: string | null
  parentTitle: string | null
  title: string
  description: string | null
  startDate: string | null
  dueDate: string | null
  sortOrder: number
  completed: boolean
  completedAt: string | null
  group: WorkTodoGroup
}

export interface WorkTodoInboxSummary {
  totalCount: number
  todayCount: number
  upcomingCount: number
  overdueCount: number
  completedCount: number
  unscheduledCount: number
  unknownCompletionCount: number
  progressRate: number
  asOfDate: string
  timeZone: string
}

export interface WorkTodoListParams {
  workUnitUuid?: string
  keyword?: string
  group?: WorkTodoGroup
  includeInactive?: boolean
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export interface SaveWorkTodoPayload {
  parentTodoUuid?: string | null
  title: string
  description?: string
  startDate?: string | null
  dueDate?: string | null
  sortOrder?: number | null
}

export interface WorkUnitListParams {
  keyword?: string
  category?: string
  status?: WorkUnitStatus | ''
  useYn?: 'Y' | 'N' | ''
  sort?: WorkUnitSort
}

export interface WorkUnitGitCommit {
  gitConnectionUuid: string
  provider: WorkGitProvider
  repositoryUrl: string
  repositoryName: string
  commitSha: string
  message: string
  authorName: string
  authoredAt: string
  commitUrl: string
}
