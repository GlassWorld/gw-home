<script setup lang="ts">
import type { BoardSummary } from '~/types/api/board'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const router = useRouter()
const { fetchBoardList } = useBoard()

const boardList = ref<BoardSummary[]>([])
const totalPages = ref(0)
const totalCount = ref(0)
const isLoading = ref(false)
const errorMessage = ref('')
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const page = ref(Number(route.query.page ?? 1) || 1)

async function loadBoardList() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchBoardList({
      keyword: keyword.value || undefined,
      page: page.value,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    })

    boardList.value = response.content
    totalPages.value = response.totalPages
    totalCount.value = response.totalCount
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '게시글 목록을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

function movePage(nextPage: number) {
  page.value = nextPage
  router.push({
    query: {
      ...route.query,
      page: String(nextPage),
      keyword: keyword.value || undefined
    }
  })
}

function handleSearch() {
  page.value = 1
  router.push({
    query: {
      ...route.query,
      page: '1',
      keyword: keyword.value || undefined
    }
  })
}

watch(
  () => route.query,
  async query => {
    keyword.value = typeof query.keyword === 'string' ? query.keyword : ''
    page.value = Number(query.page ?? 1) || 1
    await loadBoardList()
  },
  { immediate: true }
)
</script>

<template>
  <main class="page-container board-page">
    <section class="content-panel board-page__search">
      <div>
        <h1 class="section-title">게시글 목록</h1>
        <p class="section-description">키워드 검색과 페이지 이동으로 원하는 글을 빠르게 찾습니다.</p>
      </div>

      <form class="board-page__search-form" @submit.prevent="handleSearch">
        <input
          v-model="keyword"
          class="input-field"
          type="search"
          placeholder="제목 또는 키워드 검색"
        >
        <CommonBaseButton type="submit">
          검색
        </CommonBaseButton>
      </form>
    </section>

    <section class="content-panel board-page__list-panel">
      <div class="board-page__list-header">
        <p class="message-muted">총 {{ totalCount }}건</p>
        <CommonBaseButton variant="secondary" to="/dashboard">
          대시보드
        </CommonBaseButton>
      </div>

      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>
      <p v-else-if="isLoading" class="message-muted">
        게시글을 불러오는 중입니다.
      </p>
      <div v-else-if="boardList.length" class="board-page__list">
        <BoardBoardListItem
          v-for="board in boardList"
          :key="board.boardPostUuid"
          :board="board"
        />
      </div>
      <p v-else class="message-muted">
        검색 결과가 없습니다.
      </p>

      <div class="board-page__pagination">
        <CommonBaseButton
          variant="secondary"
          :disabled="page <= 1"
          @click="movePage(page - 1)"
        >
          이전
        </CommonBaseButton>
        <span>{{ page }} / {{ totalPages || 1 }}</span>
        <CommonBaseButton
          variant="secondary"
          :disabled="page >= totalPages"
          @click="movePage(page + 1)"
        >
          다음
        </CommonBaseButton>
      </div>
    </section>
  </main>
</template>

<style scoped>
.board-page {
  display: grid;
  gap: 24px;
}

.board-page__search,
.board-page__list-panel {
  padding: 28px;
}

.board-page__search {
  display: grid;
  gap: 18px;
}

.board-page__search-form {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.board-page__list-header,
.board-page__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.board-page__list {
  display: grid;
  gap: 14px;
  margin: 20px 0;
}

.board-page__pagination {
  justify-content: center;
}

@media (max-width: 768px) {
  .board-page__search-form {
    grid-template-columns: 1fr;
  }

  .board-page__list-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
