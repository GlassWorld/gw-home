<script setup lang="ts">
import type {
  SaveWorkGitAccountPayload,
  SaveWorkGitProjectPayload,
  WorkGitAccount,
  WorkGitProject,
  WorkGitProvider
} from '~/types/work'
import SearchableSelect from '~/components/common/SearchableSelect.vue'
import { applySelectableValueFromOptions } from '~/utils/selectable'

definePageMeta({
  middleware: 'auth'
})

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

await loadGitResources()
</script>

<template>
  <main class="page-container work-git-page">
    <section class="content-panel work-git-page__hero">
      <div class="work-git-page__hero-header">
        <div>
          <p class="work-git-page__eyebrow">Git Accounts</p>
          <h1 class="section-title">개인 Git 계정관리</h1>
          <p class="section-description">
            사용자 기준으로 Git 계정을 여러 개 등록하고, 각 계정 아래에 프로젝트를 관리합니다.
          </p>
        </div>

        <div class="work-git-page__hero-actions">
          <CommonBaseButton variant="secondary" to="/work">
            업무관리
          </CommonBaseButton>
          <CommonBaseButton @click="openCreateAccountModal">
            Git 계정 등록
          </CommonBaseButton>
        </div>
      </div>

      <div class="work-git-page__summary">
        <div>
          <span>계정</span>
          <strong>{{ summary.accountCount }}</strong>
        </div>
        <div>
          <span>활성 계정</span>
          <strong>{{ summary.activeAccountCount }}</strong>
        </div>
        <div>
          <span>프로젝트</span>
          <strong>{{ summary.projectCount }}</strong>
        </div>
        <div>
          <span>활성 프로젝트</span>
          <strong>{{ summary.activeProjectCount }}</strong>
        </div>
      </div>
    </section>

    <section class="work-git-page__layout">
      <section class="content-panel work-git-page__panel">
        <div class="work-git-page__panel-header">
          <div>
            <h2 class="section-title">Git 계정</h2>
            <p class="section-description">
              access token 은 암호화 저장되며, 수정 시 새 토큰을 입력하지 않으면 기존 값을 유지합니다.
            </p>
          </div>
          <CommonBaseButton size="small" @click="openCreateAccountModal">
            계정 추가
          </CommonBaseButton>
        </div>

        <p v-if="isLoading" class="message-muted">
          Git 계정 정보를 불러오는 중입니다.
        </p>

        <div v-else-if="gitAccounts.length" class="work-git-page__card-list">
          <article
            v-for="gitAccount in gitAccounts"
            :key="gitAccount.gitAccountUuid"
            class="work-git-page__card"
            :class="{ 'work-git-page__card--inactive': gitAccount.useYn === 'N' }"
          >
            <div class="work-git-page__card-head">
              <strong>{{ gitAccount.accountLabel }}</strong>
              <div class="work-git-page__badge-group">
                <span class="work-git-page__badge work-git-page__badge--provider">{{ gitAccount.provider }}</span>
                <span class="work-git-page__badge" :class="gitAccount.useYn === 'Y' ? 'work-git-page__badge--active' : 'work-git-page__badge--inactive'">
                  {{ gitAccount.useYn === 'Y' ? '사용중' : '사용안함' }}
                </span>
              </div>
            </div>

            <div class="work-git-page__meta">
              <span>Author: {{ gitAccount.authorName }}</span>
              <span>토큰: {{ gitAccount.hasAccessToken ? '등록됨' : '미등록' }}</span>
            </div>

            <div class="work-git-page__card-actions">
              <CommonBaseButton size="small" variant="danger" @click="handleDeleteGitAccount(gitAccount)">
                삭제
              </CommonBaseButton>
              <CommonBaseButton size="small" variant="secondary" @click="openEditAccountModal(gitAccount)">
                수정
              </CommonBaseButton>
            </div>
          </article>
        </div>

        <section v-else class="work-git-page__empty">
          <h3>등록된 Git 계정이 없습니다.</h3>
          <p class="section-description">GitLab 계정을 먼저 등록하세요.</p>
        </section>
      </section>

      <section class="content-panel work-git-page__panel">
        <div class="work-git-page__panel-header">
          <div>
            <h2 class="section-title">Git 프로젝트</h2>
            <p class="section-description">
              업무에는 이 프로젝트들을 연결합니다. 계정별로 필터링해서 관리할 수 있습니다.
            </p>
          </div>
          <div class="work-git-page__panel-actions">
            <CommonBaseButton size="small" :disabled="!gitAccounts.length" @click="openCreateProjectModal">
              프로젝트 추가
            </CommonBaseButton>
          </div>
        </div>

        <div class="work-git-page__filter-row">
          <label class="work-git-page__filter-field">
            <span>계정 필터</span>
            <SearchableSelect
              :options="accountFilterOptions"
              :model-value="selectedAccountFilterUuid"
              placeholder="전체 계정 또는 특정 계정 선택"
              input-class="input-field work-git-page__filter-select"
              @update:modelValue="updateAccountFilter"
            />
          </label>
        </div>

        <p v-if="isLoading" class="message-muted">
          Git 프로젝트 정보를 불러오는 중입니다.
        </p>

        <div v-else-if="visibleGitProjects.length" class="work-git-page__card-list">
          <article
            v-for="gitProject in visibleGitProjects"
            :key="gitProject.gitProjectUuid"
            class="work-git-page__card"
            :class="{ 'work-git-page__card--inactive': gitProject.useYn === 'N' }"
          >
            <div class="work-git-page__card-head">
              <strong>{{ gitProject.projectName }}</strong>
              <div class="work-git-page__badge-group">
                <span class="work-git-page__badge work-git-page__badge--provider">{{ gitProject.provider }}</span>
                <span class="work-git-page__badge" :class="gitProject.useYn === 'Y' ? 'work-git-page__badge--active' : 'work-git-page__badge--inactive'">
                  {{ gitProject.useYn === 'Y' ? '사용중' : '사용안함' }}
                </span>
              </div>
            </div>

            <div class="work-git-page__meta">
              <span>계정: {{ gitProject.gitAccountLabel }}</span>
              <span>{{ gitProject.repositoryUrl }}</span>
            </div>

            <div class="work-git-page__card-actions">
              <CommonBaseButton
                size="small"
                variant="secondary"
                :disabled="testingGitProjectUuid === gitProject.gitProjectUuid"
                @click="handleProjectConnectionTest(gitProject)"
              >
                {{ testingGitProjectUuid === gitProject.gitProjectUuid ? '확인 중' : '연결 테스트' }}
              </CommonBaseButton>
              <CommonBaseButton size="small" variant="danger" @click="handleDeleteGitProject(gitProject)">
                삭제
              </CommonBaseButton>
              <CommonBaseButton size="small" variant="secondary" @click="openEditProjectModal(gitProject)">
                수정
              </CommonBaseButton>
            </div>
          </article>
        </div>

        <section v-else class="work-git-page__empty">
          <h3>표시할 Git 프로젝트가 없습니다.</h3>
          <p class="section-description">계정을 선택해 새 프로젝트를 등록해보세요.</p>
        </section>
      </section>
    </section>

    <CommonBaseModal
      :visible="isAccountModalVisible"
      eyebrow="Git Account"
      :title="editingGitAccountUuid ? 'Git 계정 수정' : 'Git 계정 등록'"
      width="min(760px, 92vw)"
      @close="isAccountModalVisible = false"
    >
      <form class="work-git-page__form" @submit.prevent="submitAccount">
        <label class="work-git-page__field">
          <span>계정명</span>
          <input v-model="accountForm.accountLabel" class="input-field" type="text" maxlength="100" placeholder="예: 회사 GitLab" required>
        </label>

        <label class="work-git-page__field">
          <span>Author 이름</span>
          <input v-model="accountForm.authorName" class="input-field" type="text" maxlength="200" placeholder="커밋 author name" required>
        </label>

        <label class="work-git-page__field">
          <span>사용 여부</span>
          <SearchableSelect
            :options="useYnOptions"
            :model-value="accountForm.useYn"
            placeholder="사용 여부"
            input-class="input-field"
            @update:modelValue="updateAccountUseYn"
          />
        </label>

        <label class="work-git-page__field work-git-page__field--wide">
          <span>Access Token</span>
          <input
            v-model="accountForm.accessToken"
            class="input-field"
            type="password"
            maxlength="2000"
            :placeholder="editingGitAccountUuid ? '변경 시에만 새 토큰 입력' : '비공개 저장소 조회가 필요하면 입력'"
          >
          <small class="work-git-page__field-help">
            토큰 원문은 다시 표시되지 않고, 저장 시 암호화됩니다.
          </small>
        </label>
      </form>

      <template #actions>
        <CommonBaseButton variant="secondary" type="button" :disabled="isAccountSubmitting" @click="isAccountModalVisible = false">
          취소
        </CommonBaseButton>
        <CommonBaseButton type="button" :disabled="isAccountSubmitting" @click="submitAccount">
          {{ editingGitAccountUuid ? '수정 저장' : '계정 등록' }}
        </CommonBaseButton>
      </template>
    </CommonBaseModal>

    <CommonBaseModal
      :visible="isProjectModalVisible"
      eyebrow="Git Project"
      :title="editingGitProjectUuid ? 'Git 프로젝트 수정' : 'Git 프로젝트 등록'"
      width="min(760px, 92vw)"
      @close="isProjectModalVisible = false"
    >
      <form class="work-git-page__form" @submit.prevent="submitProject">
        <label class="work-git-page__field">
          <span>Git 계정</span>
          <SearchableSelect
            :options="gitAccounts.map((gitAccount) => ({ value: gitAccount.gitAccountUuid, label: `${gitAccount.accountLabel} · ${gitAccount.provider}` }))"
            :model-value="projectForm.gitAccountUuid"
            placeholder="계정 선택"
            input-class="input-field"
            @update:modelValue="updateProjectAccount"
          />
        </label>

        <label class="work-git-page__field">
          <span>사용 여부</span>
          <SearchableSelect
            :options="useYnOptions"
            :model-value="projectForm.useYn"
            placeholder="사용 여부"
            input-class="input-field"
            @update:modelValue="updateProjectUseYn"
          />
        </label>

        <label class="work-git-page__field">
          <span>프로젝트명</span>
          <input v-model="projectForm.projectName" class="input-field" type="text" maxlength="200" placeholder="예: gw-home-ui" required>
        </label>

        <label class="work-git-page__field work-git-page__field--wide">
          <span>Repository URL</span>
          <input v-model="projectForm.repositoryUrl" class="input-field" type="url" maxlength="500" placeholder="https://github.com/org/repository" required>
        </label>
      </form>

      <template #actions>
        <CommonBaseButton variant="secondary" type="button" :disabled="isProjectSubmitting" @click="isProjectModalVisible = false">
          취소
        </CommonBaseButton>
        <CommonBaseButton type="button" :disabled="isProjectSubmitting" @click="submitProject">
          {{ editingGitProjectUuid ? '수정 저장' : '프로젝트 등록' }}
        </CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.work-git-page {
  display: grid;
  gap: 24px;
}

