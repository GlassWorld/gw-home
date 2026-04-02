# API 계약

## 이 문서의 목적

이 문서는 프론트엔드와 백엔드가 어떤 응답 구조로 데이터를 주고받는지 설명한다.

## 기본 응답 구조

백엔드는 모든 API 응답을 `ApiResponse<T>` 형태로 감싼다.

```java
record ApiResponse<T>(
    boolean success,
    T data,
    String message
)
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | `boolean` | 요청 성공 여부 |
| `data` | `T \| null` | 실제 데이터 |
| `message` | `string \| null` | 오류 메시지 또는 부가 메시지 |

## 성공 응답 예시

```json
{ "success": true, "data": { "uuid": "abc", "title": "제목" }, "message": null }
```

```json
{ "success": true, "data": null, "message": null }
```

## 실패 응답 예시

```json
{ "success": false, "data": null, "message": "리소스를 찾을 수 없습니다" }
```

## 페이징 응답 구조

목록 조회는 `ApiResponse<PageResponse<T>>` 구조를 사용한다.

```java
record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalCount,
    int totalPages
)
```

예시:

```json
{
  "success": true,
  "data": {
    "content": [{ "uuid": "abc", "title": "제목" }],
    "page": 1,
    "size": 20,
    "totalCount": 150,
    "totalPages": 8
  },
  "message": null
}
```

## 오류 응답 원칙

| HTTP 상태 | ErrorCode | 설명 |
|------|------|------|
| `400` | `BAD_REQUEST` | 잘못된 요청 |
| `401` | `UNAUTHORIZED` | 인증 필요 |
| `403` | `FORBIDDEN` | 권한 없음 |
| `404` | `NOT_FOUND` | 리소스 없음 |
| `409` | `DUPLICATE` | 중복 데이터 |
| `500` | `INTERNAL_ERROR` | 서버 오류 |

오류 응답도 body 형식은 `ApiResponse<Void>`를 따른다.

## 프론트엔드 타입 예시

```typescript
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
```

## 프론트엔드 호출 원칙

- 모든 API 호출은 `ApiResponse<T>` 기준으로 처리한다
- `response.data`를 사용하기 전에 `response.success`를 먼저 확인한다
- 목록 응답은 `ApiResponse<PageResponse<T>>`로 가정한다
- 오류 메시지는 `response.message` 또는 `FetchError.data.message`에서 꺼낸다
- 응답 식별자는 `_idx`가 아니라 `uuid`만 사용한다
- composable 내부에서 `ApiResponse`를 벗기고 페이지에는 실제 `data`만 전달하는 방식을 권장한다

## 예시 코드

```typescript
import type { ApiResponse, PageResponse } from '~/types/api/common'

export function useBoard() {
  const fetchBoard = async (boardUuid: string) => {
    const response = await $fetch<ApiResponse<BoardResponse>>(`/api/v1/boards/${boardUuid}`)
    if (!response.success) {
      throw new Error(response.message ?? '게시글 조회에 실패했습니다.')
    }
    return response.data
  }

  const fetchBoardList = async () => {
    const response = await $fetch<ApiResponse<PageResponse<BoardListResponse>>>('/api/v1/boards')
    if (!response.success) {
      throw new Error(response.message ?? '게시글 목록 조회에 실패했습니다.')
    }
    return response.data
  }

  return { fetchBoard, fetchBoardList }
}
```
