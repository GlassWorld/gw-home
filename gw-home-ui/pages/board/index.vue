<script setup lang="ts">
import type { BoardListParams } from '~/types/api/board'
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
const defaultSearchType: NonNullable<BoardListParams['searchType']> = 'title'
const searchType = ref<NonNullable<BoardListParams['searchType']>>(
  route.query.searchType === 'content' || route.query.searchType === 'author' || route.query.searchType === 'all'
    ? route.query.searchType
    : defaultSearchType
)
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const page = ref(Number(route.query.page ?? 1) || 1)

async function loadBoardList() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchBoardList({
      searchType: searchType.value,
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
      searchType: searchType.value,
      keyword: keyword.value || undefined
    }
  })
}

function handleSearch() {
  page.value = 1
  router.push({
    query: {
      page: '1',
      searchType: searchType.value,
      keyword: keyword.value || undefined
    }
  })
}

watch(
  () => route.query,
  async query => {
    searchType.value = query.searchType === 'content' || query.searchType === 'author' || query.searchType === 'all'
      ? query.searchType
      : defaultSearchType
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
      <div class="board-page__search-header">
        <div>
          <h1 class="section-title">게시글 목록</h1>
          <p class="section-description">키워드 검색과 페이지 이동으로 원하는 글을 빠르게 찾습니다.</p>
        </div>
        <div class="board-page__hero-side">
          <div class="board-page__header-actions">
            <CommonBaseButton variant="secondary" to="/dashboard">
              대시보드
            </CommonBaseButton>
            <CommonBaseButton to="/board/create">
              글쓰기
            </CommonBaseButton>
          </div>
          <div class="board-page__summary" role="status" aria-label="게시글 요약">
            <span>총 <strong>{{ totalCount }}</strong>건</span>
          </div>
        </div>
      </div>
    </section>

    <section class="content-panel board-page__list-panel">
      <form class="board-page__filters" @submit.prevent="handleSearch">
        <label>
          <span>검색 조건</span>
          <select v-model="searchType" class="input-field">
            <option value="title">
              제목
            </option>
            <option value="content">
              본문
            </option>
            <option value="author">
              작성자
            </option>
            <option value="all">
              전체
            </option>
          </select>
        </label>
        <label class="board-page__search-field">
          <span>검색어</span>
          <input
            v-model="keyword"
            class="input-field"
            type="search"
            placeholder="검색어를 입력하세요"
          >
        </label>
        <div class="board-page__filter-actions">
          <CommonBaseButton type="submit">
            검색
          </CommonBaseButton>
        </div>
      </form>

      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>
      <p v-else-if="isLoading" class="message-muted">
        게시글을 불러오는 중입니다.
      </p>
      <div v-else-if="boardList.length" class="board-page__list">
        <BoardListItem
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
  padding: 28px;
}

.board-page__search,
.board-page__list-panel {
  padding: 28px;
}

.board-page__search {
  display: grid;
  gap: 18px;
  margin-bottom: 24px;
}

.board-page__search-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.board-page__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.board-page__hero-side {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.board-page__header-actions {
  display: flex;
  gap: 12px;
}

.board-page__summary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.board-page__summary strong {
  color: var(--color-text);
  font-size: 0.9rem;
}

.board-page__filters {
  display: grid;
  grid-template-columns: minmax(160px, 0.9fr) minmax(280px, 1.6fr) auto;
  align-items: end;
  gap: 14px 16px;
  margin-top: 16px;
}

.board-page__filters label {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.board-page__filters label span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.board-page__filter-actions {
  display: flex;
  justify-content: flex-end;
  white-space: nowrap;
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
  .board-page__filters {
    grid-template-columns: 1fr;
  }

  .board-page__search-header,
  .board-page__pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .board-page__hero-side {
    justify-content: flex-start;
  }

  .board-page__header-actions {
    flex-direction: column;
  }
}
</style>
