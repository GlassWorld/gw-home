import type { ApiResponse } from '~/types/api/common'
import type {
  SaveWorkUnitPayload,
  WorkGitProject,
  WorkUnit,
  WorkUnitGitCommit,
  WorkUnitListParams,
  WorkUnitOption,
  WorkUnitStatus
} from '~/types/work'

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

interface WorkUnitApi {
  workUnitUuid?: string
  work_unit_uuid?: string
  title?: string
  description?: string | null
  category?: string | null
  status?: string
  useYn?: 'Y' | 'N'
  use_yn?: 'Y' | 'N'
  useCount?: number
  use_count?: number
  lastUsedAt?: string | null
  last_used_at?: string | null
  createdAt?: string
  created_at?: string
  updatedAt?: string
  updated_at?: string
  gitProjects?: WorkGitProjectApi[]
  git_projects?: WorkGitProjectApi[]
}

interface WorkUnitOptionApi {
  workUnitUuid?: string
  work_unit_uuid?: string
  title?: string
  category?: string | null
  status?: string
  useYn?: 'Y' | 'N'
  use_yn?: 'Y' | 'N'
}

interface WorkUnitGitCommitApi {
  gitConnectionUuid?: string
  git_connection_uuid?: string
  provider?: string
  repositoryUrl?: string
  repository_url?: string
  repositoryName?: string
  repository_name?: string
  commitSha?: string
  commit_sha?: string
  message?: string
  authorName?: string
  author_name?: string
  authoredAt?: string
  authored_at?: string
  commitUrl?: string
  commit_url?: string
}

export function useWorkUnitApi() {
  const { authorizedFetch } = useAuth()

  function normalizeStatus(status: string | undefined): WorkUnitStatus {
    if (status === 'DONE' || status === 'ON_HOLD') {
      return status
    }
    return 'IN_PROGRESS'
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

  function toWorkUnit(workUnit: WorkUnitApi): WorkUnit {
    return {
      workUnitUuid: workUnit.workUnitUuid ?? workUnit.work_unit_uuid ?? '',
      title: workUnit.title ?? '',
      description: workUnit.description ?? null,
      category: workUnit.category ?? null,
      status: normalizeStatus(workUnit.status),
      useYn: workUnit.useYn ?? workUnit.use_yn ?? 'Y',
      useCount: Number(workUnit.useCount ?? workUnit.use_count ?? 0),
      lastUsedAt: workUnit.lastUsedAt ?? workUnit.last_used_at ?? null,
      createdAt: workUnit.createdAt ?? workUnit.created_at ?? '',
      updatedAt: workUnit.updatedAt ?? workUnit.updated_at ?? '',
      gitProjects: (workUnit.gitProjects ?? workUnit.git_projects ?? []).map(toGitProject)
    }
  }

  function toOption(workUnit: WorkUnitOptionApi): WorkUnitOption {
    return {
      workUnitUuid: workUnit.workUnitUuid ?? workUnit.work_unit_uuid ?? '',
      title: workUnit.title ?? '',
      category: workUnit.category ?? null,
      status: normalizeStatus(workUnit.status),
      useYn: workUnit.useYn ?? workUnit.use_yn ?? 'Y'
    }
  }

  function toRequestBody(payload: SaveWorkUnitPayload): Record<string, string | string[]> {
    return {
      title: payload.title,
      category: payload.category ?? '',
      description: payload.description ?? '',
      status: payload.status,
      git_project_uuids: payload.gitProjectUuids
    }
  }

  function toGitCommit(commit: WorkUnitGitCommitApi): WorkUnitGitCommit {
    return {
      gitConnectionUuid: commit.gitConnectionUuid ?? commit.git_connection_uuid ?? '',
      provider: 'GITLAB',
      repositoryUrl: commit.repositoryUrl ?? commit.repository_url ?? '',
      repositoryName: commit.repositoryName ?? commit.repository_name ?? '',
      commitSha: commit.commitSha ?? commit.commit_sha ?? '',
      message: commit.message ?? '',
      authorName: commit.authorName ?? commit.author_name ?? '',
      authoredAt: commit.authoredAt ?? commit.authored_at ?? '',
      commitUrl: commit.commitUrl ?? commit.commit_url ?? ''
    }
  }

  async function fetchWorkUnitList(params: WorkUnitListParams = {}): Promise<WorkUnit[]> {
    const response = await authorizedFetch<ApiResponse<WorkUnitApi[]>>('/api/v1/work-units', {
      method: 'GET',
      query: {
        keyword: params.keyword,
        category: params.category,
        status: params.status || undefined,
        useYn: params.useYn || undefined,
        sort: params.sort
      }
    })

    return response.data.map(toWorkUnit)
  }

  async function fetchWorkUnitOptions(includeInactive = false): Promise<WorkUnitOption[]> {
    const response = await authorizedFetch<ApiResponse<WorkUnitOptionApi[]>>('/api/v1/work-units/options', {
      method: 'GET',
      query: {
        includeInactive: includeInactive ? 'true' : undefined
      }
    })

    return response.data.map(toOption)
  }

  async function fetchWorkUnit(workUnitUuid: string): Promise<WorkUnit> {
    const response = await authorizedFetch<ApiResponse<WorkUnitApi>>(`/api/v1/work-units/${workUnitUuid}`, {
      method: 'GET'
    })

    return toWorkUnit(response.data)
  }

  async function createWorkUnit(payload: SaveWorkUnitPayload): Promise<WorkUnit> {
    const response = await authorizedFetch<ApiResponse<WorkUnitApi>>('/api/v1/work-units', {
      method: 'POST',
      body: toRequestBody(payload)
    })

    return toWorkUnit(response.data)
  }

  async function updateWorkUnit(workUnitUuid: string, payload: SaveWorkUnitPayload): Promise<WorkUnit> {
    const response = await authorizedFetch<ApiResponse<WorkUnitApi>>(`/api/v1/work-units/${workUnitUuid}`, {
      method: 'PUT',
      body: toRequestBody(payload)
    })

    return toWorkUnit(response.data)
  }

  async function updateWorkUnitUse(workUnitUuid: string, useYn: 'Y' | 'N'): Promise<WorkUnit> {
    const response = await authorizedFetch<ApiResponse<WorkUnitApi>>(`/api/v1/work-units/${workUnitUuid}/use`, {
      method: 'PUT',
      body: { use_yn: useYn }
    })

    return toWorkUnit(response.data)
  }

  async function fetchWorkUnitGitCommits(workUnitUuid: string, reportDate: string): Promise<WorkUnitGitCommit[]> {
    const response = await authorizedFetch<ApiResponse<WorkUnitGitCommitApi[]>>(`/api/v1/work-units/${workUnitUuid}/git-commits`, {
      method: 'GET',
      query: { reportDate }
    })

    return response.data.map(toGitCommit)
  }

  return {
    fetchWorkUnitList,
    fetchWorkUnitOptions,
    fetchWorkUnit,
    createWorkUnit,
    updateWorkUnit,
    updateWorkUnitUse,
    fetchWorkUnitGitCommits
  }
}
