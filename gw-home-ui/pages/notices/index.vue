<script setup lang="ts">
import NoticeListPanel from '~/features/notice/components/NoticeListPanel.vue'
import NoticeDetailModal from '~/features/notice/components/NoticeDetailModal.vue'
import NoticeSearchHero from '~/features/notice/components/NoticeSearchHero.vue'
import type { NoticeDetail, NoticeSummary } from '~/types/api/notice'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const router = useRouter()
const { fetchNotice, fetchNoticeList } = useNoticeApi()

const noticeList = ref<NoticeSummary[]>([])
const totalPages = ref(0)
const totalCount = ref(0)
const isLoading = ref(false)
const errorMessage = ref('')
const selectedNotice = ref<NoticeDetail | null>(null)
const isDetailLoading = ref(false)
const detailErrorMessage = ref('')
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const page = ref(Number(route.query.page ?? 1) || 1)
const selectedNoticeUuid = computed(() =>
  typeof route.query.noticeUuid === 'string' ? route.query.noticeUuid : ''
)
const isDetailModalVisible = computed(() => Boolean(selectedNoticeUuid.value))

function buildQuery(nextPage: number) {
  return {
    ...route.query,
    page: String(nextPage),
    keyword: keyword.value || undefined,
    noticeUuid: undefined
  }
}

function isSameQuery(nextQuery: ReturnType<typeof buildQuery>) {
  const currentKeyword = typeof route.query.keyword === 'string' ? route.query.keyword : undefined
  const currentPage = typeof route.query.page === 'string' ? route.query.page : '1'

  return currentPage === nextQuery.page && currentKeyword === nextQuery.keyword
}

async function loadNoticeList() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchNoticeList({
      keyword: keyword.value || undefined,
      page: page.value,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    })

    noticeList.value = response.content
    totalPages.value = response.totalPages
    totalCount.value = response.totalCount
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '공지사항 목록을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

async function loadSelectedNotice(noticeUuid: string) {
  isDetailLoading.value = true
  detailErrorMessage.value = ''
  selectedNotice.value = null

  try {
    selectedNotice.value = await fetchNotice(noticeUuid)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    detailErrorMessage.value = fetchError.data?.message ?? '공지사항 상세를 불러오지 못했습니다.'
  } finally {
    isDetailLoading.value = false
  }
}

async function openNoticeDetail(noticeUuid: string) {
  await router.push({
    query: {
      ...route.query,
      noticeUuid
    }
  })
}

async function closeNoticeDetail() {
  await router.push({
    query: {
      ...route.query,
      noticeUuid: undefined
    }
  })
}

async function handleSearch() {
  page.value = 1
  const nextQuery = buildQuery(1)

  if (isSameQuery(nextQuery)) {
    await loadNoticeList()
    return
  }

  await router.push({ query: nextQuery })
}

async function movePage(nextPage: number) {
  page.value = nextPage
  const nextQuery = buildQuery(nextPage)

  if (isSameQuery(nextQuery)) {
    await loadNoticeList()
    return
  }

  await router.push({ query: nextQuery })
}

watch(
  () => [route.query.keyword, route.query.page],
  async ([nextKeyword, nextPage]) => {
    keyword.value = typeof nextKeyword === 'string' ? nextKeyword : ''
    page.value = Number(nextPage ?? 1) || 1
    await loadNoticeList()
  },
  { immediate: true }
)

watch(
  selectedNoticeUuid,
  async noticeUuid => {
    if (!noticeUuid) {
      selectedNotice.value = null
      detailErrorMessage.value = ''
      isDetailLoading.value = false
      return
    }

    await loadSelectedNotice(noticeUuid)
  },
  { immediate: true }
)
</script>

<template>
  <main class="page-container notice-page">
    <NoticeSearchHero
      :keyword="keyword"
      @update-keyword="keyword = $event"
      @submit="handleSearch"
    />

    <NoticeListPanel
      :notices="noticeList"
      :total-count="totalCount"
      :is-loading="isLoading"
      :error-message="errorMessage"
      :page="page"
      :total-pages="totalPages"
      @open="openNoticeDetail"
      @previous="movePage(page - 1)"
      @next="movePage(page + 1)"
    />

    <NoticeDetailModal
      :visible="isDetailModalVisible"
      :notice="selectedNotice"
      :is-loading="isDetailLoading"
      :error-message="detailErrorMessage"
      @close="closeNoticeDetail"
    />
  </main>
</template>

<style scoped>
.notice-page {
  display: grid;
  gap: 24px;
}

</style>
