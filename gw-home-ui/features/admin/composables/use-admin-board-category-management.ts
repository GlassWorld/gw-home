import type { BoardCategory, SaveBoardCategoryPayload } from '~/types/api/board'

function createInitialBoardCategoryForm(): SaveBoardCategoryPayload {
  return {
    name: '',
    sortOrder: 0
  }
}

export function useAdminBoardCategoryManagement() {
  const { fetchAdminBoardCategoryList, createBoardCategory, updateBoardCategory, removeBoardCategory } = useBoardCategoryApi()
  const { showToast } = useToast()
  const { confirm } = useDialog()

  const categoryList = ref<BoardCategory[]>([])
  const editingCategoryUuid = ref('')
  const isSubmitting = ref(false)
  const formState = reactive<SaveBoardCategoryPayload>(createInitialBoardCategoryForm())

  async function loadCategoryList() {
    try {
      categoryList.value = await fetchAdminBoardCategoryList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      categoryList.value = []
      showToast(fetchError.data?.message ?? '게시글 카테고리 목록을 불러오지 못했습니다.', { variant: 'error' })
    }
  }

  function resetForm() {
    editingCategoryUuid.value = ''
    Object.assign(formState, createInitialBoardCategoryForm())
  }

  function startEdit(category: BoardCategory) {
    editingCategoryUuid.value = category.categoryUuid
    formState.name = category.categoryName
    formState.sortOrder = category.sortOrder
  }

  async function handleSubmit() {
    if (isSubmitting.value) {
      return
    }

    isSubmitting.value = true

    const payload: SaveBoardCategoryPayload = {
      name: formState.name.trim(),
      sortOrder: Number(formState.sortOrder || 0)
    }

    try {
      if (editingCategoryUuid.value) {
        await updateBoardCategory(editingCategoryUuid.value, payload)
        showToast('게시글 카테고리를 수정했습니다.', { variant: 'success' })
      } else {
        await createBoardCategory(payload)
        showToast('게시글 카테고리를 등록했습니다.', { variant: 'success' })
      }

      resetForm()
      await loadCategoryList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '게시글 카테고리 저장에 실패했습니다.', { variant: 'error' })
    } finally {
      isSubmitting.value = false
    }
  }

  async function handleDelete(categoryUuid: string) {
    const shouldDelete = await confirm('게시글 카테고리를 삭제할까요?', {
      title: '게시글 카테고리 삭제',
      confirmText: '삭제',
      cancelText: '취소'
    })

    if (!shouldDelete) {
      return
    }

    try {
      await removeBoardCategory(categoryUuid)
      showToast('게시글 카테고리를 삭제했습니다.', { variant: 'success' })
      await loadCategoryList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '게시글 카테고리 삭제에 실패했습니다.', { variant: 'error' })
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
