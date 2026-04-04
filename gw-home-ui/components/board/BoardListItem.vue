<script setup lang="ts">
import type { BoardSummary } from '~/types/api/board'

defineProps<{
  board: BoardSummary
}>()
</script>

<template>
  <NuxtLink :to="`/board/${board.boardPostUuid}`" class="board-list-item">
    <div class="board-list-item__top">
      <span class="board-list-item__category">{{ board.categoryName }}</span>
      <span>{{ new Date(board.createdAt).toLocaleString() }}</span>
    </div>

    <h3 class="board-list-item__title">
      {{ board.title }}
    </h3>

    <div class="meta-row">
      <span>작성자 {{ board.author }}</span>
      <span>조회 {{ board.viewCount }}</span>
      <span>좋아요 {{ board.favoriteCount }}</span>
      <span>댓글 {{ board.commentCount }}</span>
    </div>
  </NuxtLink>
</template>

<style scoped>
.board-list-item {
  display: block;
  padding: 20px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(20, 29, 62, 0.82) 0%, rgba(12, 18, 43, 0.8) 100%);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.board-list-item:hover {
  transform: none;
  border-color: rgba(176, 195, 255, 0.24);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 16px 30px rgba(8, 22, 58, 0.24);
}

.board-list-item__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.board-list-item__category {
  color: var(--color-accent);
  font-weight: 700;
}

.board-list-item__title {
  margin: 12px 0;
  font-size: 1.18rem;
}
</style>
