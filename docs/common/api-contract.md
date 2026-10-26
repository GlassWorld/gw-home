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
- 업무 TODO·알림은 기존 요청 검증 정책을 유지한다. TODO의 `size` 범위는 1~100이며 범위를 벗어난 요청은 검증 오류로 처리한다.

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

## 간트·완료 통계 API (5단계)

두 API는 인증 Principal의 업무만 조회하며 요청의 소유자 필드는 사용하지 않는다. UUID만 노출하고 응답은 기존 ApiResponse와 snake_case를 따른다. 조회 전용 REPEATABLE_READ로 현재 현황과 추이를 같은 스냅샷에서 조합한다.

| 메서드·경로 | 결과 |
|-------------|------|
| `GET /api/v1/work-todos/gantt` | period, units(업무 전체 파생 일정·현재 요약), rows(계층 순 TODO) |
| `GET /api/v1/work-todos/statistics` | period, bucket, summary(전체 최하위 현황), period_completed_count, trend, units |

| 쿼리 | 기준 |
|------|------|
| workUnitUuid | 선택 업무 UUID; 생략 시 본인의 전체 대상 업무. 타인/삭제 업무는 빈 결과 |
| fromDate, toDate | YYYY-MM-DD, 양쪽 함께 지정. 간트 최대 93일/기본 오늘부터 14일, 통계 최대 366일/기본 오늘 포함 최근 30일 |
| includeInactive | 생략 시 간트 false, 통계 true. 명시한 true/false를 응답 period에도 표시 |
| timeZone | IANA 이름, 기본 Asia/Seoul. 잘못된 이름과 숫자 오프셋은 400 |
| bucket | 통계 day/week/month, 기본 day. 주 시작은 월요일 |

- period: from_date/to_date/time_zone/as_of_date/include_inactive/work_unit_uuid.
- 간트 rows: todo_uuid/work_unit_uuid/parent_todo_uuid/title/start_date/due_date/status/progress_rate/leaf/completed/delayed/context_only/depth. 기간과 겹치는 일정과 미지정 항목에 조상을 추가한다. 2,000행 초과 시 400으로 기간/업무 축소를 안내하며 일부 결과를 조용히 생략하지 않는다.
- units: work_unit_uuid/title/status(업무 수동 상태)/active/start_date/due_date/partial_schedule/summary/period_completed_count. 일정은 해당 업무 전체 TODO의 알려진 날짜 범위이며 부분 미정 여부를 구별한다. 최하위 TODO가 없는 업무는 통계에서 제외한다.
- summary는 기존 내 할 일 요약과 같은 total_count/today_count/upcoming_count/overdue_count/completed_count/unscheduled_count/unknown_completion_count/progress_rate/as_of_date/time_zone 구조다.
- 통계의 기간은 `cmpl_at` 완료 추이에만 적용한다. 현재 현황 분모는 업무·활성 필터의 전체 최하위 TODO이며 일정 날짜로 제한하지 않는다. 목록과 비교할 때 같은 업무·활성·시간대와 일정 기간 미지정 조건을 사용한다.
- trend: start_date/end_date/completed_count 배열, 빈 구간은 0. 경계는 현지 첫날 00:00 이상·종료 다음날 00:00 미만이며 DST를 반영한다. 첫/마지막 주·월은 선택 기간 안에서만 집계한다.
- 완료일 미상은 현재 완료율에만 포함한다. 완료 취소·삭제·부모 전환은 기존 실적에서 빠지고 재완료는 새 날짜로 집계한다. 수정 시각이나 보고서 데이터로 과거 완료를 복원하지 않는다.

## 내 할 일 메일 API (4단계)

실제 메일 발송은 사용자 요청으로 보류하며 서버 스위치를 꺼 둔다.

모두 기존 JWT 인증이 필요하며 소유자는 Principal로 결정한다. 요청에 다른 소유자 ID를 받지 않는다. 내부 PK와 확인 코드 해시는 응답하지 않는다.

