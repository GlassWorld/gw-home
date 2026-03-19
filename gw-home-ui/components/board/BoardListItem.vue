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
  border: 1px solid rgba(116, 87, 51, 0.14);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.65);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.board-list-item:hover {
  transform: translateY(-2px);
  border-color: rgba(196, 92, 45, 0.24);
  box-shadow: 0 18px 28px rgba(69, 45, 20, 0.08);
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
