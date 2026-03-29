import type { ApiResponse, PageResponse } from '~/types/api/common'
import type {
  AdminDailyReport,
  AdminDailyReportMissing,
  DailyReport,
  DailyReportListParams,
  DailyReportMissing,
  SaveDailyReportPayload,
  UpdateDailyReportPayload
} from '~/types/work'

export function useDailyReportApi() {
  const { authorizedFetch } = useAuth()

  interface DailyReportApi extends Partial<DailyReport> {
    report_date?: string
    created_at?: string
    updated_at?: string
  }

  interface AdminDailyReportApi extends Partial<AdminDailyReport> {
    member_uuid?: string
    login_id?: string
    report_date?: string
    created_at?: string
    updated_at?: string
  }

  interface DailyReportMissingApi extends Partial<DailyReportMissing> {
    report_date?: string
  }

  interface AdminDailyReportMissingApi extends Partial<AdminDailyReportMissing> {
    member_uuid?: string
    login_id?: string
    missing_dates?: string[]
    missing_count?: number
    last_written_date?: string | null
  }

  function toCreateBody(payload: SaveDailyReportPayload): Record<string, string> {
    return {
      reportDate: payload.reportDate,
      rptDt: payload.reportDate,
      content: payload.content,
      status: payload.status,
      note: payload.note ?? ''
    }
  }

  function toUpdateBody(payload: UpdateDailyReportPayload): Record<string, string> {
    return {
      content: payload.content,
      status: payload.status,
      note: payload.note ?? ''
    }
  }

  function normalizeStatus(status: string | undefined): DailyReport['status'] {
    if (status === 'PLANNED' || status === 'DONE') {
      return status
    }

    return 'IN_PROGRESS'
  }

  function toDailyReport(report: DailyReportApi): DailyReport {
    return {
      uuid: report.uuid ?? '',
      reportDate: report.reportDate ?? report.report_date ?? '',
      content: report.content ?? '',
      status: normalizeStatus(report.status),
      note: report.note ?? null,
      createdAt: report.createdAt ?? report.created_at ?? '',
      updatedAt: report.updatedAt ?? report.updated_at ?? ''
    }
  }

  function toAdminDailyReport(report: AdminDailyReportApi): AdminDailyReport {
    return {
      ...toDailyReport(report),
      memberUuid: report.memberUuid ?? report.member_uuid ?? '',
      loginId: report.loginId ?? report.login_id ?? '',
      nickname: report.nickname ?? null
    }
  }

  async function fetchDailyReports(params: DailyReportListParams): Promise<PageResponse<DailyReport>> {
    const response = await authorizedFetch<ApiResponse<PageResponse<DailyReportApi>>>('/api/v1/daily-reports', {
      method: 'GET',
      query: {
        dateFrom: params.dateFrom,
        dateTo: params.dateTo,
        status: params.status || undefined,
        keyword: params.keyword,
        page: params.page,
        size: params.size
      }
    })

    return {
      ...response.data,
      content: response.data.content.map(toDailyReport)
    }
  }

  async function fetchDailyReport(uuid: string): Promise<DailyReport> {
    const response = await authorizedFetch<ApiResponse<DailyReportApi>>(`/api/v1/daily-reports/${uuid}`, {
      method: 'GET'
    })

    return toDailyReport(response.data)
  }

  async function createDailyReport(payload: SaveDailyReportPayload): Promise<DailyReport> {
    const response = await authorizedFetch<ApiResponse<DailyReportApi>>('/api/v1/daily-reports', {
      method: 'POST',
      body: toCreateBody(payload)
    })

    return toDailyReport(response.data)
  }

  async function updateDailyReport(uuid: string, payload: UpdateDailyReportPayload): Promise<DailyReport> {
    const response = await authorizedFetch<ApiResponse<DailyReportApi>>(`/api/v1/daily-reports/${uuid}`, {
      method: 'PUT',
      body: toUpdateBody(payload)
    })

    return toDailyReport(response.data)
  }

  async function fetchMissingDailyReports(params: Pick<DailyReportListParams, 'dateFrom' | 'dateTo'>): Promise<DailyReportMissing[]> {
    const response = await authorizedFetch<ApiResponse<DailyReportMissingApi[]>>('/api/v1/daily-reports/missing', {
      method: 'GET',
      query: {
        dateFrom: params.dateFrom,
        dateTo: params.dateTo
      }
    })

    return response.data.map((item) => ({
      reportDate: item.reportDate ?? item.report_date ?? ''
    }))
  }

  async function fetchAdminDailyReports(params: DailyReportListParams): Promise<PageResponse<AdminDailyReport>> {
    const response = await authorizedFetch<ApiResponse<PageResponse<AdminDailyReportApi>>>('/api/v1/admin/daily-reports', {
      method: 'GET',
      query: {
        memberUuid: params.memberUuid,
        dateFrom: params.dateFrom,
        dateTo: params.dateTo,
        status: params.status || undefined,
        keyword: params.keyword,
        page: params.page,
        size: params.size
      }
    })

    return {
      ...response.data,
      content: response.data.content.map(toAdminDailyReport)
    }
  }

  async function fetchAdminMissingDailyReports(params: Pick<DailyReportListParams, 'memberUuid' | 'dateFrom' | 'dateTo'>): Promise<AdminDailyReportMissing[]> {
    const response = await authorizedFetch<ApiResponse<AdminDailyReportMissingApi[]>>('/api/v1/admin/daily-reports/missing', {
      method: 'GET',
      query: {
        memberUuid: params.memberUuid,
        dateFrom: params.dateFrom,
        dateTo: params.dateTo
      }
    })

    return response.data.map((item) => ({
      memberUuid: item.memberUuid ?? item.member_uuid ?? '',
      loginId: item.loginId ?? item.login_id ?? '',
      nickname: item.nickname ?? null,
      missingDates: item.missingDates ?? item.missing_dates ?? [],
      missingCount: Number(item.missingCount ?? item.missing_count ?? 0),
      lastWrittenDate: item.lastWrittenDate ?? item.last_written_date ?? null
    }))
  }

  return {
    fetchDailyReports,
    fetchDailyReport,
    createDailyReport,
    updateDailyReport,
    fetchMissingDailyReports,
    fetchAdminDailyReports,
    fetchAdminMissingDailyReports
  }
}
