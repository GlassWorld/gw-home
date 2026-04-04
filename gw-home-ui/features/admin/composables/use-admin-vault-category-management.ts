import type { SaveVaultCategoryPayload, VaultCategory } from '~/types/vault'

const defaultVaultCategoryColor = '#6ec1ff'

function createInitialVaultCategoryForm(): SaveVaultCategoryPayload {
  return {
    name: '',
    description: '',
    sortOrder: 0,
    color: defaultVaultCategoryColor
  }
}

export function useAdminVaultCategoryManagement() {
  const { fetchAdminCategoryList, createCategory, updateCategory, removeCategory } = useVaultCategoryApi()
  const { showToast } = useToast()
  const { confirm } = useDialog()

  const categoryList = ref<VaultCategory[]>([])
  const editingCategoryUuid = ref('')
  const isSubmitting = ref(false)
  const formState = reactive<SaveVaultCategoryPayload>(createInitialVaultCategoryForm())

  async function loadCategoryList() {
    try {
      categoryList.value = await fetchAdminCategoryList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      categoryList.value = []
      showToast(fetchError.data?.message ?? '카테고리 목록을 불러오지 못했습니다.', { variant: 'error' })
    }
  }

  function resetForm() {
    editingCategoryUuid.value = ''
    Object.assign(formState, createInitialVaultCategoryForm())
  }

  function startEdit(category: VaultCategory) {
    editingCategoryUuid.value = category.categoryUuid
    formState.name = category.name
    formState.description = category.description ?? ''
    formState.sortOrder = category.sortOrder
    formState.color = category.color ?? defaultVaultCategoryColor
  }

  async function handleSubmit() {
    if (isSubmitting.value) {
      return
    }

    isSubmitting.value = true

    const payload: SaveVaultCategoryPayload = {
      name: formState.name.trim(),
      description: formState.description?.trim() || '',
      sortOrder: Number(formState.sortOrder || 0),
      color: formState.color || defaultVaultCategoryColor
    }

    try {
      if (editingCategoryUuid.value) {
        await updateCategory(editingCategoryUuid.value, payload)
        showToast('카테고리를 수정했습니다.', { variant: 'success' })
      } else {
        await createCategory(payload)
        showToast('카테고리를 등록했습니다.', { variant: 'success' })
      }

      resetForm()
      await loadCategoryList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '카테고리 저장에 실패했습니다.', { variant: 'error' })
    } finally {
      isSubmitting.value = false
    }
  }

  async function handleDelete(categoryUuid: string) {
    const shouldDelete = await confirm('카테고리를 삭제할까요?', {
      title: '카테고리 삭제',
      confirmText: '삭제',
      cancelText: '취소'
    })

    if (!shouldDelete) {
      return
    }

    try {
      await removeCategory(categoryUuid)
      showToast('카테고리를 삭제했습니다.', { variant: 'success' })
      await loadCategoryList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '카테고리 삭제에 실패했습니다.', { variant: 'error' })
    }
  }

  return {
    categoryList,
    editingCategoryUuid,
    isSubmitting,
    formState,
    resetForm,
    startEdit,
    handleSubmit,
    handleDelete,
    loadCategoryList
  }
}
