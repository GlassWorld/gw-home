import type { ApiResponse } from '~/types/api/common'
import type {
  BoardCategory,
  BoardComment,
  BoardDetail,
  BoardFavoriteState,
  BoardListParams,
  BoardListResponse,
  FileUploadResponse,
  SaveBoardPostPayload,
  SaveCommentPayload
} from '~/types/api/board'

interface TagSummaryApi {
  tag_uuid?: string
  name?: string
}

interface BoardCategoryApi {
  category_uuid?: string
  category_name?: string
  sort_order?: number
}

interface BoardAttachmentApi {
  file_uuid?: string
  original_name?: string
  file_url?: string
  mime_type?: string
  file_size?: number
  uploader_type?: string
  created_at?: string
}

interface FileUploadResponseApi {
  file_uuid?: string
  file_url?: string
  original_name?: string
  mime_type?: string
  file_size?: number
}

interface BoardCommentApi {
  board_comment_uuid?: string
  content?: string
  author?: string
  parent_comment_uuid?: string | null
  replies?: BoardCommentApi[]
  created_at?: string
  updated_at?: string | null
}

interface BoardSummaryApi {
  board_post_uuid?: string
  category_name?: string | null
  title?: string
  view_count?: number
  author?: string
  favorite_count?: number
  comment_count?: number
  created_at?: string
}

interface BoardDetailApi extends BoardSummaryApi {
  content?: string
  attachments?: BoardAttachmentApi[]
  tags?: TagSummaryApi[]
  updated_at?: string
}

interface BoardListResponseApi {
  content?: BoardSummaryApi[]
  page?: number
  size?: number
  total_count?: number
  total_pages?: number
}

interface FavoriteResponseApi {
  favorited?: boolean
  favorite_count?: number
}

