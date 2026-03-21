<script setup lang="ts">
import type { BoardSummary } from '~/types/api/board'

definePageMeta({
  middleware: 'auth'
})

const authStore = useAuthStore()
const { fetchBoardList } = useBoard()
const recentBoards = ref<BoardSummary[]>([])
const errorMessage = ref('')

try {
  const response = await fetchBoardList({
    page: 1,
    size: 5,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  })

  recentBoards.value = response.content
} catch (error) {
  const fetchError = error as { data?: { message?: string } }
  errorMessage.value = fetchError.data?.message ?? '최근 게시글을 불러오지 못했습니다.'
}
</script>

<template>
  <main class="page-container dashboard-page">
    <section class="dashboard-page__hero content-panel">
      <div>
        <p class="dashboard-page__eyebrow">Dashboard</p>
        <h1 class="section-title">{{ authStore.currentUser?.nickname }}님, 다시 오셨네요.</h1>
        <p class="section-description">
          {{ authStore.currentUser?.email }} 계정으로 로그인 중이며, 최근 글 흐름을 바로 확인할 수 있습니다.
        </p>
      </div>

      <div class="dashboard-page__profile-card">
        <span>Login ID</span>
        <strong>{{ authStore.currentUser?.loginId }}</strong>
        <span>권한 {{ authStore.currentUser?.role }}</span>
      </div>
    </section>

    <section class="dashboard-page__section content-panel">
      <div class="dashboard-page__section-header">
        <div>
          <h2>최근 게시글</h2>
          <p class="message-muted">최신 5건을 먼저 보여줍니다.</p>
        </div>

        <div class="dashboard-page__actions">
          <NuxtLink class="button-secondary" to="/board">
            전체 게시글
          </NuxtLink>
          <NuxtLink class="button-primary" to="/board/create">
            게시글 작성
          </NuxtLink>
        </div>
      </div>

      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <div v-else-if="recentBoards.length" class="dashboard-page__board-list">
        <BoardBoardListItem
          v-for="board in recentBoards"
          :key="board.boardPostUuid"
          :board="board"
        />
      </div>

      <p v-else class="message-muted">
        표시할 게시글이 아직 없습니다.
      </p>
    </section>
  </main>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 24px;
}

.dashboard-page__hero,
.dashboard-page__section {
  padding: 28px;
}

.dashboard-page__hero {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: 20px;
  align-items: center;
}

.dashboard-page__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
}

.dashboard-page__profile-card {
  display: grid;
  gap: 8px;
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.66);
}

.dashboard-page__profile-card span {
  color: var(--color-text-muted);
}

.dashboard-page__profile-card strong {
  font-size: 1.4rem;
}

.dashboard-page__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.dashboard-page__section-header h2 {
  margin: 0;
}

.dashboard-page__actions,
.dashboard-page__board-list {
  display: grid;
  gap: 14px;
}

@media (max-width: 768px) {
  .dashboard-page__hero {
    grid-template-columns: 1fr;
  }

  .dashboard-page__section-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