| 메서드·경로 | 요청 / 응답 |
|-------------|-------------|
| `GET /api/v1/work-notifications/settings` | email/verified/time_zone/digest_time/digest_cycle/digest_enabled/reminder_enabled/server_ready/next_digest_at |
| `PUT /api/v1/work-notifications/settings` | email/time_zone/digest_time/digest_cycle/두 enabled 입력; 저장된 설정 응답 |
| `POST /api/v1/work-notifications/verification` | 저장된 수신 주소로 코드 요청; SENT/RETRY/FAILED/UNKNOWN 문자열. 확인 메일은 자동 재시도 없이 발급 제한 후 재요청 |
| `POST /api/v1/work-notifications/verification/confirm` | `{ "code": "6자리 숫자" }` → `{ "verified": true/false }`. 불일치는 횟수 커밋을 위해 성공 봉투 안의 false |
| `GET /api/v1/work-notifications/deliveries?page=1&size=20` | 본인 PageResponse, size 1~100. delivery_uuid/kind/status/attempts/due_at/next_try_at/sent_at/result_code |
| `GET/PUT/DELETE /api/v1/work-todos/{todoUuid}/reminder` | PUT `{ "send_at": "오프셋 포함 ISO-8601 시각" }`; GET/PUT reminder_uuid/todo_uuid/enabled/send_at/time_zone, DELETE data=null |