export function useBoard() {
  const runtimeConfig = useRuntimeConfig()
  const apiBaseUrl = import.meta.client || runtimeConfig.public.apiBase.startsWith('http://') || runtimeConfig.public.apiBase.startsWith('https://')
    ? runtimeConfig.public.apiBase
    : (() => {
        const headers = useRequestHeaders(['host', 'x-forwarded-proto'])
        const host = headers.host

        if (!host) {
          return runtimeConfig.public.apiBase
        }

        const protocol = headers['x-forwarded-proto'] ?? 'http'
        return `${protocol}://${host}`
      })()
  const { authorizedFetch } = useAuth()

  function toBoardCategory(category: BoardCategoryApi): BoardCategory {
    return {
      categoryUuid: category.category_uuid ?? '',
      categoryName: category.category_name ?? '',
      sortOrder: Number(category.sort_order ?? 0)
    }
  }

  function toBoardAttachment(attachment: BoardAttachmentApi) {
    return {
      fileUuid: attachment.file_uuid ?? '',
      originalName: attachment.original_name ?? '',
      fileUrl: attachment.file_url ?? '',
      mimeType: attachment.mime_type ?? '',
      fileSize: Number(attachment.file_size ?? 0),
      uploaderType: attachment.uploader_type ?? '',
      createdAt: attachment.created_at ?? ''
    }
  }

  function toFileUploadResponse(file: FileUploadResponseApi): FileUploadResponse {
    return {
      fileUuid: file.file_uuid ?? '',
      fileUrl: file.file_url ?? '',
      originalName: file.original_name ?? '',
      mimeType: file.mime_type ?? '',
      fileSize: Number(file.file_size ?? 0)
    }
  }

  function toBoardComment(comment: BoardCommentApi): BoardComment {
    return {
      boardCommentUuid: comment.board_comment_uuid ?? '',
      content: comment.content ?? '',
      author: comment.author ?? '',
      parentCommentUuid: comment.parent_comment_uuid ?? null,
      replies: (comment.replies ?? []).map(toBoardComment),
      createdAt: comment.created_at ?? '',
      updatedAt: comment.updated_at ?? null
    }
  }

  function toBoardSummary(board: BoardSummaryApi) {
    return {
      boardPostUuid: board.board_post_uuid ?? '',
      categoryName: board.category_name ?? null,
      title: board.title ?? '',
      viewCount: Number(board.view_count ?? 0),
      author: board.author ?? '',
      favoriteCount: Number(board.favorite_count ?? 0),
      commentCount: Number(board.comment_count ?? 0),
      createdAt: board.created_at ?? ''
    }
  }

  function toBoardDetail(board: BoardDetailApi): BoardDetail {
    return {
      ...toBoardSummary(board),
      content: board.content ?? '',
      attachments: (board.attachments ?? []).map(toBoardAttachment),
      tags: (board.tags ?? []).map(tag => ({
        tagUuid: tag.tag_uuid ?? '',
        name: tag.name ?? ''
      })),
      updatedAt: board.updated_at ?? board.created_at ?? ''
    }
  }

  function toBoardFavoriteState(favorite: FavoriteResponseApi): BoardFavoriteState {
    return {
      favorited: Boolean(favorite.favorited),
      favoriteCount: Number(favorite.favorite_count ?? 0)
    }
  }

  async function fetchBoardList(params: BoardListParams = {}): Promise<BoardListResponse> {
    const response = await $fetch<ApiResponse<BoardListResponseApi>>('/api/v1/boards', {
      baseURL: apiBaseUrl,
      query: {
        categoryUuid: params.categoryUuid,
        searchType: params.searchType ?? 'title',
        keyword: params.keyword,
        page: params.page,
        size: params.size,
        sortBy: params.sortBy ?? 'createdAt',
        sortDirection: params.sortDirection ?? 'DESC'
      }
    })

    return {
      content: (response.data.content ?? []).map(toBoardSummary),
      page: response.data.page ?? 1,
      size: response.data.size ?? 20,
      totalCount: response.data.total_count ?? 0,
      totalPages: response.data.total_pages ?? 0
    }
  }

  async function fetchBoardCategories(): Promise<BoardCategory[]> {
    const response = await $fetch<ApiResponse<BoardCategoryApi[]>>('/api/v1/boards/categories', {
      baseURL: apiBaseUrl
    })

    return (response.data ?? []).map(toBoardCategory)
  }

  async function fetchBoard(boardUuid: string): Promise<BoardDetail> {
    const response = await $fetch<ApiResponse<BoardDetailApi>>(`/api/v1/boards/${boardUuid}`, {
      baseURL: apiBaseUrl
    })

    return toBoardDetail(response.data)
  }

  async function createBoard(payload: SaveBoardPostPayload): Promise<BoardDetail> {
    const response = await authorizedFetch<ApiResponse<BoardDetailApi>>('/api/v1/boards', {
      method: 'POST',
      body: payload
    })

    return toBoardDetail(response.data)
  }

  async function updateBoard(boardUuid: string, payload: SaveBoardPostPayload): Promise<BoardDetail> {
    const response = await authorizedFetch<ApiResponse<BoardDetailApi>>(`/api/v1/boards/${boardUuid}`, {
      method: 'PUT',
      body: payload
    })

    return toBoardDetail(response.data)
  }

  async function deleteBoard(boardUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/boards/${boardUuid}`, {
      method: 'DELETE'
    })
  }

  async function fetchBoardFavorite(boardUuid: string): Promise<BoardFavoriteState> {
    const authStore = useAuthStore()
    const response = await $fetch<ApiResponse<FavoriteResponseApi>>(`/api/v1/boards/${boardUuid}/favorite/count`, {
      baseURL: apiBaseUrl,
      headers: authStore.accessToken
        ? { Authorization: `Bearer ${authStore.accessToken}` }
        : undefined
    })

    return toBoardFavoriteState(response.data)
  }

  async function toggleBoardFavorite(boardUuid: string): Promise<BoardFavoriteState> {
    const response = await authorizedFetch<ApiResponse<FavoriteResponseApi>>(`/api/v1/boards/${boardUuid}/favorite`, {
      method: 'POST'
    })

    return toBoardFavoriteState(response.data)
  }

  async function fetchComments(boardUuid: string): Promise<BoardComment[]> {
    const response = await $fetch<ApiResponse<BoardCommentApi[]>>(`/api/v1/boards/${boardUuid}/comments`, {
      baseURL: apiBaseUrl
    })

    return (response.data ?? []).map(toBoardComment)
  }

  async function createComment(boardUuid: string, payload: SaveCommentPayload): Promise<BoardComment> {
    const response = await authorizedFetch<ApiResponse<BoardCommentApi>>(`/api/v1/boards/${boardUuid}/comments`, {
      method: 'POST',
      body: {
        parent_comment_uuid: payload.parentCommentUuid,
        content: payload.content
      }
    })

    return toBoardComment(response.data)
  }

  async function updateComment(commentUuid: string, content: string): Promise<BoardComment> {
    const response = await authorizedFetch<ApiResponse<BoardCommentApi>>(`/api/v1/comments/${commentUuid}`, {
      method: 'PUT',
      body: { content }
    })

    return toBoardComment(response.data)
  }

  async function deleteComment(commentUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/comments/${commentUuid}`, {
      method: 'DELETE'
    })
  }

  async function uploadBoardFile(file: File, uploaderType: 'BOARD_ATTACHMENT' | 'BOARD_IMAGE'): Promise<FileUploadResponse> {
    const formData = new FormData()
    formData.set('upldrType', uploaderType)
    formData.set('file', file)

    const response = await authorizedFetch<ApiResponse<FileUploadResponseApi>>('/api/v1/files', {
      method: 'POST',
      body: formData
    })

    return toFileUploadResponse(response.data)
  }

  async function downloadBoardFile(fileUuid: string, originalName: string): Promise<void> {
    const blob = await authorizedFetch<Blob>(`/api/v1/files/${fileUuid}/download`, {
      method: 'GET',
      responseType: 'blob'
    })
    const blobUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = originalName
    document.body.append(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(blobUrl)
  }

  return {
    fetchBoardCategories,
    fetchBoardList,
    fetchBoard,
    createBoard,
    updateBoard,
    deleteBoard,
    fetchBoardFavorite,
    toggleBoardFavorite,
    fetchComments,
    createComment,
    updateComment,
    deleteComment,
    uploadBoardFile,
    downloadBoardFile
  }
}
