import type { ApiResponse } from '~/types/api/common'
import type {
  OpenWeeklyReport,
  OpenWeeklyReportMember,
  SaveWeeklyReportPayload,
  WeeklyReport,
  WeeklyReportAiDraft,
  WeeklyReportAiDraftPayload,
  WeeklyReportDailySource,
  WeeklyReportGenerationType
} from '../types/work.types'

interface DailyReportWorkUnitApi {
  workUnitUuid?: string
  work_unit_uuid?: string
  title?: string
  category?: string | null
}

interface WeeklyReportDailySourceApi extends Partial<WeeklyReportDailySource> {
  report_date?: string
  work_units?: DailyReportWorkUnitApi[]
}

interface WeeklyReportApi extends Partial<WeeklyReport> {
  week_start_date?: string
  week_end_date?: string
  open_yn?: 'Y' | 'N'
  published_at?: string | null
  generation_type?: WeeklyReportGenerationType
  created_at?: string
  updated_at?: string
}

interface WeeklyReportAiDraftApi extends Partial<WeeklyReportAiDraft> {
  generation_type?: WeeklyReportGenerationType
  source_count?: number
  model_name?: string
}

interface OpenWeeklyReportMemberApi extends Partial<OpenWeeklyReportMember> {
  member_uuid?: string
  login_id?: string
  open_report_count?: number
  last_published_at?: string | null
}

interface OpenWeeklyReportApi extends WeeklyReportApi, Partial<OpenWeeklyReport> {
  member_uuid?: string
  login_id?: string
}

