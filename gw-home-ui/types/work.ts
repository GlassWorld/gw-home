import type { PageResponse } from '~/types/api/common'

export type WorkUnitStatus = 'IN_PROGRESS' | 'DONE' | 'ON_HOLD'
export type WorkUnitSort = 'updated' | 'recent' | 'frequent'
export type DailyReportStatus = 'PLANNED' | 'IN_PROGRESS' | 'DONE'
export type WeeklyReportGenerationType = 'MANUAL' | 'OPENAI' | 'RULE_BASED'

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
  content: string
  status: DailyReportStatus
  note: string | null
  createdAt: string
  updatedAt: string
}

export interface SaveDailyReportPayload {
  reportDate: string
  content: string
  status: DailyReportStatus
  note?: string
}

export interface UpdateDailyReportPayload {
  content: string
  status: DailyReportStatus
  note?: string
}

export interface DailyReportListParams {
  memberUuid?: string
  dateFrom?: string
  dateTo?: string
  status?: DailyReportStatus | ''
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
  status: DailyReportStatus
  content: string
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
