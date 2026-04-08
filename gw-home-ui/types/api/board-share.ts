import type { BoardComment } from '~/types/api/board'

export type BoardShareStatus = 'INACTIVE' | 'SHARING' | 'EXPIRING_SOON' | 'EXPIRED' | 'REVOKED'

export type BoardShareAccessStatus =
  | 'AVAILABLE'
  | 'PASSWORD_REQUIRED'
  | 'EXPIRED'
  | 'REVOKED'
  | 'NOT_FOUND'
  | 'UNAVAILABLE'

export interface BoardShareSettings {
  shareEnabled: boolean
  status: BoardShareStatus
  shareToken: string | null
  passwordEnabled: boolean
  expiresAt: string | null
  revokedAt: string | null
  createdAt: string | null
}

export interface SaveBoardSharePayload {
  shareEnabled: boolean
  expirationDays?: number
  expiresAt?: string
  passwordEnabled: boolean
  password?: string
}

export interface BoardShareAccessStatusInfo {
  status: BoardShareAccessStatus
  passwordRequired: boolean
  expiresAt: string | null
  title: string | null
}

export interface PublicBoardShareAttachment {
  originalName: string
  mimeType: string
  fileSize: number
  createdAt: string
}

export interface PublicBoardShare {
  title: string
  categoryName: string | null
  content: string
  author: string
  attachments: PublicBoardShareAttachment[]
  comments: BoardComment[]
  createdAt: string
  updatedAt: string
  expiresAt: string
}
