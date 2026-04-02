import type { ApiResponse } from '~/types/api/common'
import type {
  SaveWeeklyReportPayload,
  WeeklyReport,
  WeeklyReportAiDraft,
  WeeklyReportAiDraftPayload,
  WeeklyReportDailySource
} from '~/types/work'

export function useWeeklyReportApi() {
  const { authorizedFetch } = useAuth()

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
      weekStartDate: payload.weekStartDate,
      weekEndDate: payload.weekEndDate,
      title: payload.title,
      content: payload.content,
      openYn: payload.openYn,
      generationType: payload.generationType
    }
  }

  function toDraftBody(payload: WeeklyReportAiDraftPayload): Record<string, string> {
    return {
      weekStartDate: payload.weekStartDate,
      weekEndDate: payload.weekEndDate,
      additionalPrompt: payload.additionalPrompt ?? ''
    }
  }

  function normalizeGenerationType(value: string | undefined): WeeklyReport['generationType'] {
    if (value === 'OPENAI' || value === 'RULE_BASED') {
      return value
    }

    return 'MANUAL'
  }

  function toWeeklyReport(report: Partial<WeeklyReport>): WeeklyReport {
    return {
      uuid: report.uuid ?? '',
      weekStartDate: report.weekStartDate ?? '',
      weekEndDate: report.weekEndDate ?? '',
      title: report.title ?? '',
      content: report.content ?? '',
      openYn: report.openYn === 'Y' ? 'Y' : 'N',
      publishedAt: report.publishedAt ?? null,
      generationType: normalizeGenerationType(report.generationType),
      createdAt: report.createdAt ?? '',
      updatedAt: report.updatedAt ?? ''
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

  async function fetchWeeklyReports(): Promise<WeeklyReport[]> {
    const response = await authorizedFetch<ApiResponse<WeeklyReport[]>>('/api/v1/weekly-reports', {
      method: 'GET'
    })

    return response.data.map(toWeeklyReport)
  }

  async function fetchWeeklyReport(uuid: string): Promise<WeeklyReport> {
    const response = await authorizedFetch<ApiResponse<WeeklyReport>>(`/api/v1/weekly-reports/${uuid}`, {
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

  async function createWeeklyReport(payload: SaveWeeklyReportPayload): Promise<WeeklyReport> {
    const response = await authorizedFetch<ApiResponse<WeeklyReport>>('/api/v1/weekly-reports', {
      method: 'POST',
      body: toWeeklyBody(payload)
    })

    return toWeeklyReport(response.data)
  }

  async function updateWeeklyReport(uuid: string, payload: SaveWeeklyReportPayload): Promise<WeeklyReport> {
    const response = await authorizedFetch<ApiResponse<WeeklyReport>>(`/api/v1/weekly-reports/${uuid}`, {
      method: 'PUT',
      body: toWeeklyBody(payload)
    })

    return toWeeklyReport(response.data)
  }

  async function generateWeeklyAiDraft(payload: WeeklyReportAiDraftPayload): Promise<WeeklyReportAiDraft> {
    const response = await authorizedFetch<ApiResponse<WeeklyReportAiDraft>>('/api/v1/weekly-reports/ai-draft', {
      method: 'POST',
      body: toDraftBody(payload)
    })

    return {
      title: response.data.title ?? '',
      content: response.data.content ?? '',
      generationType: normalizeGenerationType(response.data.generationType),
      sourceCount: Number(response.data.sourceCount ?? 0),
      modelName: response.data.modelName ?? ''
    }
  }

  return {
    fetchWeeklyReports,
    fetchWeeklyReport,
    fetchWeeklyDailySources,
    createWeeklyReport,
    updateWeeklyReport,
    generateWeeklyAiDraft
  }
}
