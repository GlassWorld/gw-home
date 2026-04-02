import type {
  SaveWorkUnitPayload,
  WorkGitProject,
  WorkUnit,
  WorkUnitOption,
  WorkUnitSort,
  WorkUnitStatus
} from '../types/work.types'
import { useWorkGitApi } from '../api/work-git.api'
import { useWorkUnitApi } from '../api/work-unit.api'
import { applySelectableValueFromOptions } from '~/utils/selectable'

interface WorkUnitFilterState {
  keyword: string
  category: string
  status: WorkUnitStatus | ''
  includeInactive: boolean
  sort: WorkUnitSort
}

export function useWorkUnitManagement() {
  const { fetchWorkUnitList, fetchWorkUnitOptions, createWorkUnit, updateWorkUnit, updateWorkUnitUse } = useWorkUnitApi()
  const { fetchGitProjectOptions } = useWorkGitApi()
  const { showToast } = useToast()
  const { confirm } = useDialog()

  const workUnitList = ref<WorkUnit[]>([])
  const workUnitOptions = ref<WorkUnitOption[]>([])
  const availableGitProjects = ref<WorkGitProject[]>([])
  const editingWorkUnitUuid = ref('')
  const isLoading = ref(false)
  const isSubmitting = ref(false)
  const isFormVisible = ref(false)

  const filters = reactive<WorkUnitFilterState>({
    keyword: '',
    category: '',
    status: '',
    includeInactive: false,
    sort: 'updated'
  })

  const formState = ref<SaveWorkUnitPayload>({
    title: '',
    category: '',
    description: '',
    status: 'IN_PROGRESS',
    gitProjectUuids: []
  })

  const statusLabelMap: Record<WorkUnitStatus, string> = {
    IN_PROGRESS: '진행중',
    DONE: '완료',
    ON_HOLD: '보류'
  }

  const statusFilterOptions: Array<{ value: WorkUnitStatus | ''; label: string }> = [
    { value: '', label: '전체' },
    { value: 'IN_PROGRESS', label: '진행중' },
    { value: 'DONE', label: '완료' },
    { value: 'ON_HOLD', label: '보류' }
  ]

  const sortOptions: Array<{ value: WorkUnitSort; label: string }> = [
    { value: 'updated', label: '최근 수정순' },
    { value: 'recent', label: '최근 사용순' },
    { value: 'frequent', label: '자주 사용순' }
  ]

  const categoryOptions = computed(() => {
    const categorySet = new Set<string>()

    for (const workUnit of workUnitOptions.value) {
      if (workUnit.category) {
        categorySet.add(workUnit.category)
      }
    }

    return Array.from(categorySet).sort((left, right) => left.localeCompare(right))
  })

  const categoryFilterOptions = computed(() => [
    { value: '', label: '전체' },
    ...categoryOptions.value.map((category) => ({ value: category, label: category }))
  ])

  const summary = computed(() => {
    const activeCount = workUnitList.value.filter((workUnit) => workUnit.useYn === 'Y').length
    const inactiveCount = workUnitList.value.filter((workUnit) => workUnit.useYn === 'N').length

    return {
      total: workUnitList.value.length,
      active: activeCount,
      inactive: inactiveCount
    }
  })

  function resetForm() {
    editingWorkUnitUuid.value = ''
    formState.value = {
      title: '',
      category: '',
      description: '',
      status: 'IN_PROGRESS',
      gitProjectUuids: []
    }
  }

  function startCreate() {
    resetForm()
    isFormVisible.value = true
  }

  function startEdit(workUnit: WorkUnit) {
    isFormVisible.value = true
    editingWorkUnitUuid.value = workUnit.workUnitUuid
    formState.value = {
      title: workUnit.title,
      category: workUnit.category ?? '',
      description: workUnit.description ?? '',
      status: workUnit.status,
      gitProjectUuids: workUnit.gitProjects.map((gitProject) => gitProject.gitProjectUuid)
    }
  }

  async function loadWorkUnitList() {
    isLoading.value = true

    try {
      workUnitList.value = await fetchWorkUnitList({
        keyword: filters.keyword || undefined,
        category: filters.category || undefined,
        status: filters.status,
        useYn: filters.includeInactive ? '' : 'Y',
        sort: filters.sort
      })
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      workUnitList.value = []
      showToast(fetchError.data?.message ?? '업무 목록을 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoading.value = false
    }
  }

  async function loadWorkUnitOptions() {
    try {
      workUnitOptions.value = await fetchWorkUnitOptions(true)
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      workUnitOptions.value = []
      showToast(fetchError.data?.message ?? '업무 옵션을 불러오지 못했습니다.', { variant: 'error' })
    }
  }

  async function loadGitProjectOptions() {
    try {
      availableGitProjects.value = await fetchGitProjectOptions()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      availableGitProjects.value = []
      showToast(fetchError.data?.message ?? 'Git 프로젝트 목록을 불러오지 못했습니다.', { variant: 'error' })
    }
  }

  async function reloadAll() {
    await Promise.all([loadWorkUnitList(), loadWorkUnitOptions(), loadGitProjectOptions()])
  }

  async function handleSubmit() {
    if (isSubmitting.value) {
      return
    }

    if (!formState.value.title.trim()) {
      showToast('업무명을 입력해주세요.', { variant: 'error' })
      return
    }

    isSubmitting.value = true

    const payload: SaveWorkUnitPayload = {
      title: formState.value.title.trim(),
      category: formState.value.category?.trim() || '',
      description: formState.value.description?.trim() || '',
      status: formState.value.status,
      gitProjectUuids: [...new Set(formState.value.gitProjectUuids)]
    }

    try {
      if (editingWorkUnitUuid.value) {
        await updateWorkUnit(editingWorkUnitUuid.value, payload)
        showToast('업무를 수정했습니다.', { variant: 'success' })
      } else {
        await createWorkUnit(payload)
        showToast('업무를 등록했습니다.', { variant: 'success' })
      }

      resetForm()
      isFormVisible.value = false
      await reloadAll()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '업무 저장에 실패했습니다.', { variant: 'error' })
    } finally {
      isSubmitting.value = false
    }
  }

  function updateCategoryFilter(category: string) {
    filters.category = category
  }

  function updateStatusFilter(status: string) {
    applySelectableValueFromOptions(status, statusFilterOptions, (validStatus) => {
      filters.status = validStatus
    })
  }

  function updateSortFilter(sort: string) {
    applySelectableValueFromOptions(sort, sortOptions, (validSort) => {
      filters.sort = validSort
    })
  }

  async function handleUseToggle(workUnit: WorkUnit) {
    const nextUseYn = workUnit.useYn === 'Y' ? 'N' : 'Y'
    const shouldUpdate = await confirm(
      nextUseYn === 'N' ? '이 업무를 사용안함 처리할까요?' : '이 업무를 다시 사용하도록 전환할까요?',
      {
        title: nextUseYn === 'N' ? '업무 사용안함' : '업무 재사용',
        confirmText: nextUseYn === 'N' ? '사용안함' : '재사용',
        cancelText: '취소'
      }
    )

    if (!shouldUpdate) {
      return
    }

    try {
      await updateWorkUnitUse(workUnit.workUnitUuid, nextUseYn)

      if (editingWorkUnitUuid.value === workUnit.workUnitUuid && nextUseYn === 'N' && !filters.includeInactive) {
        resetForm()
      }

      showToast(nextUseYn === 'N' ? '업무를 사용안함 처리했습니다.' : '업무를 다시 활성화했습니다.', {
        variant: 'success'
      })
      await reloadAll()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '업무 상태 변경에 실패했습니다.', { variant: 'error' })
    }
  }

  function handleCancelForm() {
    resetForm()
    isFormVisible.value = false
  }

  async function applyFilters() {
    await loadWorkUnitList()
  }

  watch(
    () => filters.includeInactive,
    async () => {
      await loadWorkUnitList()
    }
  )

  onMounted(() => {
    void reloadAll()
  })

  return {
    availableGitProjects,
    categoryFilterOptions,
    editingWorkUnitUuid,
    filters,
    formState,
    isFormVisible,
    isLoading,
    isSubmitting,
    sortOptions,
    startCreate,
    startEdit,
    statusFilterOptions,
    statusLabelMap,
    summary,
    updateCategoryFilter,
    updateStatusFilter,
    updateSortFilter,
    workUnitList,
    handleSubmit,
    handleUseToggle,
    handleCancelForm,
    applyFilters
  }
}
