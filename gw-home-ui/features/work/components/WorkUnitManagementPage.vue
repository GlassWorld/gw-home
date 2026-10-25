<script setup lang="ts">
import SearchableSelect from '~/components/common/SearchableSelect.vue'
import WorkUnitForm from './WorkUnitForm.vue'
import { useWorkUnitManagement } from '../composables/use-work-unit-management'

const {
  availableGitProjects,
  categoryFilterOptions,
  editingWorkUnitUuid,
  filters,
  formState,
  isFormVisible,
  isLoading,
  isSubmitting,
  startCreate,
  startEdit,
  statusFilterOptions,
  statusLabelMap,
  summary,
  updateCategoryFilter,
  updateStatusFilter,
  workUnitList,
  handleSubmit,
  handleUseToggle,
  handleCancelForm,
  applyFilters
} = useWorkUnitManagement()
</script>

<template>
  <main class="page-container page-stack work-page">
    <section class="content-panel page-panel-padding-lg work-page__hero">
      <CommonBasePageHeader title="업무등록 관리" eyebrow="Work"
        description="업무와 연결 프로젝트를 관리하고, 업무별 할 일을 확인하세요. 최근 수정순으로 표시합니다.">
        <p class="work-page__helper-copy">Git 토큰과 계정은 개인 Git 계정관리에서 관리하고, 업무에는 프로젝트만 연결합니다.</p>
        <template #actions>
          <CommonBaseButton variant="secondary" to="/work/todos">업무 세부 작업</CommonBaseButton>
          <CommonBaseButton variant="secondary" to="/work/git-accounts">Git 계정관리</CommonBaseButton>
          <CommonBaseButton @click="startCreate">업무 등록</CommonBaseButton>
        </template>
        <template #summary>
          <div class="work-page__summary">
            <div><span>조회 결과</span><strong>{{ summary.total }}</strong></div>
            <div><span>활성</span><strong>{{ summary.active }}</strong></div>
            <div><span>비활성</span><strong>{{ summary.inactive }}</strong></div>
          </div>
        </template>
      </CommonBasePageHeader>
    </section>

    <section class="work-page__layout">
      <section class="content-panel page-panel-padding-lg work-page__panel">
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

        <CommonBaseState v-if="isLoading" state="loading" message="업무 목록을 불러오는 중입니다." />

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

              <div class="work-page__meta-row">
                <span class="work-page__meta-item">
                  <em>카테고리</em>
                  <strong>{{ workUnit.category || '-' }}</strong>
                </span>
                <span class="work-page__meta-item">
                  <em>최근수정</em>
                  <strong>{{ workUnit.updatedAt.slice(0, 10) }}</strong>
                </span>
              </div>

              <p class="work-page__description">
                {{ workUnit.description || '설명 없음' }}
              </p>

              <div class="work-page__project-list">
                <span class="work-page__project-label">연결 프로젝트</span>
                <div v-if="workUnit.gitProjects.length" class="work-page__project-badges">
                  <span
                    v-for="gitProject in workUnit.gitProjects"
                    :key="gitProject.gitProjectUuid"
                    class="work-page__project-badge"
                  >
                    {{ gitProject.projectName }}
                  </span>
                </div>
                <span v-else class="work-page__project-empty">
                  연결된 프로젝트 없음
                </span>
              </div>
            </div>

            <div class="work-page__item-actions">
              <CommonBaseButton variant="secondary" size="small" :to="`/work/todos/${workUnit.workUnitUuid}`">
                세부 작업
              </CommonBaseButton>
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

        <CommonBaseState v-else state="empty" title="조건에 맞는 업무가 없습니다."
          message="검색 조건을 조정하거나 새 업무를 등록해보세요." action-label="업무 등록" @action="startCreate" />
      </section>
    </section>

    <WorkUnitForm
      v-model="formState"
      :visible="isFormVisible"
      :available-git-projects="availableGitProjects"
      :is-submitting="isSubmitting"
      :is-editing="Boolean(editingWorkUnitUuid)"
      @submit="handleSubmit"
      @close="handleCancelForm"
    />
  </main>
</template>

<style scoped>
.work-page__hero {
  display: grid;
  gap: var(--section-gap);
}

.work-page__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  min-width: min(280px, 100%);
}

.work-page__summary div {
  display: grid;
  gap: 6px;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
}

.work-page__summary span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.work-page__helper-copy {
  margin: 10px 0 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.work-page__summary strong {
  font-size: 1.25rem;
}

.work-page__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--section-gap);
}

.work-page__panel {
  display: grid;
  gap: var(--section-gap);
}

.work-page__panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.work-page__filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px var(--section-gap);
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
  gap: var(--section-gap);
  padding: var(--card-padding) var(--card-padding);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
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
  background: var(--color-primary-soft);
  color: var(--color-accent);
}

.work-page__badge--active {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.work-page__badge--inactive {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.work-page__meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
}

.work-page__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 0.83rem;
  line-height: 1;
}

.work-page__meta-item em {
  color: var(--color-text-muted);
  font-style: normal;
  font-weight: 700;
}

.work-page__meta-item strong {
  color: var(--color-text);
  font-size: 0.84rem;
  font-weight: 700;
}

.work-page__description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
  word-break: break-word;
}

.work-page__project-list {
  display: grid;
  gap: 8px;
}

.work-page__project-label,
.work-page__project-empty {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.work-page__project-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.work-page__project-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  border: 1px solid var(--color-border);
  color: var(--color-text);
  font-size: 0.82rem;
}

.work-page__item-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

@media (max-width: 1080px) {
  .work-page__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {

  .work-page__item {
    padding: var(--mobile-panel-padding);
  }

  .work-page__item,
  .work-page__item-head {
    flex-direction: column;
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
