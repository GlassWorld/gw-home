# API Contract (Front ↔ Back)

## 개요

Backend는 모든 API 응답을 `ApiResponse<T>` 로 래핑한다.
Frontend는 이 구조를 기준으로 데이터를 주고받아야 한다.

---

## Backend 응답 구조

### ApiResponse\<T\>

```java
// com.gw.share.common.response.ApiResponse
record ApiResponse<T>(
    boolean success,
    T data,
    String message
)
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | `boolean` | 성공 여부 |
| `data` | `T \| null` | 실제 데이터 (`ApiResponse.ok()` 또는 실패 응답에서는 `null` 가능) |
| `message` | `string \| null` | 에러 메시지 (성공 시 `null`) |

#### 성공 응답 예시

```json
// 데이터 있음
{ "success": true, "data": { "uuid": "abc...", "title": "제목" }, "message": null }

// 데이터 없음 (생성/삭제 등)
{ "success": true, "data": null, "message": null }
```

#### 실패 응답 예시

```json
{ "success": false, "data": null, "message": "리소스를 찾을 수 없습니다" }
```

---

### PageResponse\<T\> — 페이징 응답

```java
// com.gw.share.common.response.PageResponse
record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalCount,
    int totalPages
)
```

페이징 응답은 `ApiResponse<PageResponse<T>>` 로 래핑된다.

```json
{
  "success": true,
  "data": {
    "content": [ { "uuid": "...", "title": "..." }, ... ],
    "page": 1,
    "size": 20,
    "totalCount": 150,
    "totalPages": 8
  },
  "message": null
}
```

---

### HTTP 상태 코드 + ErrorCode

| HTTP | ErrorCode | message |
|------|-----------|---------|
| 400 | `BAD_REQUEST` | 잘못된 요청입니다 |
| 401 | `UNAUTHORIZED` | 인증이 필요합니다 |
| 403 | `FORBIDDEN` | 접근 권한이 없습니다 |
| 404 | `NOT_FOUND` | 리소스를 찾을 수 없습니다 |
| 409 | `DUPLICATE` | 이미 존재합니다 |
| 500 | `INTERNAL_ERROR` | 서버 오류가 발생했습니다 |

에러 응답은 HTTP 상태 코드가 4xx/5xx이며 body는 항상 `ApiResponse<Void>` 형태이다.

---

## Frontend TypeScript 타입 정의

```typescript
// types/api/common.ts

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalCount: number
  totalPages: number
}

// 페이징 응답 래핑 타입
export type PageApiResponse<T> = ApiResponse<PageResponse<T>>
```

---

## Frontend API 호출 패턴

### 기본 패턴 — `$fetch` 사용

`$fetch`는 4xx/5xx 상태에서 자동으로 throw 한다.
현재 프론트 공통 타입은 성공 경로 기준으로 `data: T`를 사용한다.
실패 응답의 `data: null`은 `FetchError.data`에서 별도로 처리한다.

```typescript
// composables/use-board.ts
import type { ApiResponse, PageApiResponse } from '~/types/api/common'
import type { BoardResponse, BoardListResponse } from '~/types/api/board'

export function useBoard() {

  // 단건 조회
  const fetchBoard = async (boardUuid: string): Promise<BoardResponse> => {
    const response = await $fetch<ApiResponse<BoardResponse>>(
      `/api/v1/boards/${boardUuid}`
    )
    if (!response.success) {
      throw new Error(response.message ?? '조회 실패')
    }
    return response.data
  }

  // 페이징 목록 조회
  const fetchBoardList = async (
    params?: { page?: number; size?: number; keyword?: string }
  ): Promise<PageResponse<BoardListResponse>> => {
    const response = await $fetch<PageApiResponse<BoardListResponse>>(
      '/api/v1/boards',
      { query: params }
    )
    if (!response.success) {
      throw new Error(response.message ?? '목록 조회 실패')
    }
    return response.data
  }

  // 데이터 없는 요청 (생성, 삭제 등)
  const deleteBoard = async (boardUuid: string): Promise<void> => {
    const response = await $fetch<ApiResponse<null>>(
      `/api/v1/boards/${boardUuid}`,
      { method: 'DELETE' }
    )
    if (!response.success) {
      throw new Error(response.message ?? '삭제 실패')
    }
  }

  return { fetchBoard, fetchBoardList, deleteBoard }
}
```

### 에러 처리 패턴

`$fetch`는 4xx/5xx에서 `FetchError`를 throw한다.
`FetchError.data`에서 `ApiResponse` body를 꺼낼 수 있다.

```typescript
// composables/use-auth.ts
import type { FetchError } from 'ofetch'
import type { ApiResponse } from '~/types/api/common'

export function useAuth() {
  const login = async (loginId: string, password: string): Promise<void> => {
    try {
      const response = await $fetch<ApiResponse<LoginResponse>>(
        '/api/v1/auth/login',
        { method: 'POST', body: { loginId, password } }
      )
      if (!response.success) {
        throw new Error(response.message ?? '로그인 실패')
      }
      // 성공 응답의 실제 데이터 구조는 도메인별 DTO를 따른다.
    } catch (error) {
      const fetchError = error as FetchError<ApiResponse<null>>
      throw new Error(fetchError.data?.message ?? '로그인 실패')
    }
  }

  return { login }
}
```

### useFetch 사용 패턴 (SSR / 페이지 컴포넌트)

```typescript
// pages/board/index.vue
const { data: boardPage, pending: isLoading } = await useFetch<PageApiResponse<BoardListResponse>>(
  '/api/v1/boards',
  { query: { page: currentPage, size: 20 } }
)

// 실제 데이터 접근
const boards = computed(() => boardPage.value?.data?.content ?? [])
const totalPages = computed(() => boardPage.value?.data?.totalPages ?? 0)
```

---

## 규칙 요약

- [ ] 모든 API 호출은 `ApiResponse<T>` 래핑을 가정한다
- [ ] `response.data`를 꺼내기 전 `response.success` 를 반드시 확인한다
- [ ] 페이징 응답은 `ApiResponse<PageResponse<T>>` 구조이다
- [ ] 에러 메시지는 `response.message` 또는 `FetchError.data.message` 에서 가져온다
- [ ] `_idx` 필드는 응답에 없으므로 `uuid` 기반으로만 식별한다
- [ ] composable 내부에서 `ApiResponse` 를 벗겨 `data` 만 반환한다 — 페이지 컴포넌트는 data 구조에만 집중