function normalizeLocalDate(value: unknown): string {
  if (typeof value !== 'string') {
    return ''
  }

  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return value
  }

  const parsedDate = new Date(value)

  if (!Number.isNaN(parsedDate.getTime())) {
    const year = parsedDate.getFullYear()
    const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
    const day = String(parsedDate.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const matchedDate = value.match(/\d{4}-\d{2}-\d{2}/)
  return matchedDate?.[0] ?? ''
}

function toWeeklyBody(payload: SaveWeeklyReportPayload): Record<string, string> {
  return {
    week_start_date: payload.weekStartDate,
    week_end_date: payload.weekEndDate,
    title: payload.title,
    content: payload.content,
    open_yn: payload.openYn,
    generation_type: payload.generationType
  }
}

function toDraftBody(payload: WeeklyReportAiDraftPayload): Record<string, string> {
  return {
    week_start_date: payload.weekStartDate,
    week_end_date: payload.weekEndDate,
    additional_prompt: payload.additionalPrompt ?? ''
  }
}

function normalizeGenerationType(value: string | undefined): WeeklyReport['generationType'] {
  if (value === 'OPENAI' || value === 'RULE_BASED') {
    return value
  }

  return 'MANUAL'
}

function toWeeklyReport(report: WeeklyReportApi): WeeklyReport {
  return {
    uuid: report.uuid ?? '',
    weekStartDate: normalizeLocalDate(report.weekStartDate ?? report.week_start_date),
    weekEndDate: normalizeLocalDate(report.weekEndDate ?? report.week_end_date),
    title: report.title ?? '',
    content: report.content ?? '',
    openYn: (report.openYn ?? report.open_yn) === 'Y' ? 'Y' : 'N',
    publishedAt: report.publishedAt ?? report.published_at ?? null,
    generationType: normalizeGenerationType(report.generationType ?? report.generation_type),
    createdAt: report.createdAt ?? report.created_at ?? '',
    updatedAt: report.updatedAt ?? report.updated_at ?? ''
  }
}

function toDailySource(source: WeeklyReportDailySourceApi): WeeklyReportDailySource {
  const workUnitList = (source.workUnits ?? source.work_units ?? []) as DailyReportWorkUnitApi[]

  return {
    uuid: source.uuid ?? '',
    reportDate: normalizeLocalDate(source.reportDate ?? source.report_date),
    workUnits: workUnitList.map((workUnit) => ({
      workUnitUuid: workUnit.workUnitUuid ?? workUnit.work_unit_uuid ?? '',
      title: workUnit.title ?? '',
      category: workUnit.category ?? null
    })),
    content: source.content ?? null,
    note: source.note ?? null
  }
}

function toOpenWeeklyReportMember(member: OpenWeeklyReportMemberApi): OpenWeeklyReportMember {
  return {
    memberUuid: member.memberUuid ?? member.member_uuid ?? '',
    loginId: member.loginId ?? member.login_id ?? '',
    nickname: member.nickname ?? null,
    openReportCount: Number(member.openReportCount ?? member.open_report_count ?? 0),
    lastPublishedAt: member.lastPublishedAt ?? member.last_published_at ?? null
  }
}

function toOpenWeeklyReport(report: OpenWeeklyReportApi): OpenWeeklyReport {
  return {
    ...toWeeklyReport(report),
    memberUuid: report.memberUuid ?? report.member_uuid ?? '',
    loginId: report.loginId ?? report.login_id ?? '',
    nickname: report.nickname ?? null
  }
}

export function useWeeklyReportApi() {
  const { authorizedFetch } = useAuth()

  async function fetchWeeklyReports(): Promise<WeeklyReport[]> {
    const response = await authorizedFetch<ApiResponse<WeeklyReportApi[]>>('/api/v1/weekly-reports', {
      method: 'GET'
    })

    return response.data.map(toWeeklyReport)
  }

  async function fetchWeeklyReport(uuid: string): Promise<WeeklyReport> {
    const response = await authorizedFetch<ApiResponse<WeeklyReportApi>>(`/api/v1/weekly-reports/${uuid}`, {
      method: 'GET'
    })

    return toWeeklyReport(response.data)
  }

  async function fetchWeeklyDailySources(weekStartDate: string, weekEndDate: string): Promise<WeeklyReportDailySource[]> {
    const response = await authorizedFetch<ApiResponse<WeeklyReportDailySourceApi[]>>('/api/v1/weekly-reports/daily-sources', {
      method: 'GET',
      query: {
        weekStartDate,
        weekEndDate
      }
    })

    return response.data.map(toDailySource)
  }

  async function fetchOpenWeeklyReportMembers(): Promise<OpenWeeklyReportMember[]> {
    const response = await authorizedFetch<ApiResponse<OpenWeeklyReportMemberApi[]>>('/api/v1/weekly-reports/open/members', {
      method: 'GET'
    })

    return response.data.map(toOpenWeeklyReportMember)
  }

  async function fetchOpenWeeklyReports(memberUuid?: string): Promise<OpenWeeklyReport[]> {
    const response = await authorizedFetch<ApiResponse<OpenWeeklyReportApi[]>>('/api/v1/weekly-reports/open', {
      method: 'GET',
      query: {
        memberUuid
      }
    })

    return response.data.map(toOpenWeeklyReport)
  }

  async function fetchOpenWeeklyReport(uuid: string): Promise<OpenWeeklyReport> {
    const response = await authorizedFetch<ApiResponse<OpenWeeklyReportApi>>(`/api/v1/weekly-reports/open/${uuid}`, {
      method: 'GET'
    })

    return toOpenWeeklyReport(response.data)
  }

  async function createWeeklyReport(payload: SaveWeeklyReportPayload): Promise<WeeklyReport> {
    const response = await authorizedFetch<ApiResponse<WeeklyReportApi>>('/api/v1/weekly-reports', {
      method: 'POST',
      body: toWeeklyBody(payload)
    })

    return toWeeklyReport(response.data)
  }

  async function updateWeeklyReport(uuid: string, payload: SaveWeeklyReportPayload): Promise<WeeklyReport> {
    const response = await authorizedFetch<ApiResponse<WeeklyReportApi>>(`/api/v1/weekly-reports/${uuid}`, {
      method: 'PUT',
      body: toWeeklyBody(payload)
    })

    return toWeeklyReport(response.data)
  }

  async function generateWeeklyAiDraft(payload: WeeklyReportAiDraftPayload): Promise<WeeklyReportAiDraft> {
    const response = await authorizedFetch<ApiResponse<WeeklyReportAiDraftApi>>('/api/v1/weekly-reports/ai-draft', {
      method: 'POST',
      body: toDraftBody(payload)
    })

    return {
      title: response.data.title ?? '',
      content: response.data.content ?? '',
      generationType: normalizeGenerationType(response.data.generationType ?? response.data.generation_type),
      sourceCount: Number(response.data.sourceCount ?? response.data.source_count ?? 0),
      modelName: response.data.modelName ?? response.data.model_name ?? ''
    }
  }

  return {
    fetchWeeklyReports,
    fetchWeeklyReport,
    fetchWeeklyDailySources,
    fetchOpenWeeklyReport,
    fetchOpenWeeklyReportMembers,
    fetchOpenWeeklyReports,
    createWeeklyReport,
    updateWeeklyReport,
    generateWeeklyAiDraft
  }
}
