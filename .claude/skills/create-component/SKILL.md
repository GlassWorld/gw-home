---
name: create-component
description: Create a Vue3 component with explicit Props/Emits types, kebab-case filename, and PascalCase component name.
tags:
  - frontend
  - nuxt
  - vue
  - component
  - typescript
---

# create-component

## Use When

- A reusable Vue3 component needs to be created
- Props and emits must be typed explicitly
- A UI element is shared across multiple pages

## Read First

- `docs/frontend/frontend-rules.md`

## Input

```
NAME: {컴포넌트명 (PascalCase)}
DOMAIN: {도메인명 또는 common}
PURPOSE: {컴포넌트 역할}
PROPS: {props 목록}
EMITS: {emit 이벤트 목록}
```

## Output

Provide implementation:

- `{project}-ui/components/{domain}/{component-name}.vue` (file name in kebab-case)

## Must Follow

- File name: kebab-case (`board-list-item.vue`)
- Component name: PascalCase (`BoardListItem`)
- Props: `defineProps<{...}>()` with explicit types
- Emits: `defineEmits<{...}>()` with explicit types
- Variable/function names: camelCase full name (no abbreviations)
- CSS class names: kebab-case

## Never

- ❌ Use `any` type
- ❌ Use abbreviated variable names (`btn`, `usr`, `idx`)
- ❌ Call API directly in component — use composable or receive via props

## Example

```vue
<!-- components/board/board-list-item.vue -->
<script setup lang="ts">
import type { BoardListResponse } from '~/types/api/board'

defineProps<{
  board: BoardListResponse
}>()

defineEmits<{
  click: [boardUuid: string]
}>()
</script>

<template>
  <div class="board-list-item" @click="$emit('click', board.uuid)">
    <h3 class="board-title">{{ board.title }}</h3>
    <span class="board-author">{{ board.createdBy }}</span>
    <time class="board-created-at">{{ board.createdAt }}</time>
  </div>
</template>
```

## Implementation Order

1. Confirm types in `types/api/{domain}.ts`
2. Define Props and Emits
3. Write template
4. Write styles
