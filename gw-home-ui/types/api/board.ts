import type { PageResponse } from '~/types/api/common'

export interface TagSummary {
  tagUuid: string
  name: string
}

export interface BoardCategory {
  categoryUuid: string
  categoryName: string
  sortOrder: number
}

export interface BoardAttachment {
  fileUuid: string
  originalName: string
  fileUrl: string
  mimeType: string
  fileSize: number
  uploaderType: string
  createdAt: string
}

export interface BoardComment {
  boardCommentUuid: string
  content: string
  author: string
  parentCommentUuid: string | null
  replies: BoardComment[]
  createdAt: string
  updatedAt: string | null
}

export interface BoardListParams {
  categoryUuid?: string
  searchType?: 'title' | 'content' | 'author' | 'all'
  keyword?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface BoardSummary {
  boardPostUuid: string
  categoryName: string | null
  title: string
  viewCount: number
  author: string
  favoriteCount: number
  commentCount: number
  createdAt: string
}

export interface BoardDetail {
  boardPostUuid: string
  categoryName: string | null
  title: string
  content: string
  viewCount: number
  author: string
  favoriteCount: number
  commentCount: number
  attachments: BoardAttachment[]
  tags: TagSummary[]
  createdAt: string
  updatedAt: string
}

export interface SaveBoardPostPayload {
  categoryUuid?: string
  title: string
  content: string
  attachmentFileUuids: string[]
}

export interface SaveBoardCategoryPayload {
  name: string
  sortOrder: number
}

export interface SaveCommentPayload {
  parentCommentUuid?: string
  content: string
}

export interface FileUploadResponse {
  fileUuid: string
  fileUrl: string
  originalName: string
  mimeType: string
  fileSize: number
}

export interface BoardFavoriteState {
  favorited: boolean
  favoriteCount: number
}

export type BoardListResponse = PageResponse<BoardSummary>
