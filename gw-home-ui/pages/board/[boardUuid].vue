<script setup lang="ts">
import type { BoardDetail } from '~/types/api/board'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const authStore = useAuthStore()
const { fetchBoard, deleteBoard } = useBoard()

const board = ref<BoardDetail | null>(null)
const errorMessage = ref('')
const isDeleting = ref(false)

const isOwner = computed(() => {
  if (!board.value || !authStore.currentUser) {
    return false
  }

  return board.value.author === authStore.currentUser.nickname
})

try {
  board.value = await fetchBoard(String(route.params.boardUuid))
} catch (error) {
  const fetchError = error as { data?: { message?: string } }
  errorMessage.value = fetchError.data?.message ?? '게시글 상세를 불러오지 못했습니다.'
}

async function handleDelete() {
  if (!board.value || isDeleting.value) {
    return
  }

  isDeleting.value = true

  try {
    await deleteBoard(board.value.boardPostUuid)
    await navigateTo('/board')
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '게시글 삭제에 실패했습니다.'
  } finally {
    isDeleting.value = false
  }
}
</script>

<template>
  <main class="page-container board-detail-page">
    <section class="content-panel board-detail-page__panel">
      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <template v-else-if="board">
        <div class="board-detail-page__header">
          <div>
            <p class="board-detail-page__category">{{ board.categoryName }}</p>
            <h1 class="section-title">{{ board.title }}</h1>
            <div class="meta-row">
              <span>작성자 {{ board.author }}</span>
              <span>작성일 {{ new Date(board.createdAt).toLocaleString() }}</span>
              <span>조회 {{ board.viewCount }}</span>
            </div>
          </div>

          <div v-if="isOwner" class="board-detail-page__actions">
            <button class="button-secondary" type="button" disabled>
              수정 준비 중
            </button>
            <button class="button-danger" type="button" :disabled="isDeleting" @click="handleDelete">
              {{ isDeleting ? '삭제 중...' : '삭제' }}
            </button>
          </div>
        </div>

        <div v-if="board.tags.length" class="board-detail-page__tags">
          <span v-for="tag in board.tags" :key="tag.tagUuid">
            #{{ tag.name }}
          </span>
        </div>

        <article class="board-detail-page__content">
          {{ board.content }}
        </article>
      </template>

      <p v-else class="message-muted">
        게시글을 찾는 중입니다.
      </p>
    </section>
  </main>
</template>

<style scoped>
.board-detail-page__panel {
  padding: 32px;
}

.board-detail-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.board-detail-page__category {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
}

.board-detail-page__actions,
.board-detail-page__tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.board-detail-page__tags {
  margin-top: 20px;
}

.board-detail-page__tags span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(29, 107, 99, 0.12);
  color: var(--color-accent);
}

.board-detail-page__content {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(116, 87, 51, 0.12);
  white-space: pre-wrap;
  line-height: 1.75;
}

@media (max-width: 768px) {
  .board-detail-page__header {
    flex-direction: column;
  }
}
</style>
