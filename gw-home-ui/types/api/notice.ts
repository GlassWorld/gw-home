import type { PageResponse } from '~/types/api/common'

export interface NoticeListParams {
  keyword?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface NoticeSummary {
  noticeUuid: string
  title: string
  viewCount: number
  createdBy: string
  createdAt: string
}

export interface NoticeDetail extends NoticeSummary {
  content: string
  updatedAt: string
}

export interface SaveNoticeForm {
  title: string
  content: string
}

export type NoticeListResponse = PageResponse<NoticeSummary>
