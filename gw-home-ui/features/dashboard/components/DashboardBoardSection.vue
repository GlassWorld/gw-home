<script setup lang="ts">
import type { BoardSummary } from '~/types/api/board'

const props = defineProps<{
  boards: BoardSummary[]
}>()
</script>

<template>
  <article class="dashboard-section content-panel">
    <div class="dashboard-section__header">
      <div>
        <h2>게시글</h2>
        <p class="message-muted">최근 등록 4건</p>
      </div>
      <CommonBaseButton variant="secondary" to="/board">
        전체
      </CommonBaseButton>
    </div>

    <div v-if="props.boards.length" class="dashboard-compact-list">
      <NuxtLink
        v-for="board in props.boards"
        :key="board.boardPostUuid"
        class="dashboard-compact-list__item dashboard-compact-list__item--link"
        :to="`/board/${board.boardPostUuid}`"
      >
        <strong>{{ board.title }}</strong>
        <span>{{ board.author }} · 댓글 {{ board.commentCount }}</span>
      </NuxtLink>
    </div>
    <p v-else class="message-muted">
      게시글이 없습니다.
    </p>
  </article>
</template>

<style scoped>
.dashboard-section {
  padding: 24px;
}

.dashboard-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-section__header h2 {
  margin: 0;
  font-size: 1.05rem;
}

.dashboard-section__header p {
  margin: 6px 0 0;
}

.dashboard-compact-list {
  display: grid;
  gap: 12px;
}

.dashboard-compact-list__item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.dashboard-compact-list__item--link {
  color: inherit;
  text-decoration: none;
}

.dashboard-compact-list__item strong {
  font-size: 0.96rem;
  line-height: 1.4;
}

.dashboard-compact-list__item span {
  color: var(--color-text-muted);
}

.dashboard-compact-list__item--link:hover {
  border-color: rgba(147, 210, 255, 0.24);
}

@media (max-width: 768px) {
  .dashboard-section__header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
