---
name: api-connect
description: Create frontend TypeScript types and composable for API integration based on ApiResponse<T> contract.
tags:
  - frontend
  - api
  - nuxt
  - composable
  - typescript
---

# api-connect

## Use When

- A frontend composable needs to call a backend API endpoint
- TypeScript types for API request/response must be defined
- `ApiResponse<T>` unwrapping pattern needs to be implemented
- Paging response (`PageResponse<T>`) must be handled

## Read First

- `docs/common/api-contract.md`
- `docs/frontend/frontend-rules.md`

## Input

```
DOMAIN: {도메인명}
ENDPOINT: {HTTP메서드} {경로}
PURPOSE: {기능 설명}
RESPONSE: {응답 데이터 구조 설명}
PAGING: true | false
```

## Output

Provide implementation:

- `{project}-ui/types/api/{domain}.ts` — add types
- `{project}-ui/composables/use-{domain}.ts` — add/update composable

## Must Follow

- All API responses assumed to be wrapped in `ApiResponse<T>`
- Unwrap inside composable — return `data` only to the caller
- Check `response.success` before accessing `response.data`
- Paging: `ApiResponse<PageResponse<T>>`
- Import `ApiResponse`, `PageResponse` from `~/types/api/common`
- Variable/function names: camelCase full name (no abbreviations)

## Never

- ❌ Use `any` type
- ❌ Use `_idx` as identifier — always `uuid`
- ❌ Return raw `ApiResponse` from composable — unwrap before returning
- ❌ Inline `$fetch` in page components — always through composable

## Type Example

```typescript
// types/api/board.ts
export interface BoardResponse {
  uuid: string
  title: string
  content: string
  createdBy: string
  createdAt: string
}

export interface BoardListResponse {
  uuid: string
  title: string
  createdBy: string
  createdAt: string
}

export interface BoardListParams {
  page?: number
  size?: number
  keyword?: string
}
```

## Composable Patterns

```typescript
// Single fetch
const fetchBoard = async (uuid: string): Promise<BoardResponse> => {
  const response = await $fetch<ApiResponse<BoardResponse>>(`/api/v1/boards/${uuid}`)
  if (!response.success || response.data === null) throw new Error(response.message ?? '조회 실패')
  return response.data
}

// Paging list
const fetchBoardList = async (params?: BoardListParams): Promise<PageResponse<BoardListResponse>> => {
  const response = await $fetch<ApiResponse<PageResponse<BoardListResponse>>>('/api/v1/boards', { query: params })
  if (!response.success || response.data === null) throw new Error(response.message ?? '목록 조회 실패')
  return response.data
}

// Void (create / update / delete)
const deleteBoard = async (uuid: string): Promise<void> => {
  const response = await $fetch<ApiResponse<null>>(`/api/v1/boards/${uuid}`, { method: 'DELETE' })
  if (!response.success) throw new Error(response.message ?? '삭제 실패')
}

// With 4xx error handling (login etc.)
import type { FetchError } from 'ofetch'
try {
  const response = await $fetch<ApiResponse<LoginResponse>>('/api/v1/auth/login', {
    method: 'POST', body: { loginId, password }
  })
  if (!response.success || !response.data) throw new Error(response.message ?? '로그인 실패')
} catch (error) {
  const fetchError = error as FetchError<ApiResponse<null>>
  throw new Error(fetchError.data?.message ?? '로그인 실패')
}
```
