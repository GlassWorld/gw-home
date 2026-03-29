<script setup lang="ts">
import type { SaveWorkUnitPayload, WorkUnitStatus } from '~/types/work'
import SearchableSelect from '~/components/common/SearchableSelect.vue'
import { applySelectableValueFromOptions } from '~/utils/selectable'

const props = defineProps<{
  visible: boolean
  modelValue: SaveWorkUnitPayload
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

function handleSubmit() {
  emit('submit')
}
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    eyebrow="Work"
    :title="isEditing ? '업무 수정' : '업무 등록'"
    width="760px"
    @close="emit('close')"
  >
    <p class="work-unit-form__description">
      업무명, 카테고리, 상태를 입력해 일일보고에서 반복 사용할 업무를 등록합니다.
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
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: var(--radius-small);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text);
}

.work-unit-form__textarea {
  min-height: 112px;
  padding: 14px 16px;
  resize: vertical;
  font: inherit;
}

.work-unit-form__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.22);
  border-color: var(--color-primary);
}
@media (max-width: 768px) {
  .work-unit-form {
    grid-template-columns: 1fr;
  }
}
</style>
