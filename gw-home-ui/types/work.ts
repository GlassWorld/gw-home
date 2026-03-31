import type { PageResponse } from '~/types/api/common'

export type WorkUnitStatus = 'IN_PROGRESS' | 'DONE' | 'ON_HOLD'
export type WorkUnitSort = 'updated' | 'recent' | 'frequent'
export type WeeklyReportGenerationType = 'MANUAL' | 'OPENAI' | 'RULE_BASED'
export type WorkGitProvider = 'GITLAB'

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

export interface WorkUnitListParams {
  keyword?: string
  category?: string
  status?: WorkUnitStatus | ''
  useYn?: 'Y' | 'N' | ''
  sort?: WorkUnitSort
}

export interface DailyReport {
  uuid: string
  reportDate: string
  workUnits: DailyReportWorkUnit[]
  content: string | null
  note: string | null
  createdAt: string
  updatedAt: string
}

export interface DailyReportWorkUnit {
  workUnitUuid: string
  title: string
  category: string | null
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

export interface SaveDailyReportPayload {
  reportDate: string
  workUnitUuids: string[]
  note?: string
  content?: string
}

export interface UpdateDailyReportPayload {
  workUnitUuids: string[]
  note?: string
  content?: string
}

export interface DailyReportListParams {
  memberUuid?: string
  dateFrom?: string
  dateTo?: string
  keyword?: string
  page?: number
  size?: number
}

export interface DailyReportMissing {
  reportDate: string
}

export interface AdminDailyReport extends DailyReport {
  memberUuid: string
  loginId: string
  nickname: string | null
}

export interface AdminDailyReportMissing {
  memberUuid: string
  loginId: string
  nickname: string | null
  missingDates: string[]
  missingCount: number
  lastWrittenDate: string | null
}

export interface WeeklyReport {
  uuid: string
  weekStartDate: string
  weekEndDate: string
  title: string
  content: string
  openYn: 'Y' | 'N'
  publishedAt: string | null
  generationType: WeeklyReportGenerationType
  createdAt: string
  updatedAt: string
}

export interface SaveWeeklyReportPayload {
  weekStartDate: string
  weekEndDate: string
  title: string
  content: string
  openYn: 'Y' | 'N'
  generationType: WeeklyReportGenerationType
}

export interface WeeklyReportDailySource {
  uuid: string
  reportDate: string
  workUnits: DailyReportWorkUnit[]
  note: string | null
}

export interface WeeklyReportAiDraft {
  title: string
  content: string
  generationType: WeeklyReportGenerationType
  sourceCount: number
  modelName: string
}

export interface WeeklyReportAiDraftPayload {
  weekStartDate: string
  weekEndDate: string
  additionalPrompt?: string
}

export type DailyReportPage = PageResponse<DailyReport>
export type AdminDailyReportPage = PageResponse<AdminDailyReport>
