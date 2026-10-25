<script setup lang="ts">
import { useId } from 'vue'
import type { LifeCategory, LifeTodoForm } from '../types/life-todo.types'

const props = defineProps<{ visible: boolean; modelValue: LifeTodoForm; categories: LifeCategory[]; isEditing: boolean; isSaving: boolean; errorMessage: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: LifeTodoForm]; close: []; submit: [] }>()
const formId = `life-todo-form-${useId()}`
const missingCategory = computed(() => props.modelValue.categoryUuid && !props.categories.some(category => category.categoryUuid === props.modelValue.categoryUuid))
function update(field: keyof LifeTodoForm, event: Event) {
  emit('update:modelValue', { ...props.modelValue, [field]: (event.target as HTMLInputElement).value })
}
</script>

<template>
  <CommonBaseModal :visible="visible" :title="isEditing ? '일상 할 일 수정' : '일상 할 일 추가'" width="600px" :dismissible="!isSaving" @close="emit('close')">
    <form :id="formId" @submit.prevent="emit('submit')">
      <fieldset class="life-form page-form-grid" :disabled="isSaving">
        <label>제목 <span class="section-description">필수</span>
          <input class="input-field" :value="modelValue.title" required maxlength="200" placeholder="예: 주말 장보기" @input="update('title', $event)">
        </label>
        <label>생활 카테고리
          <select class="input-field" :value="modelValue.categoryUuid" @change="update('categoryUuid', $event)">
            <option value="">미분류</option>
            <option v-if="missingCategory" :value="modelValue.categoryUuid">기존 카테고리</option>
            <option v-for="category in categories" :key="category.categoryUuid" :value="category.categoryUuid">{{ category.name }}</option>
          </select>
        </label>
        <label>날짜 <span class="section-description">선택</span>
          <input class="input-field" type="date" :value="modelValue.dueDate" @input="update('dueDate', $event)">
        </label>
        <label>메모 <span class="section-description">선택</span>
          <textarea class="input-field life-form__memo" :value="modelValue.memo" maxlength="4000" rows="4" placeholder="필요한 내용을 적어두세요." @input="update('memo', $event)" />
        </label>
        <p v-if="errorMessage" class="life-form__error" role="alert">{{ errorMessage }}</p>
      </fieldset>
    </form>
    <template #actions>
      <CommonBaseButton variant="secondary" :disabled="isSaving" @click="emit('close')">취소</CommonBaseButton>
      <CommonBaseButton type="submit" :form="formId" :loading="isSaving">{{ isEditing ? '수정' : '추가' }}</CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.life-form { border: 0; padding: 0; margin: 0; }
.life-form label { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; min-width: 0; }
.life-form label .input-field { flex: 0 0 100%; width: 100%; min-width: 0; box-sizing: border-box; }
.life-form__memo { resize: vertical; min-height: 108px; padding-block: 10px; }
.life-form__error { margin: 0; color: var(--color-danger); }
</style>
