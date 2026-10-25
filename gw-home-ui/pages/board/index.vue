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
  <main class="page-container page-stack board-page">
    <section class="content-panel page-panel-padding-lg">
      <CommonBasePageHeader title="게시글 목록" eyebrow="Board" description="키워드 검색과 페이지 이동으로 원하는 글을 빠르게 찾습니다.">
        <template #actions>
          <CommonBaseButton variant="secondary" to="/dashboard">대시보드</CommonBaseButton>
          <CommonBaseButton to="/board/create">글쓰기</CommonBaseButton>
        </template>
        <template #summary>
          <p class="board-page__summary" role="status">총 <strong>{{ totalCount }}</strong>건</p>
        </template>
      </CommonBasePageHeader>
    </section>

    <section class="content-panel page-panel-padding-lg page-stack">
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

      <CommonBaseState v-if="errorMessage" state="error" :message="errorMessage" action-label="다시 시도" @action="loadBoardList" />
      <CommonBaseState v-else-if="isLoading" state="loading" message="게시글을 불러오는 중입니다." />
      <div v-else-if="boardList.length" class="board-page__list">
        <BoardListItem
          v-for="board in boardList"
          :key="board.boardPostUuid"
          :board="board"
        />
      </div>
      <CommonBaseState v-else state="empty" message="검색 결과가 없습니다. 검색 조건을 바꿔보세요." />

      <CommonBasePagination :page="page" :total-pages="totalPages" :disabled="isLoading"
        @previous="movePage(page - 1)" @next="movePage(page + 1)" />
    </section>
  </main>
</template>

<style scoped>
.board-page__summary { margin: 0; color: var(--color-text-muted); text-align: right; font-size: 0.875rem; }
.board-page__summary strong { color: var(--color-text); }
.board-page__filters { display: grid; grid-template-columns: minmax(140px, 0.8fr) minmax(0, 1.6fr) auto; align-items: end; gap: var(--space-3); }
.board-page__filters label { display: grid; min-width: 0; gap: var(--space-2); }
.board-page__filters label span { color: var(--color-text-muted); font-size: 0.875rem; font-weight: 600; }
.board-page__list { display: grid; gap: var(--space-3); }
@media (max-width: 767px) {
  .board-page__filters { grid-template-columns: minmax(0, 1fr) auto; }
  .board-page__filters > label:first-child { grid-column: 1 / -1; }
  .board-page__summary { text-align: left; }
}
</style>