- digest_cycle: DAILY/WEEKDAYS, digest_time: HH:mm, time_zone: IANA 식별자. 개별 예약은 오프셋 있는 순간을 받는다.
- 다른 소유자/삭제 TODO는 404. 과거 예약, 완료/부모/비활성 업무, 미확인 주소, 잘못된 시각은 400. 발송 환경 미설정도 활성화/예약/코드 발송 시 400이다.
- server_ready는 환경설정 준비 여부이며 SMTP 연결 성공 보장은 아니다. SENT는 SMTP 접수, UNKNOWN은 접수 결과 불명으로 자동 재전송 대상이 아니다.
- 정책·환경변수는 [도메인 문서](../backend/domain.md#내-할-일-메일-알림-4단계), 배포 전 SQL은 [DB 문서](../backend/database.md#메일-알림-스키마-리뉴얼-4단계)를 따른다.

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

## 일상 할 일 API (2026-10-25)

모든 경로는 인증이 필요하다. 소유자는 Principal의 로그인 계정으로 결정하며 내부 PK를 받거나 반환하지 않는다.

| 요청 | 계약 |
|------|------|
| `GET /api/v1/life-todos` | `PageResponse<Todo>` |
| `GET /api/v1/life-todos/summary` | 검색·카테고리 조건의 독립 일상 집계 |
| `GET /api/v1/life-todos/{uuid}` | 본인 항목 상세 |
| `POST /api/v1/life-todos` | 일상 할 일 생성, Todo 반환 |
| `PUT /api/v1/life-todos/{uuid}` | 제목·메모·날짜·카테고리 전체 수정, 완료 상태 보존, Todo 반환 |
| `PUT /api/v1/life-todos/{uuid}/completion` | `{ "completed": true/false }`, Todo 반환 |
| `DELETE /api/v1/life-todos/{uuid}` | 소프트 삭제, data=null |
| `GET /api/v1/life-todos/categories` | 본인 카테고리 목록 |
| `POST /api/v1/life-todos/categories` | `{ "name": "집안일" }`, Category 반환 |
| `PUT /api/v1/life-todos/categories/{uuid}` | 카테고리 이름 변경, Category 반환 |
| `DELETE /api/v1/life-todos/categories/{uuid}` | 카테고리 삭제·활성 할 일 미분류 전환, data=null |

- 목록/요약 query는 camelCase: `categoryUuid`, `uncategorized=false`, `keyword`, `group=ALL`, `timeZone=Asia/Seoul`, `page=1`, `size=20`. page 1~1,000,000, size 1~100, keyword 최대 200자. 카테고리 지정과 uncategorized=true는 동시에 사용할 수 없다.
- group: ALL/TODAY/UPCOMING/OVERDUE/COMPLETED/UNSCHEDULED. 완료를 먼저 분류하고 미완료의 날짜가 오늘/이후/이전/NULL인지 판단한다. 오늘은 요청한 IANA 시간대 기준이다. 요약은 group과 페이징을 무시하고 검색·카테고리 조건만 반영한다.
- 생성/수정 body: `{ "title": "장보기", "memo": null, "category_uuid": null, "due_date": null }`. 제목 1~200자, 메모 최대 4000자, 날짜 YYYY-MM-DD. 제목만 필수. 상세/생성/수정/완료에는 query `timeZone`을 전달할 수 있다.
- Todo: `todo_uuid`, `title`, `memo`, `category_uuid`, `category_name`, `due_date`, `completed`, `completed_at`, `group`, `created_at`, `updated_at`. 날짜 미지정·미분류·미완료 시 해당 선택 필드는 null이다.
- Summary: `total_count`, `today_count`, `upcoming_count`, `overdue_count`, `completed_count`, `unscheduled_count`, `progress_rate`, `as_of_date`. 현재 유효 항목의 완료율이며 기간별 완료 이력이 아니다.
- Category: `category_uuid`, `name`. 이름 1~50자, 양끝 공백 정리, 동일 계정에서 대소문자 무시 중복은 409. 다른 계정은 같은 이름을 사용할 수 있다.
- 다른 소유자/삭제 항목·카테고리의 상세·변경·연결은 404, 잘못된 UUID·시간대·필터·JSON·날짜는 400. 타인 카테고리로 목록을 필터하면 본인 조건과 일치하는 항목이 없어 빈 결과를 반환한다.
- 일반 일상 API와 업무 TODO API는 저장·조회·집계가 분리되어 있다. 매일 반복 체크는 아래 별도 API를 사용하며 일상 알림 API는 제공하지 않는다.

## 매일 체크 API (2026-10-27)

모든 경로는 인증이 필요하며 소유자는 Principal에서 결정한다. 본인 항목이 아니면 404, 잘못된 월·UUID·날짜·시간대·입력은 400이다. `timeZone` 쿼리는 기본 `Asia/Seoul`이며 화면은 기기 시간대를 전달한다.

| 메서드·경로 | 입력 | 응답 data |
|---|---|---|
| GET `/api/v1/life-daily` | `month=YYYY-MM`, `timeZone` | `{ month, today, time_zone, items, checks }` |
| POST `/api/v1/life-daily` | `{ title }`, `timeZone` | Item |
| PUT `/api/v1/life-daily/{uuid}` | `{ title }` | Item |
| PUT `/api/v1/life-daily/{uuid}/end` | `timeZone` | null |
| PUT `/api/v1/life-daily/{uuid}/checks/{date}` | `{ completed: boolean }`, `timeZone` | null |

- Item: `{ item_uuid, title, start_date, end_date }`. Check: `{ item_uuid, date }`. 내부 PK는 노출하지 않는다.
- 제목은 1~200자이며 공백을 정리한다. 생성 시작일은 서버가 해당 시간대의 오늘로 정한다.
- 월 조회는 0001~9999년의 정확한 YYYY-MM 형식이다. 월과 반복 기간이 겹치는 항목 및 완료 기록을 동일 트랜잭션 스냅샷으로 반환한다.
- 체크 날짜는 YYYY-MM-DD이며 시작일~종료일(양 끝 포함) 내 과거·오늘만 허용한다. 미체크는 미완료이며 동일 상태 재전송은 중복 행을 만들지 않는다.
- 종료 요청은 오늘을 마지막 유효 날짜로 기록한다. 반복 종료 재요청은 기존 종료일을 유지한다. 종료 항목의 제목 변경은 거부하지만 유효 기간의 과거 체크는 수정 가능하다.
- 날짜별 체크는 일반 일상 할 일 및 업무 완료 집계와 독립적이다.
