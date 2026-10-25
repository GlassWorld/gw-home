<script setup lang="ts">
import { useId } from 'vue'
import type { SaveWorkTodoPayload, WorkUnitOption } from '../types/work.types'

const props = defineProps<{
  visible: boolean
  modelValue: SaveWorkTodoPayload
  isSubmitting: boolean
  isEditing: boolean
  parentTitle: string
  workUnitOptions?: WorkUnitOption[]
  workUnitUuid?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SaveWorkTodoPayload]
  submit: []
  close: []
  'update:workUnitUuid': [value: string]
}>()

const formId = `todo-form-${useId()}`

function updateField<K extends keyof SaveWorkTodoPayload>(key: K, value: SaveWorkTodoPayload[K]) {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value
  })
}
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    eyebrow="Work Todo"
    :title="isEditing ? '할 일 수정' : '할 일 추가'"
    :dismissible="!isSubmitting"
    width="min(760px, 92vw)"
    @close="emit('close')"
  >
    <form :id="formId" @submit.prevent="emit('submit')">
      <fieldset class="work-todo-form" :disabled="isSubmitting">
      <label v-if="workUnitOptions" class="work-todo-form__field work-todo-form__field--wide">
        <span>소속 업무</span>
        <select class="input-field" required :value="workUnitUuid" :disabled="isEditing || !!modelValue.parentTodoUuid"
          @change="emit('update:workUnitUuid', ($event.target as HTMLSelectElement).value)">
          <option value="" disabled>업무를 선택하세요</option>
          <option v-for="unit in workUnitOptions" :key="unit.workUnitUuid" :value="unit.workUnitUuid">
            {{ unit.title }}{{ unit.useYn === 'N' ? ' (비활성)' : '' }}
          </option>
        </select>
      </label>
      <p v-if="parentTitle" class="work-todo-form__parent">
        상위 TODO: <strong>{{ parentTitle }}</strong>
      </p>

      <label class="work-todo-form__field work-todo-form__field--wide">
        <span>제목</span>
        <input
          class="input-field"
          type="text"
          maxlength="200"
          required
          :value="modelValue.title"
          placeholder="예: API 응답 구조 설계"
          @input="updateField('title', ($event.target as HTMLInputElement).value)"
        >
      </label>

      <label class="work-todo-form__field work-todo-form__field--wide">
        <span>설명</span>
        <textarea
          class="work-todo-form__textarea"
          rows="4"
          :value="modelValue.description ?? ''"
          placeholder="작업 메모나 세부 설명을 입력합니다."
          @input="updateField('description', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>

      <label class="work-todo-form__field">
        <span>시작일</span>
        <input
          class="input-field"
          type="date"
          :value="modelValue.startDate ?? ''"
          @input="updateField('startDate', ($event.target as HTMLInputElement).value || null)"
        >
      </label>

      <label class="work-todo-form__field">
        <span>마감일</span>
        <input
          class="input-field"
          type="date"
          :value="modelValue.dueDate ?? ''"
          :min="modelValue.startDate || undefined"
          @input="updateField('dueDate', ($event.target as HTMLInputElement).value || null)"
        >
      </label>

      <label class="work-todo-form__field">
        <span>정렬 순서</span>
        <input
          class="input-field"
          type="number"
          min="1"
          max="9999"
          :value="modelValue.sortOrder ?? ''"
          placeholder="비워두면 마지막 순서"
          @input="updateField('sortOrder', Number(($event.target as HTMLInputElement).value) || null)"
        >
      </label>
      </fieldset>
    </form>

    <template #actions>
      <CommonBaseButton variant="secondary" type="button" :disabled="isSubmitting" @click="emit('close')">
        취소
      </CommonBaseButton>
      <CommonBaseButton type="submit" :form="formId" :disabled="isSubmitting">
        {{ isSubmitting ? '저장 중…' : isEditing ? '수정' : '추가' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.work-todo-form {
  border: 0;
  padding: 0;
  margin: 0;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--section-gap);
}

.work-todo-form__parent {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--color-text-muted);
}

.work-todo-form__field {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.work-todo-form__field--wide {
  grid-column: 1 / -1;
}

.work-todo-form__field span {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.work-todo-form__textarea {
  width: 100%;
  min-height: 120px;
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-small);
  background: var(--color-surface);
  color: var(--color-text);
  resize: vertical;
  font: inherit;
}

@media (max-width: 768px) {
  .work-todo-form {
    grid-template-columns: 1fr;
  }
}
</style>