.work-git-page__hero,
.work-git-page__panel,
.work-git-page__empty {
  padding: 22px;
}

.work-git-page__hero,
.work-git-page__panel,
.work-git-page__empty,
.work-git-page__card {
  border-radius: var(--radius-large);
}

.work-git-page__hero,
.work-git-page__panel {
  display: grid;
  gap: 18px;
}

.work-git-page__hero-header,
.work-git-page__panel-header,
.work-git-page__card-head,
.work-git-page__card-actions {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.work-git-page__hero-actions,
.work-git-page__panel-actions,
.work-git-page__badge-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

.work-git-page__filter-row,
.work-git-page__filter-field {
  display: grid;
  gap: 8px;
}

.work-git-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.work-git-page__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.work-git-page__summary div {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.work-git-page__summary span,
.work-git-page__field span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
  font-weight: 600;
}

.work-git-page__summary strong {
  font-size: 1.2rem;
}

.work-git-page__layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.work-git-page__card-list {
  display: grid;
  gap: 14px;
}

.work-git-page__card {
  display: grid;
  gap: 12px;
  padding: 16px 18px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  background: rgba(255, 255, 255, 0.04);
}

.work-git-page__card--inactive {
  opacity: 0.8;
}

.work-git-page__meta {
  display: grid;
  gap: 6px;
  color: var(--color-text-muted);
  line-height: 1.5;
  word-break: break-word;
}

.work-git-page__badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 700;
}

.work-git-page__badge--provider {
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
}

.work-git-page__badge--active {
  background: rgba(42, 176, 119, 0.16);
  color: #6fd8a3;
}

.work-git-page__badge--inactive {
  background: rgba(224, 92, 92, 0.16);
  color: #ff8b8b;
}

.work-git-page__filter-select {
  width: 100%;
  min-width: 0;
}

.work-git-page__empty {
  display: grid;
  gap: 12px;
  justify-items: start;
  border: 1px dashed rgba(147, 210, 255, 0.22);
  background: rgba(255, 255, 255, 0.03);
}

.work-git-page__empty h3 {
  margin: 0;
}

.work-git-page__form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.work-git-page__field {
  display: grid;
  gap: 8px;
}

.work-git-page__field--wide {
  grid-column: 1 / -1;
}

.work-git-page__field-help {
  color: var(--color-text-muted);
  line-height: 1.5;
}

@media (max-width: 1080px) {
  .work-git-page__layout,
  .work-git-page__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .work-git-page__hero-header,
  .work-git-page__panel-header,
  .work-git-page__card-head,
  .work-git-page__hero-actions,
  .work-git-page__summary,
  .work-git-page__layout,
  .work-git-page__form {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
