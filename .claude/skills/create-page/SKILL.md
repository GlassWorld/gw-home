---
name: create-page
description: Create a Nuxt3 page component with TypeScript, auth middleware, composable integration, and ApiResponse pattern.
tags:
  - frontend
  - nuxt
  - page
  - typescript
---

# create-page

## Use When

- A new Nuxt3 page needs to be created
- A route with auth/guest middleware must be set up
- Page needs to fetch data via composable and render it

## Read First

- `docs/frontend/frontend-rules.md`
- `docs/frontend/pages.md`
- `docs/common/api-contract.md`

## Input

```
DOMAIN: {도메인명}
PAGE: {페이지명 및 경로}
PURPOSE: {페이지 기능 설명}
AUTH: required | guest | public
```

## Output

Provide implementation:

- `{project}-ui/pages/{domain}/{page}.vue`
- `{project}-ui/composables/use-{domain}.ts` (create if not exists, extend if exists)
- `{project}-ui/types/api/{domain}.ts` (create if not exists)

## Must Follow

- File name: kebab-case
- `<script setup lang="ts">`
- Variable/function names: camelCase full name (no abbreviations)
- AUTH=required → `definePageMeta({ middleware: 'auth' })`
- AUTH=guest → `definePageMeta({ middleware: 'guest' })`
- API calls go through composable — follow `api-connect` pattern

## Never

- ❌ Use `any` type
- ❌ Call `$fetch` directly in page — use composable
- ❌ Use `_idx` as identifier — `uuid` only
- ❌ Use abbreviated variable names (`btn`, `usr`, `idx`)

## Example

```vue
<!-- pages/board/index.vue -->
<script setup lang="ts">
import type { PageResponse } from '~/types/api/common'
import type { BoardListResponse } from '~/types/api/board'

definePageMeta({ middleware: 'auth' })

const { fetchBoardList } = useBoard()
const currentPage = ref(0)

const { data: boardPage, pending: isLoading, refresh } = await useAsyncData(
  'boardList',
  () => fetchBoardList({ page: currentPage.value, size: 20 })
)

const boards = computed(() => boardPage.value?.content ?? [])
const totalPages = computed(() => boardPage.value?.totalPages ?? 0)
</script>

<template>
  <div class="board-list-page">
    <h1>게시글 목록</h1>
    <div v-if="isLoading">로딩 중...</div>
    <BoardListItem
      v-for="board in boards"
      :key="board.uuid"
      :board="board"
    />
  </div>
</template>
```

## Implementation Order

1. Confirm/add types in `types/api/{domain}.ts`
2. Confirm/add composable in `composables/use-{domain}.ts`
3. Implement page component
