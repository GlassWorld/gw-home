import type { ApiResponse } from '~/types/api/common'
import type {
  SaveWorkUnitPayload,
  WorkUnit,
  WorkUnitListParams,
  WorkUnitOption,
  WorkUnitStatus
} from '~/types/work'

interface WorkUnitApi extends Partial<WorkUnit> {
  work_unit_uuid?: string
  use_yn?: 'Y' | 'N'
  use_count?: number
  last_used_at?: string | null
  created_at?: string
  updated_at?: string
}

interface WorkUnitOptionApi extends Partial<WorkUnitOption> {
  work_unit_uuid?: string
  use_yn?: 'Y' | 'N'
}

export function useWorkUnitApi() {
  const { authorizedFetch } = useAuth()

  function normalizeStatus(status: string | undefined): WorkUnitStatus {
    if (status === 'DONE' || status === 'ON_HOLD') {
      return status
    }

    return 'IN_PROGRESS'
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
      updatedAt: workUnit.updatedAt ?? workUnit.updated_at ?? ''
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

  function toRequestBody(payload: SaveWorkUnitPayload): Record<string, string> {
    return {
      title: payload.title,
      category: payload.category ?? '',
      description: payload.description ?? '',
      status: payload.status
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
      body: { useYn }
    })

    return toWorkUnit(response.data)
  }

  return {
    fetchWorkUnitList,
    fetchWorkUnitOptions,
    createWorkUnit,
    updateWorkUnit,
    updateWorkUnitUse
  }
}
