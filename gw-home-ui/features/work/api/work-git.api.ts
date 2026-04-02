import type { ApiResponse } from '~/types/api/common'
import type {
  SaveWorkGitAccountPayload,
  SaveWorkGitProjectPayload,
  WorkGitAccount,
  WorkGitConnectionTestResult,
  WorkGitProject
} from '../types/work.types'

interface WorkGitAccountApi {
  gitAccountUuid?: string
  git_account_uuid?: string
  provider?: string
  accountLabel?: string
  account_label?: string
  authorName?: string
  author_name?: string
  hasAccessToken?: boolean
  has_access_token?: boolean
  useYn?: 'Y' | 'N'
  use_yn?: 'Y' | 'N'
  createdAt?: string
  created_at?: string
  updatedAt?: string
  updated_at?: string
}

interface WorkGitProjectApi {
  gitProjectUuid?: string
  git_project_uuid?: string
  gitAccountUuid?: string
  git_account_uuid?: string
  gitAccountLabel?: string
  git_account_label?: string
  provider?: string
  projectName?: string
  project_name?: string
  repositoryUrl?: string
  repository_url?: string
  useYn?: 'Y' | 'N'
  use_yn?: 'Y' | 'N'
  createdAt?: string
  created_at?: string
  updatedAt?: string
  updated_at?: string
}

interface WorkGitConnectionTestApi {
  gitProjectUuid?: string
  git_project_uuid?: string
  provider?: string
  projectName?: string
  project_name?: string
  repositoryUrl?: string
  repository_url?: string
  connected?: boolean
  message?: string
  checkedAt?: string
  checked_at?: string
}

function toGitAccount(gitAccount: WorkGitAccountApi): WorkGitAccount {
  return {
    gitAccountUuid: gitAccount.gitAccountUuid ?? gitAccount.git_account_uuid ?? '',
    provider: 'GITLAB',
    accountLabel: gitAccount.accountLabel ?? gitAccount.account_label ?? '',
    authorName: gitAccount.authorName ?? gitAccount.author_name ?? '',
    hasAccessToken: Boolean(gitAccount.hasAccessToken ?? gitAccount.has_access_token ?? false),
    useYn: gitAccount.useYn ?? gitAccount.use_yn ?? 'Y',
    createdAt: gitAccount.createdAt ?? gitAccount.created_at ?? '',
    updatedAt: gitAccount.updatedAt ?? gitAccount.updated_at ?? ''
  }
}

function toGitProject(gitProject: WorkGitProjectApi): WorkGitProject {
  return {
    gitProjectUuid: gitProject.gitProjectUuid ?? gitProject.git_project_uuid ?? '',
    gitAccountUuid: gitProject.gitAccountUuid ?? gitProject.git_account_uuid ?? '',
    gitAccountLabel: gitProject.gitAccountLabel ?? gitProject.git_account_label ?? '',
    provider: 'GITLAB',
    projectName: gitProject.projectName ?? gitProject.project_name ?? '',
    repositoryUrl: gitProject.repositoryUrl ?? gitProject.repository_url ?? '',
    useYn: gitProject.useYn ?? gitProject.use_yn ?? 'Y',
    createdAt: gitProject.createdAt ?? gitProject.created_at ?? '',
    updatedAt: gitProject.updatedAt ?? gitProject.updated_at ?? ''
  }
}

function toConnectionTestResult(result: WorkGitConnectionTestApi): WorkGitConnectionTestResult {
  return {
    gitProjectUuid: result.gitProjectUuid ?? result.git_project_uuid ?? '',
    provider: 'GITLAB',
    projectName: result.projectName ?? result.project_name ?? '',
    repositoryUrl: result.repositoryUrl ?? result.repository_url ?? '',
    connected: Boolean(result.connected),
    message: result.message ?? '',
    checkedAt: result.checkedAt ?? result.checked_at ?? ''
  }
}

