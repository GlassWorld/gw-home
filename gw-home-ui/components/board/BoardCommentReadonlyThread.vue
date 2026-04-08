<script setup lang="ts">
import type { BoardComment } from '~/types/api/board'
import { formatDateTime } from '~/utils/date'
import { renderMarkdown } from '~/utils/markdown'

const props = defineProps<{
  comment: BoardComment
  depth?: number
}>()

const isReply = computed(() => Boolean(props.comment.parentCommentUuid))
const commentDepth = computed(() => props.depth ?? 1)
const renderedContent = computed(() => renderMarkdown(props.comment.content))
</script>

<template>
  <article class="board-comment-readonly" :class="{ 'board-comment-readonly--reply': isReply }">
    <header class="board-comment-readonly__header">
      <div class="board-comment-readonly__meta">
        <span v-if="isReply" class="board-comment-readonly__reply-badge">대댓글 {{ commentDepth }}단계</span>
        <strong>{{ comment.author || '알 수 없는 사용자' }}</strong>
        <div class="meta-row">
          <span>{{ formatDateTime(comment.createdAt) }}</span>
          <span v-if="comment.updatedAt">수정 {{ formatDateTime(comment.updatedAt) }}</span>
        </div>
      </div>
    </header>

    <div class="board-comment-readonly__content markdown-body" v-html="renderedContent" />

    <div v-if="comment.replies.length" class="board-comment-readonly__replies">
      <BoardCommentReadonlyThread
        v-for="reply in comment.replies"
        :key="reply.boardCommentUuid"
        :comment="reply"
        :depth="commentDepth + 1"
      />
    </div>
  </article>
</template>

<style scoped>
.board-comment-readonly {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.22);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.board-comment-readonly--reply {
  margin-left: 18px;
}

.board-comment-readonly__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.board-comment-readonly__meta {
  display: grid;
  gap: 6px;
}

.board-comment-readonly__reply-badge {
  display: inline-flex;
  width: fit-content;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.12);
  color: #7dd3fc;
  font-size: 0.76rem;
  font-weight: 700;
}

.board-comment-readonly__content {
  min-height: 24px;
}

.board-comment-readonly__replies {
  display: grid;
  gap: 12px;
}

@media (max-width: 768px) {
  .board-comment-readonly--reply {
    margin-left: 10px;
  }
}
</style>
