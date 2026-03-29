<script setup lang="ts">
import type { SaveWorkUnitPayload, WorkUnit, WorkUnitOption, WorkUnitSort, WorkUnitStatus } from '~/types/work'
import SearchableSelect from '~/components/common/SearchableSelect.vue'
import { applySelectableValueFromOptions } from '~/utils/selectable'

definePageMeta({
  middleware: 'auth'
})

const { fetchWorkUnitList, fetchWorkUnitOptions, createWorkUnit, updateWorkUnit, updateWorkUnitUse } = useWorkUnitApi()
const { showToast } = useToast()
const { confirm } = useDialog()

const workUnitList = ref<WorkUnit[]>([])
const workUnitOptions = ref<WorkUnitOption[]>([])
const editingWorkUnitUuid = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)
const isFormVisible = ref(false)

const filters = reactive<{
  keyword: string
  category: string
  status: WorkUnitStatus | ''
  includeInactive: boolean
  sort: WorkUnitSort
}>({
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
  status: 'IN_PROGRESS'
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
    status: 'IN_PROGRESS'
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
    status: workUnit.status
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

async function reloadAll() {
  await Promise.all([loadWorkUnitList(), loadWorkUnitOptions()])
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
    status: formState.value.status
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

await reloadAll()
</script>

<template>
  <main class="page-container work-page">
    <section class="content-panel work-page__hero">
      <div class="work-page__hero-header">
        <div>
          <p class="work-page__eyebrow">Work</p>
          <h1 class="section-title">업무등록 관리</h1>
          <p class="section-description">
            일일보고에서 반복 사용하는 업무를 계정별로 미리 등록하고, 상태와 사용여부를 안정적으로 관리합니다.
          </p>
        </div>

        <div class="work-page__hero-side">
          <CommonBaseButton @click="startCreate">
            업무 등록
          </CommonBaseButton>

          <div class="work-page__summary">
            <div>
              <span>조회 결과</span>
              <strong>{{ summary.total }}</strong>
            </div>
            <div>
              <span>활성</span>
              <strong>{{ summary.active }}</strong>
            </div>
            <div>
              <span>비활성</span>
              <strong>{{ summary.inactive }}</strong>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="work-page__layout">
      <section class="content-panel work-page__panel">
        <div class="work-page__panel-header">
          <div>
            <h2 class="section-title">업무 목록</h2>
            <p class="section-description">검색과 필터를 조합해 필요한 업무만 빠르게 찾을 수 있습니다.</p>
          </div>
        </div>

        <form class="work-page__filters" @submit.prevent="applyFilters">
          <label class="work-page__filter">
            <span>업무명 검색</span>
            <input v-model="filters.keyword" class="input-field" type="search" placeholder="업무명 검색">
          </label>

          <label class="work-page__filter">
            <span>카테고리</span>
            <SearchableSelect
              :options="categoryFilterOptions"
              :model-value="filters.category"
              placeholder="카테고리"
              input-class="input-field work-page__select"
              @update:modelValue="updateCategoryFilter"
            />
          </label>

          <label class="work-page__filter">
            <span>상태</span>
            <SearchableSelect
              :options="statusFilterOptions"
              :model-value="filters.status"
              placeholder="상태"
              input-class="input-field work-page__select"
              @update:modelValue="updateStatusFilter"
            />
          </label>

          <label class="work-page__filter">
            <span>정렬</span>
            <SearchableSelect
              :options="sortOptions"
              :model-value="filters.sort"
              placeholder="정렬"
              input-class="input-field work-page__select"
              @update:modelValue="updateSortFilter"
            />
          </label>

          <label class="work-page__toggle">
            <input v-model="filters.includeInactive" type="checkbox">
            <span>비활성 업무 포함</span>
          </label>

          <div class="work-page__filter-actions">
            <CommonBaseButton variant="secondary" type="submit">
              검색
            </CommonBaseButton>
          </div>
        </form>

        <p v-if="isLoading" class="message-muted">
          업무 목록을 불러오는 중입니다.
        </p>

        <div v-else-if="workUnitList.length" class="work-page__list">
          <article
            v-for="workUnit in workUnitList"
            :key="workUnit.workUnitUuid"
            class="work-page__item"
            :class="{ 'work-page__item--inactive': workUnit.useYn === 'N' }"
          >
            <div class="work-page__item-main">
              <div class="work-page__item-head">
                <strong>{{ workUnit.title }}</strong>
                <div class="work-page__badges">
                  <span class="work-page__badge work-page__badge--status">
                    {{ statusLabelMap[workUnit.status] }}
                  </span>
                  <span
                    class="work-page__badge"
                    :class="workUnit.useYn === 'Y' ? 'work-page__badge--active' : 'work-page__badge--inactive'"
                  >
                    {{ workUnit.useYn === 'Y' ? '사용중' : '사용안함' }}
                  </span>
                </div>
              </div>

              <div class="meta-row">
                <span>카테고리 {{ workUnit.category || '-' }}</span>
                <span>사용횟수 {{ workUnit.useCount }}</span>
                <span>최근수정 {{ workUnit.updatedAt.slice(0, 10) }}</span>
              </div>

              <p class="work-page__description">
                {{ workUnit.description || '설명 없음' }}
              </p>
            </div>

            <div class="work-page__item-actions">
              <CommonBaseButton variant="secondary" size="small" @click="startEdit(workUnit)">
                수정
              </CommonBaseButton>
              <CommonBaseButton
                :variant="workUnit.useYn === 'Y' ? 'danger' : 'secondary'"
                size="small"
                @click="handleUseToggle(workUnit)"
              >
                {{ workUnit.useYn === 'Y' ? '사용안함' : '재사용' }}
              </CommonBaseButton>
            </div>
          </article>
        </div>

        <section v-else class="work-page__empty">
          <h3>조건에 맞는 업무가 없습니다.</h3>
          <p class="section-description">검색 조건을 조정하거나 새 업무를 등록해보세요.</p>
          <CommonBaseButton @click="startCreate">
            업무 등록
          </CommonBaseButton>
        </section>
      </section>
    </section>

    <WorkUnitForm
      v-model="formState"
      :visible="isFormVisible"
      :is-submitting="isSubmitting"
      :is-editing="Boolean(editingWorkUnitUuid)"
      @submit="handleSubmit"
      @close="handleCancelForm"
    />
  </main>
</template>

<style scoped>
.work-page {
  display: grid;
  gap: 24px;
}

.work-page__hero,
.work-page__panel,
.work-page__empty {
  padding: 22px;
}

.work-page__hero {
  display: grid;
  gap: 16px;
}

.work-page__hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.work-page__hero-side {
  display: grid;
  justify-items: end;
  gap: 14px;
}

.work-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.work-page__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 10px;
  min-width: 280px;
}

.work-page__summary div {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.work-page__summary span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.work-page__summary strong {
  font-size: 1.25rem;
}

.work-page__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
}

.work-page__panel {
  display: grid;
  gap: 18px;
}

.work-page__panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.work-page__filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px 16px;
  align-items: end;
}

.work-page__filter {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.work-page__filter span,
.work-page__toggle span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.work-page__select {
  padding: 0 16px;
}

.work-page__toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
}

.work-page__filter-actions {
  display: flex;
  justify-content: flex-end;
}

.work-page__list {
  display: grid;
  gap: 14px;
}

.work-page__item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.work-page__item--inactive {
  opacity: 0.82;
}

.work-page__item-main {
  display: grid;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.work-page__item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.work-page__item-head strong {
  font-size: 1rem;
}

.work-page__badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.work-page__badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
}

.work-page__badge--status {
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
}

.work-page__badge--active {
  background: rgba(42, 176, 119, 0.16);
  color: #6fd8a3;
}

.work-page__badge--inactive {
  background: rgba(224, 92, 92, 0.16);
  color: #ff8b8b;
}

.work-page__description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
  word-break: break-word;
}

.work-page__item-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.work-page__empty {
  display: grid;
  gap: 12px;
  justify-items: start;
  border: 1px dashed rgba(147, 210, 255, 0.22);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
}

.work-page__empty h3 {
  margin: 0;
}

@media (max-width: 1080px) {
  .work-page__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .work-page__hero-header,
  .work-page__item,
  .work-page__item-head {
    flex-direction: column;
  }

  .work-page__hero-side {
    justify-items: stretch;
  }

  .work-page__summary {
    width: 100%;
    min-width: 0;
  }

  .work-page__filters {
    grid-template-columns: 1fr;
  }

  .work-page__filter-actions {
    justify-content: stretch;
  }

  .work-page__filter-actions :deep(.base-button) {
    width: 100%;
  }

  .work-page__item-actions {
    flex-wrap: wrap;
  }
}
</style>