export function useWorkGitApi() {
  const { authorizedFetch } = useAuth()

  async function fetchGitAccounts(): Promise<WorkGitAccount[]> {
    const response = await authorizedFetch<ApiResponse<WorkGitAccountApi[]>>('/api/v1/work-git/accounts', { method: 'GET' })
    return response.data.map(toGitAccount)
  }

  async function createGitAccount(payload: SaveWorkGitAccountPayload): Promise<WorkGitAccount> {
    const response = await authorizedFetch<ApiResponse<WorkGitAccountApi>>('/api/v1/work-git/accounts', {
      method: 'POST',
      body: {
        account_label: payload.accountLabel,
        provider: payload.provider,
        author_name: payload.authorName,
        access_token: payload.accessToken ?? '',
        use_yn: payload.useYn ?? 'Y'
      }
    })

    return toGitAccount(response.data)
  }

  async function updateGitAccount(gitAccountUuid: string, payload: SaveWorkGitAccountPayload): Promise<WorkGitAccount> {
    const response = await authorizedFetch<ApiResponse<WorkGitAccountApi>>(`/api/v1/work-git/accounts/${gitAccountUuid}`, {
      method: 'PUT',
      body: {
        account_label: payload.accountLabel,
        provider: payload.provider,
        author_name: payload.authorName,
        access_token: payload.accessToken ?? '',
        use_yn: payload.useYn ?? 'Y'
      }
    })

    return toGitAccount(response.data)
  }

  async function deleteGitAccount(gitAccountUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/work-git/accounts/${gitAccountUuid}`, {
      method: 'DELETE'
    })
  }

  async function fetchGitProjects(gitAccountUuid?: string): Promise<WorkGitProject[]> {
    const response = await authorizedFetch<ApiResponse<WorkGitProjectApi[]>>('/api/v1/work-git/projects', {
      method: 'GET',
      query: { gitAccountUuid }
    })

    return response.data.map(toGitProject)
  }

  async function fetchGitProjectOptions(): Promise<WorkGitProject[]> {
    const response = await authorizedFetch<ApiResponse<WorkGitProjectApi[]>>('/api/v1/work-git/projects/options', { method: 'GET' })
    return response.data.map(toGitProject)
  }

  async function createGitProject(payload: SaveWorkGitProjectPayload): Promise<WorkGitProject> {
    const response = await authorizedFetch<ApiResponse<WorkGitProjectApi>>('/api/v1/work-git/projects', {
      method: 'POST',
      body: {
        git_account_uuid: payload.gitAccountUuid,
        project_name: payload.projectName,
        repository_url: payload.repositoryUrl,
        use_yn: payload.useYn ?? 'Y'
      }
    })

    return toGitProject(response.data)
  }

  async function updateGitProject(gitProjectUuid: string, payload: SaveWorkGitProjectPayload): Promise<WorkGitProject> {
    const response = await authorizedFetch<ApiResponse<WorkGitProjectApi>>(`/api/v1/work-git/projects/${gitProjectUuid}`, {
      method: 'PUT',
      body: {
        git_account_uuid: payload.gitAccountUuid,
        project_name: payload.projectName,
        repository_url: payload.repositoryUrl,
        use_yn: payload.useYn ?? 'Y'
      }
    })

    return toGitProject(response.data)
  }

  async function deleteGitProject(gitProjectUuid: string): Promise<void> {
    await authorizedFetch<ApiResponse<null>>(`/api/v1/work-git/projects/${gitProjectUuid}`, {
      method: 'DELETE'
    })
  }

  async function testGitProjectConnection(gitProjectUuid: string): Promise<WorkGitConnectionTestResult> {
    const response = await authorizedFetch<ApiResponse<WorkGitConnectionTestApi>>(`/api/v1/work-git/projects/${gitProjectUuid}/connection-test`, {
      method: 'POST'
    })

    return toConnectionTestResult(response.data)
  }

  return {
    fetchGitAccounts,
    createGitAccount,
    updateGitAccount,
    deleteGitAccount,
    fetchGitProjects,
    fetchGitProjectOptions,
    createGitProject,
    updateGitProject,
    deleteGitProject,
    testGitProjectConnection
  }
}
