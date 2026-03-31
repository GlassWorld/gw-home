<script setup lang="ts">
import type { SaveWorkUnitPayload, WorkGitProject, WorkUnitStatus } from '~/types/work'
import SearchableSelect from '~/components/common/SearchableSelect.vue'
import { applySelectableValueFromOptions } from '~/utils/selectable'

const props = defineProps<{
  visible: boolean
  modelValue: SaveWorkUnitPayload
  availableGitProjects: WorkGitProject[]
  isSubmitting: boolean
  isEditing: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SaveWorkUnitPayload]
  submit: []
  close: []
}>()

const statusOptions: Array<{ value: WorkUnitStatus; label: string }> = [
  { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'DONE', label: '완료' },
  { value: 'ON_HOLD', label: '보류' }
]

function updateField<K extends keyof SaveWorkUnitPayload>(key: K, value: SaveWorkUnitPayload[K]) {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value
  })
}

function updateStatus(status: string) {
  applySelectableValueFromOptions(status, statusOptions, (validStatus) => {
    updateField('status', validStatus)
  })
}

function toggleGitProject(gitProjectUuid: string, checked: boolean) {
  const currentGitProjectUuids = props.modelValue.gitProjectUuids

  if (checked) {
    updateField('gitProjectUuids', [...new Set([...currentGitProjectUuids, gitProjectUuid])])
    return
  }

  updateField(
    'gitProjectUuids',
    currentGitProjectUuids.filter((currentUuid) => currentUuid !== gitProjectUuid)
  )
}

function handleSubmit() {
  emit('submit')
}
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    eyebrow="Work"
    :title="isEditing ? '업무 수정' : '업무 등록'"
    width="min(980px, 92vw)"
    @close="emit('close')"
  >
    <p class="work-unit-form__description">
      업무명, 카테고리, 상태를 입력하고 필요하면 개인 Git 계정관리에서 등록한 프로젝트를 연결할 수 있습니다.
    </p>

    <form class="work-unit-form" @submit.prevent="handleSubmit">
      <label class="work-unit-form__field work-unit-form__field--wide">
        <span>업무명</span>
        <input
          class="input-field"
          type="text"
          maxlength="200"
          required
          :value="modelValue.title"
          placeholder="예: 일일보고 기능 설계"
          @input="updateField('title', ($event.target as HTMLInputElement).value)"
        >
      </label>

      <label class="work-unit-form__field">
        <span>카테고리</span>
        <input
          class="input-field"
          type="text"
          maxlength="100"
          :value="modelValue.category ?? ''"
          placeholder="예: 개발, 운영, 기획"
          @input="updateField('category', ($event.target as HTMLInputElement).value)"
        >
      </label>

      <label class="work-unit-form__field">
        <span>상태</span>
        <SearchableSelect
          :options="statusOptions"
          :model-value="modelValue.status"
          placeholder="상태 선택"
          input-class="input-field"
          @update:modelValue="updateStatus"
        />
      </label>

      <label class="work-unit-form__field work-unit-form__field--wide">
        <span>설명</span>
        <textarea
          class="work-unit-form__textarea"
          rows="4"
          maxlength="1000"
          :value="modelValue.description ?? ''"
          placeholder="업무 설명이나 보고 작성 시 참고할 내용을 입력합니다."
          @input="updateField('description', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>

      <section class="work-unit-form__field work-unit-form__field--wide work-unit-form__git">
        <div class="work-unit-form__git-header">
          <div>
            <span>연결 프로젝트</span>
            <p class="work-unit-form__git-description">
              프로젝트를 연결하면 일일보고 작성 시 해당 프로젝트들의 커밋을 불러올 수 있습니다.
            </p>
          </div>
          <CommonBaseButton variant="secondary" size="small" type="button" to="/work/git-accounts">
            Git 계정관리
          </CommonBaseButton>
        </div>

        <div v-if="availableGitProjects.length" class="work-unit-form__git-list">
          <label
            v-for="gitProject in availableGitProjects"
            :key="gitProject.gitProjectUuid"
            class="work-unit-form__git-card work-unit-form__git-card--selectable"
          >
            <div class="work-unit-form__git-card-header">
              <div class="work-unit-form__git-card-heading">
                <input
                  type="checkbox"
                  :checked="modelValue.gitProjectUuids.includes(gitProject.gitProjectUuid)"
                  @change="toggleGitProject(gitProject.gitProjectUuid, ($event.target as HTMLInputElement).checked)"
                >
                <strong>{{ gitProject.projectName }}</strong>
              </div>
              <span class="work-unit-form__git-provider">{{ gitProject.provider }}</span>
            </div>

            <div class="work-unit-form__git-meta">
              <span>{{ gitProject.gitAccountLabel }}</span>
              <span>{{ gitProject.repositoryUrl }}</span>
            </div>
          </label>
        </div>

        <p v-else class="work-unit-form__git-empty">
          등록된 Git 프로젝트가 없습니다. Git 계정관리에서 계정과 프로젝트를 먼저 등록하면 여기서 연결할 수 있습니다.
        </p>
      </section>
    </form>

    <template #actions>
      <CommonBaseButton
        variant="secondary"
        type="button"
        :disabled="isSubmitting"
        @click="emit('close')"
      >
        취소
      </CommonBaseButton>
      <CommonBaseButton type="button" :disabled="isSubmitting" @click="handleSubmit">
        {{ isEditing ? '업무 수정' : '업무 등록' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.work-unit-form__description {
  margin: 0;
}

.work-unit-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.work-unit-form__field {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.work-unit-form__field--wide {
  grid-column: 1 / -1;
}

.work-unit-form__field span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.work-unit-form__textarea {
  width: 100%;
  min-height: 112px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-small);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text);
  resize: vertical;
  font: inherit;
}

.work-unit-form__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.22);
  border-color: var(--color-primary);
}

.work-unit-form__git {
  gap: 14px;
}

.work-unit-form__git-header,
.work-unit-form__git-card-header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.work-unit-form__git-description,
.work-unit-form__git-empty {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.work-unit-form__git-list {
  display: grid;
  gap: 14px;
}

.work-unit-form__git-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
}

.work-unit-form__git-card--selectable {
  cursor: pointer;
}

.work-unit-form__git-card-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.work-unit-form__git-meta {
  display: grid;
  gap: 6px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.work-unit-form__git-provider {
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
}

@media (max-width: 768px) {
  .work-unit-form {
    grid-template-columns: 1fr;
  }

  .work-unit-form__git-header,
  .work-unit-form__git-card-header {
    display: grid;
  }
}
</style>
