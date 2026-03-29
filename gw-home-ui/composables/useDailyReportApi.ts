import type { ApiResponse, PageResponse } from '~/types/api/common'
import type {
  AdminDailyReport,
  AdminDailyReportMissing,
  DailyReport,
  DailyReportWorkUnit,
  DailyReportListParams,
  DailyReportMissing,
  SaveDailyReportPayload,
  UpdateDailyReportPayload
} from '~/types/work'

export function useDailyReportApi() {
  const { authorizedFetch } = useAuth()

  interface DailyReportApi {
    uuid?: string
    reportDate?: string
    content?: string | null
    note?: string | null
    createdAt?: string
    updatedAt?: string
    workUnits?: DailyReportWorkUnitApi[]
    work_units?: DailyReportWorkUnitApi[]
    report_date?: string
    cntn?: string | null
    created_at?: string
    updated_at?: string
  }

  interface AdminDailyReportApi extends DailyReportApi {
    memberUuid?: string
    loginId?: string
    nickname?: string | null
    workUnits?: DailyReportWorkUnitApi[]
    work_units?: DailyReportWorkUnitApi[]
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

  interface DailyReportWorkUnitApi extends Partial<DailyReportWorkUnit> {
    work_unit_uuid?: string
  }

  function toCreateBody(payload: SaveDailyReportPayload): Record<string, string | string[]> {
    return {
      report_date: payload.reportDate,
      work_unit_uuids: payload.workUnitUuids,
      note: payload.note ?? '',
      content: payload.content ?? ''
    }
  }

  function toUpdateBody(payload: UpdateDailyReportPayload): Record<string, string | string[]> {
    return {
      work_unit_uuids: payload.workUnitUuids,
      note: payload.note ?? '',
      content: payload.content ?? ''
    }
  }

  function toDailyReportWorkUnit(workUnit: DailyReportWorkUnitApi): DailyReportWorkUnit {
    return {
      workUnitUuid: workUnit.workUnitUuid ?? workUnit.work_unit_uuid ?? '',
      title: workUnit.title ?? '',
      category: workUnit.category ?? null
    }
  }

  function toDailyReport(report: DailyReportApi): DailyReport {
    return {
      uuid: report.uuid ?? '',
      reportDate: report.reportDate ?? report.report_date ?? '',
      workUnits: (report.workUnits ?? report.work_units ?? []).map(toDailyReportWorkUnit),
      content: report.content ?? report.cntn ?? null,
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
