<script setup lang="ts">
import type { BoardSummary } from '~/types/api/board'
import { formatDateTime } from '~/utils/date'

defineProps<{
  board: BoardSummary
}>()
</script>

<template>
  <NuxtLink :to="`/board/${board.boardPostUuid}`" class="board-list-item">
    <h3 class="board-list-item__title">
      {{ board.title }}
    </h3>

    <div class="board-list-item__meta-row">
      <div class="board-list-item__meta-item">
        <span class="board-list-item__meta-label">작성일</span>
        <span class="board-list-item__meta-value">{{ formatDateTime(board.createdAt) }}</span>
      </div>
      <div class="board-list-item__meta-item">
        <span class="board-list-item__meta-label">작성자</span>
        <span class="board-list-item__meta-value">{{ board.author }}</span>
      </div>
      <div class="board-list-item__meta-item">
        <span class="board-list-item__meta-label">조회</span>
        <span class="board-list-item__meta-value">{{ board.viewCount }}</span>
      </div>
      <div class="board-list-item__meta-item">
        <span class="board-list-item__meta-label">좋아요</span>
        <span class="board-list-item__meta-value">{{ board.favoriteCount }}</span>
      </div>
      <div class="board-list-item__meta-item">
        <span class="board-list-item__meta-label">댓글</span>
        <span class="board-list-item__meta-value">{{ board.commentCount }}</span>
      </div>
    </div>
  </NuxtLink>
</template>

<style scoped>
.board-list-item {
  display: grid;
  gap: 8px;
  padding: 10px 12px;
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

.board-list-item__title {
  margin: 0;
  font-size: 0.94rem;
  line-height: 1.2;
  font-weight: 600;
}

.board-list-item__meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 6px;
}

.board-list-item__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 22px;
  padding: 2px 6px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.025);
  font-size: 0.72rem;
}

.board-list-item__meta-label {
  color: var(--color-text-muted);
  font-weight: 700;
  letter-spacing: 0.03em;
}

.board-list-item__meta-value {
  color: var(--color-text);
  font-weight: 500;
}
</style>
