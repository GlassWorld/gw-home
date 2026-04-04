import type { NoticeDetail, NoticeSummary, SaveNoticeForm } from '~/types/api/notice'
import { formatDateTime } from '~/utils/date'

export function useAdminNoticeManagement() {
  const { fetchAdminNoticeList, createNotice, updateNotice, deleteNotice, fetchAdminNotice } = useNoticeApi()
  const { showToast } = useToast()
  const { confirm } = useDialog()

  const noticeList = ref<NoticeSummary[]>([])
  const keyword = ref('')
  const page = ref(1)
  const totalPages = ref(0)
  const isSubmitting = ref(false)
  const isLoading = ref(false)
  const isPreviewLoading = ref(false)
  const isEditorVisible = ref(false)
  const editingNoticeUuid = ref('')
  const previewNotice = ref<NoticeDetail | null>(null)
  const previewNoticeUuid = ref('')
  const formState = reactive<SaveNoticeForm>({
    title: '',
    content: ''
  })

  async function loadNoticeList() {
    isLoading.value = true

    try {
      const response = await fetchAdminNoticeList({
        keyword: keyword.value || undefined,
        page: page.value,
        size: 10,
        sortBy: 'createdAt',
        sortDirection: 'DESC'
      })

      noticeList.value = response.content
      totalPages.value = response.totalPages
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      noticeList.value = []
      showToast(fetchError.data?.message ?? '공지 목록을 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoading.value = false
    }
  }

  function resetForm() {
    editingNoticeUuid.value = ''
    formState.title = ''
    formState.content = ''
  }

  function openCreateModal() {
    resetForm()
    isEditorVisible.value = true
  }

  function closeEditorModal() {
    isEditorVisible.value = false
    resetForm()
  }

  async function startEdit(noticeUuid: string) {
    try {
      const notice = await fetchAdminNotice(noticeUuid)
      editingNoticeUuid.value = notice.noticeUuid
      formState.title = notice.title
      formState.content = notice.content
      isEditorVisible.value = true
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '공지 상세를 불러오지 못했습니다.', { variant: 'error' })
    }
  }

  async function openPreview(noticeUuid: string) {
    isPreviewLoading.value = true
    previewNoticeUuid.value = noticeUuid
    previewNotice.value = null

    try {
      previewNotice.value = await fetchAdminNotice(noticeUuid)
    } catch (error) {
      previewNoticeUuid.value = ''
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '공지 상세를 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isPreviewLoading.value = false
    }
  }

  function closePreview() {
    previewNoticeUuid.value = ''
    previewNotice.value = null
    isPreviewLoading.value = false
  }

  async function handleSubmit() {
    if (isSubmitting.value) {
      return
    }

    isSubmitting.value = true

    try {
      const payload: SaveNoticeForm = {
        title: formState.title.trim(),
        content: formState.content.trim()
      }

      if (editingNoticeUuid.value) {
        await updateNotice(editingNoticeUuid.value, payload)
        showToast('공지사항을 수정했습니다.', { variant: 'success' })
      } else {
        await createNotice(payload)
        showToast('공지사항을 등록했습니다.', { variant: 'success' })
      }

      closeEditorModal()
      await loadNoticeList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '공지사항 저장에 실패했습니다.', { variant: 'error' })
    } finally {
      isSubmitting.value = false
    }
  }

  async function handleDelete(noticeUuid: string) {
    const shouldDelete = await confirm('공지사항을 삭제할까요?', {
      title: '공지사항 삭제',
      confirmText: '삭제',
      cancelText: '취소'
    })

    if (!shouldDelete) {
      return
    }

    try {
      await deleteNotice(noticeUuid)
      showToast('공지사항을 삭제했습니다.', { variant: 'success' })

      if (editingNoticeUuid.value === noticeUuid) {
        resetForm()
      }

      await loadNoticeList()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '공지사항 삭제에 실패했습니다.', { variant: 'error' })
    }
  }

  async function handleSearch() {
    page.value = 1
    await loadNoticeList()
  }

  async function movePage(nextPage: number) {
    page.value = nextPage
    await loadNoticeList()
  }

  return {
    noticeList,
    keyword,
    page,
    totalPages,
    isSubmitting,
    isLoading,
    isPreviewLoading,
    isEditorVisible,
    editingNoticeUuid,
    previewNotice,
    previewNoticeUuid,
    formState,
    formatDateTime,
    loadNoticeList,
    openCreateModal,
    closeEditorModal,
    startEdit,
    openPreview,
    closePreview,
    handleSubmit,
    handleDelete,
    handleSearch,
    movePage
  }
}
