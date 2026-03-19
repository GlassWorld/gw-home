import type { PageResponse } from '~/types/api/common'

export interface TagSummary {
  tagUuid: string
  name: string
}

export interface BoardListParams {
  categoryUuid?: string
  keyword?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface BoardSummary {
  boardPostUuid: string
  categoryName: string
  title: string
  viewCount: number
  author: string
  favoriteCount: number
  commentCount: number
  createdAt: string
}

export interface BoardDetail {
  boardPostUuid: string
  categoryName: string
  title: string
  content: string
  viewCount: number
  author: string
  favoriteCount: number
  commentCount: number
  tags: TagSummary[]
  createdAt: string
  updatedAt: string
}

export type BoardListResponse = PageResponse<BoardSummary>
