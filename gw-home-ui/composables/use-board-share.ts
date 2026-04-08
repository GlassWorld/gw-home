import type { ApiResponse } from '~/types/api/common'
import type { BoardComment } from '~/types/api/board'
import type {
  BoardShareAccessStatusInfo,
  BoardShareSettings,
  PublicBoardShare,
  SaveBoardSharePayload
} from '~/types/api/board-share'

interface BoardShareSettingsApi {
  share_enabled?: boolean
  status?: BoardShareSettings['status']
  share_token?: string | null
  password_enabled?: boolean
  expires_at?: string | null
  revoked_at?: string | null
  created_at?: string | null
}

interface BoardShareAccessStatusApi {
  status?: BoardShareAccessStatusInfo['status']
  password_required?: boolean
  expires_at?: string | null
  title?: string | null
}

interface PublicBoardShareAttachmentApi {
  original_name?: string
  mime_type?: string
  file_size?: number
  created_at?: string
}

interface PublicBoardShareApi {
  title?: string
  category_name?: string | null
  content?: string
  author?: string
  attachments?: PublicBoardShareAttachmentApi[]
  comments?: BoardCommentApi[]
  created_at?: string
  updated_at?: string
  expires_at?: string
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

export function useBoardShare() {
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

  function toBoardShareSettings(response: BoardShareSettingsApi): BoardShareSettings {
    return {
      shareEnabled: Boolean(response.share_enabled),
      status: response.status ?? 'INACTIVE',
      shareToken: response.share_token ?? null,
      passwordEnabled: Boolean(response.password_enabled),
      expiresAt: response.expires_at ?? null,
      revokedAt: response.revoked_at ?? null,
      createdAt: response.created_at ?? null
    }
  }

  function toBoardShareAccessStatusInfo(response: BoardShareAccessStatusApi): BoardShareAccessStatusInfo {
    return {
      status: response.status ?? 'NOT_FOUND',
      passwordRequired: Boolean(response.password_required),
      expiresAt: response.expires_at ?? null,
      title: response.title ?? null
    }
  }

  function toPublicBoardShare(response: PublicBoardShareApi): PublicBoardShare {
    return {
      title: response.title ?? '',
      categoryName: response.category_name ?? null,
      content: response.content ?? '',
      author: response.author ?? '',
      attachments: (response.attachments ?? []).map(attachment => ({
        originalName: attachment.original_name ?? '',
        mimeType: attachment.mime_type ?? '',
        fileSize: Number(attachment.file_size ?? 0),
        createdAt: attachment.created_at ?? ''
      })),
      comments: (response.comments ?? []).map(toBoardComment),
      createdAt: response.created_at ?? '',
      updatedAt: response.updated_at ?? response.created_at ?? '',
      expiresAt: response.expires_at ?? ''
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

  async function fetchBoardShare(boardUuid: string): Promise<BoardShareSettings> {
    const response = await authorizedFetch<ApiResponse<BoardShareSettingsApi>>(`/api/v1/boards/${boardUuid}/share`, {
      method: 'GET'
    })

    return toBoardShareSettings(response.data)
  }

  async function saveBoardShare(boardUuid: string, payload: SaveBoardSharePayload): Promise<BoardShareSettings> {
    const response = await authorizedFetch<ApiResponse<BoardShareSettingsApi>>(`/api/v1/boards/${boardUuid}/share`, {
      method: 'PUT',
      body: {
        share_enabled: payload.shareEnabled,
        expiration_days: payload.expirationDays,
        expires_at: payload.expiresAt,
        password_enabled: payload.passwordEnabled,
        password: payload.password
      }
    })

    return toBoardShareSettings(response.data)
  }

  async function reissueBoardShare(boardUuid: string): Promise<BoardShareSettings> {
    const response = await authorizedFetch<ApiResponse<BoardShareSettingsApi>>(`/api/v1/boards/${boardUuid}/share/reissue`, {
      method: 'POST'
    })

    return toBoardShareSettings(response.data)
  }

  async function deactivateBoardShare(boardUuid: string): Promise<BoardShareSettings> {
    const response = await authorizedFetch<ApiResponse<BoardShareSettingsApi>>(`/api/v1/boards/${boardUuid}/share`, {
      method: 'DELETE'
    })

    return toBoardShareSettings(response.data)
  }

  async function fetchBoardShareStatus(shareToken: string): Promise<BoardShareAccessStatusInfo> {
    const response = await $fetch<ApiResponse<BoardShareAccessStatusApi>>(`/api/v1/board-shares/${shareToken}/status`, {
      baseURL: apiBaseUrl
    })

    return toBoardShareAccessStatusInfo(response.data)
  }

  async function accessBoardShare(shareToken: string, password?: string): Promise<PublicBoardShare> {
    const response = await $fetch<ApiResponse<PublicBoardShareApi>>(`/api/v1/board-shares/${shareToken}/access`, {
      baseURL: apiBaseUrl,
      method: 'POST',
      body: {
        password
      }
    })

    return toPublicBoardShare(response.data)
  }

  return {
    fetchBoardShare,
    saveBoardShare,
    reissueBoardShare,
    deactivateBoardShare,
    fetchBoardShareStatus,
    accessBoardShare
  }
}
