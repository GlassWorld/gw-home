import type {
  SaveWorkGitAccountPayload,
  SaveWorkGitProjectPayload,
  WorkGitAccount,
  WorkGitProject,
  WorkGitProvider
} from '../types/work.types'
import { useWorkGitApi } from '../api/work-git.api'
import { applySelectableValueFromOptions } from '~/utils/selectable'

interface GitAccountFormState {
  accountLabel: string
  provider: WorkGitProvider
  authorName: string
  accessToken: string
  useYn: 'Y' | 'N'
}

interface GitProjectFormState {
  gitAccountUuid: string
  projectName: string
  repositoryUrl: string
  useYn: 'Y' | 'N'
}

export function useWorkGitManagement() {
  const {
    fetchGitAccounts,
    createGitAccount,
    updateGitAccount,
    deleteGitAccount,
    fetchGitProjects,
    createGitProject,
    updateGitProject,
    deleteGitProject,
    testGitProjectConnection
  } = useWorkGitApi()
  const { showToast } = useToast()
  const { confirm } = useDialog()

  const gitAccounts = ref<WorkGitAccount[]>([])
  const gitProjects = ref<WorkGitProject[]>([])
  const selectedAccountFilterUuid = ref('')
  const editingGitAccountUuid = ref('')
  const editingGitProjectUuid = ref('')
  const isLoading = ref(false)
  const isAccountSubmitting = ref(false)
  const isProjectSubmitting = ref(false)
  const testingGitProjectUuid = ref('')
  const isAccountModalVisible = ref(false)
  const isProjectModalVisible = ref(false)

  const useYnOptions: Array<{ value: 'Y' | 'N'; label: string }> = [
    { value: 'Y', label: '사용' },
    { value: 'N', label: '사용안함' }
  ]

  const accountFilterOptions = computed(() => [
    { value: '', label: '전체 계정' },
    ...gitAccounts.value.map((gitAccount) => ({
      value: gitAccount.gitAccountUuid,
      label: `${gitAccount.accountLabel} · ${gitAccount.provider}`
    }))
  ])

  const visibleGitProjects = computed(() => {
    if (!selectedAccountFilterUuid.value) {
      return gitProjects.value
    }

    return gitProjects.value.filter((gitProject) => gitProject.gitAccountUuid === selectedAccountFilterUuid.value)
  })

  const summary = computed(() => ({
    accountCount: gitAccounts.value.length,
    activeAccountCount: gitAccounts.value.filter((gitAccount) => gitAccount.useYn === 'Y').length,
    projectCount: gitProjects.value.length,
    activeProjectCount: gitProjects.value.filter((gitProject) => gitProject.useYn === 'Y').length
  }))

  const accountForm = reactive<GitAccountFormState>({
    accountLabel: '',
    provider: 'GITLAB',
    authorName: '',
    accessToken: '',
    useYn: 'Y'
  })

  const projectForm = reactive<GitProjectFormState>({
    gitAccountUuid: '',
    projectName: '',
    repositoryUrl: '',
    useYn: 'Y'
  })

  function resetAccountForm() {
    editingGitAccountUuid.value = ''
    accountForm.accountLabel = ''
    accountForm.provider = 'GITLAB'
    accountForm.authorName = ''
    accountForm.accessToken = ''
    accountForm.useYn = 'Y'
  }

  function resetProjectForm() {
    editingGitProjectUuid.value = ''
    projectForm.gitAccountUuid = selectedAccountFilterUuid.value || gitAccounts.value[0]?.gitAccountUuid || ''
    projectForm.projectName = ''
    projectForm.repositoryUrl = ''
    projectForm.useYn = 'Y'
  }

  function openCreateAccountModal() {
    resetAccountForm()
    isAccountModalVisible.value = true
  }

  function openEditAccountModal(gitAccount: WorkGitAccount) {
    editingGitAccountUuid.value = gitAccount.gitAccountUuid
    accountForm.accountLabel = gitAccount.accountLabel
    accountForm.provider = gitAccount.provider
    accountForm.authorName = gitAccount.authorName
    accountForm.accessToken = ''
    accountForm.useYn = gitAccount.useYn
    isAccountModalVisible.value = true
  }

  function openCreateProjectModal() {
    resetProjectForm()
    isProjectModalVisible.value = true
  }

  function openEditProjectModal(gitProject: WorkGitProject) {
    editingGitProjectUuid.value = gitProject.gitProjectUuid
    projectForm.gitAccountUuid = gitProject.gitAccountUuid
    projectForm.projectName = gitProject.projectName
    projectForm.repositoryUrl = gitProject.repositoryUrl
    projectForm.useYn = gitProject.useYn
    isProjectModalVisible.value = true
  }

  function updateAccountUseYn(value: string) {
    applySelectableValueFromOptions(value, useYnOptions, (useYn) => {
      accountForm.useYn = useYn
    })
  }

  function updateProjectUseYn(value: string) {
    applySelectableValueFromOptions(value, useYnOptions, (useYn) => {
      projectForm.useYn = useYn
    })
  }

  function updateAccountFilter(value: string) {
    selectedAccountFilterUuid.value = value
  }

  function updateProjectAccount(value: string) {
    if (gitAccounts.value.some((gitAccount) => gitAccount.gitAccountUuid === value)) {
      projectForm.gitAccountUuid = value
    }
  }

  async function loadGitResources() {
    isLoading.value = true

    try {
      const [nextAccounts, nextProjects] = await Promise.all([
        fetchGitAccounts(),
        fetchGitProjects()
      ])

      gitAccounts.value = nextAccounts
      gitProjects.value = nextProjects

      if (selectedAccountFilterUuid.value && !nextAccounts.some((gitAccount) => gitAccount.gitAccountUuid === selectedAccountFilterUuid.value)) {
        selectedAccountFilterUuid.value = ''
      }
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      gitAccounts.value = []
      gitProjects.value = []
      showToast(fetchError.data?.message ?? 'Git 계정관리 정보를 불러오지 못했습니다.', { variant: 'error' })
    } finally {
      isLoading.value = false
    }
  }

  async function submitAccount() {
    if (isAccountSubmitting.value) {
      return
    }

    if (!accountForm.accountLabel.trim() || !accountForm.authorName.trim()) {
      showToast('계정명과 author 이름을 입력해주세요.', { variant: 'error' })
      return
    }

    isAccountSubmitting.value = true

    const payload: SaveWorkGitAccountPayload = {
      accountLabel: accountForm.accountLabel.trim(),
      provider: accountForm.provider,
      authorName: accountForm.authorName.trim(),
      accessToken: accountForm.accessToken.trim() || undefined,
      useYn: accountForm.useYn
    }

    try {
      if (editingGitAccountUuid.value) {
        await updateGitAccount(editingGitAccountUuid.value, payload)
        showToast('Git 계정을 수정했습니다.', { variant: 'success' })
      } else {
        await createGitAccount(payload)
        showToast('Git 계정을 등록했습니다.', { variant: 'success' })
      }

      isAccountModalVisible.value = false
      resetAccountForm()
      await loadGitResources()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'Git 계정 저장에 실패했습니다.', { variant: 'error' })
    } finally {
      isAccountSubmitting.value = false
    }
  }

  async function submitProject() {
    if (isProjectSubmitting.value) {
      return
    }

    if (!projectForm.gitAccountUuid || !projectForm.projectName.trim() || !projectForm.repositoryUrl.trim()) {
      showToast('계정, 프로젝트명, 저장소 URL을 모두 입력해주세요.', { variant: 'error' })
      return
    }

    isProjectSubmitting.value = true

    const payload: SaveWorkGitProjectPayload = {
      gitAccountUuid: projectForm.gitAccountUuid,
      projectName: projectForm.projectName.trim(),
      repositoryUrl: projectForm.repositoryUrl.trim(),
      useYn: projectForm.useYn
    }

    try {
      if (editingGitProjectUuid.value) {
        await updateGitProject(editingGitProjectUuid.value, payload)
        showToast('Git 프로젝트를 수정했습니다.', { variant: 'success' })
      } else {
        await createGitProject(payload)
        showToast('Git 프로젝트를 등록했습니다.', { variant: 'success' })
      }

      isProjectModalVisible.value = false
      resetProjectForm()
      await loadGitResources()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'Git 프로젝트 저장에 실패했습니다.', { variant: 'error' })
    } finally {
      isProjectSubmitting.value = false
    }
  }

  async function handleProjectConnectionTest(gitProject: WorkGitProject) {
    if (testingGitProjectUuid.value) {
      return
    }

    testingGitProjectUuid.value = gitProject.gitProjectUuid

    try {
      const result = await testGitProjectConnection(gitProject.gitProjectUuid)
      showToast(result.message || '연결 확인이 완료되었습니다.', {
        variant: result.connected ? 'success' : 'error'
      })
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '연결 테스트에 실패했습니다.', { variant: 'error' })
    } finally {
      testingGitProjectUuid.value = ''
    }
  }

  async function handleDeleteGitAccount(gitAccount: WorkGitAccount) {
    const confirmed = await confirm(
      '이 Git 계정을 삭제하면 연결된 프로젝트도 함께 삭제되고, 업무에 연결된 프로젝트 매핑도 제외됩니다.',
      {
        title: 'Git 계정 삭제',
        confirmText: '삭제',
        cancelText: '취소'
      }
    )

    if (!confirmed) {
      return
    }

    try {
      await deleteGitAccount(gitAccount.gitAccountUuid)
      showToast('Git 계정을 삭제했습니다.', { variant: 'success' })
      await loadGitResources()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'Git 계정 삭제에 실패했습니다.', { variant: 'error' })
    }
  }

  async function handleDeleteGitProject(gitProject: WorkGitProject) {
    const confirmed = await confirm(
      '이 Git 프로젝트를 삭제하면 업무에 연결된 프로젝트 매핑도 함께 제외됩니다.',
      {
        title: 'Git 프로젝트 삭제',
        confirmText: '삭제',
        cancelText: '취소'
      }
    )

    if (!confirmed) {
      return
    }

    try {
      await deleteGitProject(gitProject.gitProjectUuid)
      showToast('Git 프로젝트를 삭제했습니다.', { variant: 'success' })
      await loadGitResources()
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? 'Git 프로젝트 삭제에 실패했습니다.', { variant: 'error' })
    }
  }

  onMounted(() => {
    void loadGitResources()
  })

  return {
    accountFilterOptions,
    accountForm,
    editingGitAccountUuid,
    editingGitProjectUuid,
    gitAccounts,
    handleDeleteGitAccount,
    handleDeleteGitProject,
    handleProjectConnectionTest,
    isAccountModalVisible,
    isAccountSubmitting,
    isLoading,
    isProjectModalVisible,
    isProjectSubmitting,
    openCreateAccountModal,
    openCreateProjectModal,
    openEditAccountModal,
    openEditProjectModal,
    projectForm,
    selectedAccountFilterUuid,
    submitAccount,
    submitProject,
    summary,
    testingGitProjectUuid,
    updateAccountFilter,
    updateAccountUseYn,
    updateProjectAccount,
    updateProjectUseYn,
    useYnOptions,
    visibleGitProjects
  }
}
