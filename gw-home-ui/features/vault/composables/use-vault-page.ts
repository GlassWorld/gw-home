import type { Credential, VaultCategory } from '~/types/vault'

function normalizeCategoryUuids(categoryUuids: readonly string[]): string[] {
  const normalizedCategoryUuids = new Set<string>()

  for (const categoryUuid of categoryUuids) {
    const normalizedCategoryUuid = categoryUuid.trim()

    if (normalizedCategoryUuid) {
      normalizedCategoryUuids.add(normalizedCategoryUuid)
    }
  }

  return [...normalizedCategoryUuids]
}

function normalizeKeyword(value: string): string {
  return value.trim().replace(/\s+/g, ' ')
}

export function useVaultPage() {
  const authStore = useAuthStore()
  const route = useRoute()
  const router = useRouter()
  const { fetchCredentialList } = useVaultApi()
  const { fetchCategoryList } = useVaultCategoryApi()
  const { showToast } = useToast()

  const credentialList = ref<Credential[]>([])
  const categoryList = ref<VaultCategory[]>([])
  const selectedCredential = ref<Credential | null>(null)
  const isLoading = ref(false)
  const isFormVisible = ref(false)
  const isDetailVisible = ref(false)
  const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')

  function getCategoryUuidsFromQuery(): string[] {
    const queryCategoryUuids = route.query.categoryUuids
    const legacyCategoryUuid = route.query.categoryUuid

    if (Array.isArray(queryCategoryUuids)) {
      return normalizeCategoryUuids(queryCategoryUuids.filter((value): value is string => typeof value === 'string'))
    }

    if (typeof queryCategoryUuids === 'string') {
      return normalizeCategoryUuids(queryCategoryUuids.split(','))
    }

    if (typeof legacyCategoryUuid === 'string') {
      return normalizeCategoryUuids([legacyCategoryUuid])
    }

    return []
  }

  const selectedCategoryUuids = ref<string[]>(getCategoryUuidsFromQuery())

  function pushQuery() {
    void router.push({
      query: {
        ...route.query,
        keyword: keyword.value || undefined,
        categoryUuids: selectedCategoryUuids.value.length ? selectedCategoryUuids.value : undefined,
        categoryUuid: undefined
      }
    })
  }

  async function loadCredentialList() {
    isLoading.value = true

    try {
      credentialList.value = await fetchCredentialList({
        keyword: normalizeKeyword(keyword.value) || undefined,
        categoryUuids: selectedCategoryUuids.value
      })
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      credentialList.value = []
      showToast(fetchError.data?.message ?? '자격증명 목록을 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoading.value = false
    }
  }

  async function loadCategoryList() {
    categoryList.value = await fetchCategoryList()
  }

  function openDetail(credential: Credential) {
    selectedCredential.value = credential
    isDetailVisible.value = true
  }

  function openCreateModal() {
    selectedCredential.value = null
    isFormVisible.value = true
  }

  function openEditModal(credential: Credential) {
    selectedCredential.value = credential
    isDetailVisible.value = false
    isFormVisible.value = true
  }

  function handleSearch() {
    keyword.value = normalizeKeyword(keyword.value)
    pushQuery()
  }

  function handleCategorySelect(categoryUuid: string) {
    const selectedCategoryUuidSet = new Set(selectedCategoryUuids.value)

    if (selectedCategoryUuidSet.has(categoryUuid)) {
      selectedCategoryUuidSet.delete(categoryUuid)
    } else {
      selectedCategoryUuidSet.add(categoryUuid)
    }

    selectedCategoryUuids.value = [...selectedCategoryUuidSet]
    pushQuery()
  }

  function clearCategoryFilter() {
    selectedCategoryUuids.value = []
    pushQuery()
  }

  async function handleSaved() {
    isFormVisible.value = false
    await loadCredentialList()
    showToast('저장되었습니다.', { variant: 'success' })
  }

  async function handleDeleted() {
    isDetailVisible.value = false
    selectedCredential.value = null
    await loadCredentialList()
    showToast('삭제되었습니다.', { variant: 'success' })
  }

  async function copyCredential(credential: Credential) {
    try {
      await navigator.clipboard.writeText(credential.password)
      showToast('복사되었습니다.', { variant: 'success' })
    } catch {
      showToast('클립보드 복사에 실패했습니다.', { variant: 'error' })
    }
  }

  watch(
    () => [route.query.keyword, route.query.categoryUuids, route.query.categoryUuid],
    async ([queryKeyword]) => {
      keyword.value = typeof queryKeyword === 'string' ? queryKeyword : ''
      selectedCategoryUuids.value = getCategoryUuidsFromQuery()
      await loadCredentialList()
    },
    { immediate: true }
  )

  return {
    authStore,
    credentialList,
    categoryList,
    selectedCredential,
    isLoading,
    isFormVisible,
    isDetailVisible,
    keyword,
    selectedCategoryUuids,
    loadCategoryList,
    openDetail,
    openCreateModal,
    openEditModal,
    handleSearch,
    handleCategorySelect,
    clearCategoryFilter,
    handleSaved,
    handleDeleted,
    copyCredential
  }
}
