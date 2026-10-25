# API 계약

## 이 문서의 목적

이 문서는 프론트엔드와 백엔드가 어떤 응답 구조로 데이터를 주고받는지 설명한다.

## 기본 응답 구조

백엔드는 모든 API 응답을 `ApiResponse<T>` 형태로 감싼다.
응답 JSON 필드명은 전역적으로 `snake_case` 직렬화를 사용한다.

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

- 게시판·공지·관리자 회원/게시글·계정 목록은 `page` 기본값 1, `size` 기본값 20을 사용한다. 누락 또는 1 미만의 값에는 기본값을 적용하고, `size`가 100을 넘으면 100으로 보정한다. 응답 `size`와 조회 offset은 보정된 크기를 기준으로 한다.
- 대시보드 공지 `limit`은 기본 5, 최대 100이다. 1 미만은 5로, 100 초과는 100으로 보정한다.
- 위 목록의 정렬값이 같으면 내부 PK 내림차순으로 순서를 고정한다. PK는 응답에 노출하지 않는다. 조회 도중 데이터가 추가·삭제되는 경우의 OFFSET 페이지 이동까지 고정하는 계약은 아니다.

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

게시글 생성·수정의 `attachment_file_uuids`는 요청 순서대로 첨부를 구성한다. null·빈 항목은 제외하고 UUID 대소문자를 정규화해 중복을 제거한다. 수정 요청에 유효한 첨부가 없으면 기존 첨부 연결을 해제한다.

- 잘못된 UUID 형식은 `400`, 누락/삭제 파일은 `404`, 본인 소유가 아닌 파일은 `403`을 반환한다.
- 파일 검증이 끝난 뒤 연결을 일괄 교체하며, 저장 실패 시 게시글 변경과 첨부 교체를 같은 트랜잭션에서 롤백한다.

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
- 백엔드 응답 JSON은 `snake_case` 이므로 프론트 타입 정의와 매핑 시 이를 기준으로 처리한다

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

## 실제 응답 필드 예시

```json
{
  "success": true,
  "data": {
    "board_post_uuid": "abc",
    "category_name": "공지",
    "created_at": "2026-04-02T10:00:00+09:00"
  },
  "message": null
}
```
