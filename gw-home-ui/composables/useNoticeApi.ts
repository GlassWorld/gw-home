import type { ApiResponse, PageResponse } from '~/types/api/common'
import type { NoticeDetail, NoticeListParams, NoticeSummary, SaveNoticeForm } from '~/types/api/notice'

interface NoticeApi {
  notice_uuid?: string
  title?: string
  content?: string
  view_count?: number
  created_by?: string
  created_at?: string
  updated_at?: string
}

interface NoticePageApi {
  content?: NoticeApi[]
  page?: number
  size?: number
  total_count?: number
  total_pages?: number
}

function toNoticeSummary(notice: NoticeApi): NoticeSummary {
  return {
    noticeUuid: notice.notice_uuid ?? '',
    title: notice.title ?? '',
    viewCount: notice.view_count ?? 0,
    createdBy: notice.created_by ?? '',
    createdAt: notice.created_at ?? ''
  }
}

function toNoticeDetail(notice: NoticeApi): NoticeDetail {
  return {
    ...toNoticeSummary(notice),
    content: notice.content ?? '',
    updatedAt: notice.updated_at ?? notice.created_at ?? ''
  }
}

function resolveApiBaseUrl(apiBase: string): string {
  if (import.meta.client || apiBase.startsWith('http://') || apiBase.startsWith('https://')) {
    return apiBase
  }

  const headers = useRequestHeaders(['host', 'x-forwarded-proto'])
  const host = headers.host

  if (!host) {
    return apiBase
  }

  const protocol = headers['x-forwarded-proto'] ?? 'http'
  return `${protocol}://${host}`
}

export function useNoticeApi() {
  const runtimeConfig = useRuntimeConfig()
  const apiBaseUrl = resolveApiBaseUrl(runtimeConfig.public.apiBase)
  const { authorizedFetch } = useAuth()

  async function fetchNoticeList(params: NoticeListParams = {}): Promise<PageResponse<NoticeSummary>> {
    const response = await authorizedFetch<ApiResponse<NoticePageApi>>('/api/v1/notices', {
      method: 'GET',
      query: {
        keyword: params.keyword,
        page: params.page ?? 1,
        size: params.size ?? 20,
        sortBy: params.sortBy ?? 'createdAt',
        sortDirection: params.sortDirection ?? 'DESC'
      }
    })

    return {
      content: (response.data.content ?? []).map(toNoticeSummary),
      page: response.data.page ?? 1,
      size: response.data.size ?? 20,
      totalCount: response.data.total_count ?? 0,
      totalPages: response.data.total_pages ?? 0
    }
  }

  async function fetchDashboardNotices(limit = 5): Promise<NoticeSummary[]> {
    const response = await authorizedFetch<ApiResponse<NoticeApi[]>>('/api/v1/notices/dashboard', {
      method: 'GET',
      query: { limit }
    })

    return (response.data ?? []).map(toNoticeSummary)
  }

  async function fetchNotice(noticeUuid: string): Promise<NoticeDetail> {
    const response = await authorizedFetch<ApiResponse<NoticeApi>>(`/api/v1/notices/${noticeUuid}`, {
      method: 'GET'
    })

    return toNoticeDetail(response.data)
  }

  async function fetchAdminNoticeList(params: NoticeListParams = {}): Promise<PageResponse<NoticeSummary>> {
    const response = await authorizedFetch<ApiResponse<NoticePageApi>>('/api/v1/admin/notices', {
      method: 'GET',
      query: {
        keyword: params.keyword,
        page: params.page ?? 1,
        size: params.size ?? 20,
        sortBy: params.sortBy ?? 'createdAt',
        sortDirection: params.sortDirection ?? 'DESC'
      }
    })

    return {
      content: (response.data.content ?? []).map(toNoticeSummary),
      page: response.data.page ?? 1,
      size: response.data.size ?? 20,
      totalCount: response.data.total_count ?? 0,
      totalPages: response.data.total_pages ?? 0
    }
  }

  async function fetchAdminNotice(noticeUuid: string): Promise<NoticeDetail> {
    const response = await authorizedFetch<ApiResponse<NoticeApi>>(`/api/v1/admin/notices/${noticeUuid}`, {
      method: 'GET'
    })

    return toNoticeDetail(response.data)
  }

  async function createNotice(form: SaveNoticeForm): Promise<NoticeDetail> {
    const response = await authorizedFetch<ApiResponse<NoticeApi>>('/api/v1/admin/notices', {
      method: 'POST',
      body: {
        title: form.title,
        content: form.content
      }
    })

    return toNoticeDetail(response.data)
  }

  async function updateNotice(noticeUuid: string, form: SaveNoticeForm): Promise<NoticeDetail> {
    const response = await authorizedFetch<ApiResponse<NoticeApi>>(`/api/v1/admin/notices/${noticeUuid}`, {
      method: 'PUT',
      body: {
        title: form.title,
        content: form.content
      }
    })

    return toNoticeDetail(response.data)
  }

  async function deleteNotice(noticeUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/admin/notices/${noticeUuid}`, {
      method: 'DELETE'
    })
  }

  return {
    fetchNoticeList,
    fetchDashboardNotices,
    fetchNotice,
    fetchAdminNoticeList,
    fetchAdminNotice,
    createNotice,
    updateNotice,
    deleteNotice
  }
}
