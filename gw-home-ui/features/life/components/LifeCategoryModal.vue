<script setup lang="ts">
import { useLifeTodoApi } from '~/composables/use-life-todo-api'
import { lifeTodoErrorMessage } from '../api/life-todo.api'
import type { LifeCategory } from '../types/life-todo.types'

const emit = defineEmits<{ close: []; changed: [] }>()
const api = useLifeTodoApi()
const { showToast } = useToast()
const categories = ref<LifeCategory[]>([])
const name = ref('')
const editingUuid = ref('')
const deleteTarget = ref<LifeCategory | null>(null)
const isLoading = ref(false)
const isSaving = ref(false)
const loadError = ref('')
const formError = ref('')
const isBusy = computed(() => isLoading.value || isSaving.value)
const nameInput = ref<HTMLInputElement | null>(null)

async function load() {
  isLoading.value = true
  loadError.value = ''
  try { categories.value = await api.fetchCategories() }
  catch { loadError.value = '생활 카테고리를 불러오지 못했습니다.' }
  finally { isLoading.value = false }
}

function edit(category?: LifeCategory) {
  editingUuid.value = category?.categoryUuid ?? ''
  name.value = category?.name ?? ''
  formError.value = ''
  deleteTarget.value = null
  nameInput.value?.focus()
}

async function save() {
  if (isBusy.value) return
  if (!name.value.trim()) { formError.value = '카테고리 이름을 입력해주세요.'; return }
  isSaving.value = true
  formError.value = ''
  try {
    await api.saveCategory(name.value, editingUuid.value || undefined)
    edit()
    showToast('생활 카테고리를 저장했습니다.', { variant: 'success' })
    await load()
    emit('changed')
  } catch (error) { formError.value = lifeTodoErrorMessage(error, '생활 카테고리를 저장하지 못했습니다.') }
  finally { isSaving.value = false }
}

async function remove() {
  if (isBusy.value || !deleteTarget.value) return
  isSaving.value = true
  formError.value = ''
  try {
    await api.deleteCategory(deleteTarget.value.categoryUuid)
    edit()
    showToast('카테고리를 삭제하고 할 일은 미분류로 옮겼습니다.', { variant: 'success' })
    await load()
    emit('changed')
  } catch (error) { formError.value = lifeTodoErrorMessage(error, '생활 카테고리를 삭제하지 못했습니다.') }
  finally { isSaving.value = false }
}
onMounted(load)
</script>

<template>
  <CommonBaseModal :visible="true" title="생활 카테고리 관리" width="600px" :dismissible="!isSaving" @close="emit('close')">
    <p class="section-description">집안일, 장보기, 개인 용무처럼 원하는 이름으로 분류하세요.</p>
    <form class="category-editor" @submit.prevent="save">
      <label :for="'life-category-name'">{{ editingUuid ? '카테고리 이름 변경' : '새 카테고리' }}</label>
      <input id="life-category-name" ref="nameInput" v-model="name" class="input-field" required maxlength="50" :disabled="isBusy" placeholder="예: 집안일">
      <div class="page-actions">
        <CommonBaseButton v-if="editingUuid" variant="secondary" :disabled="isBusy" @click="edit()">수정 취소</CommonBaseButton>
        <CommonBaseButton type="submit" :disabled="isBusy" :loading="isSaving">{{ editingUuid ? '이름 저장' : '카테고리 추가' }}</CommonBaseButton>
      </div>
    </form>
    <p v-if="formError" class="category-error" role="alert">{{ formError }}</p>
    <CommonBaseState v-if="isLoading" state="loading" message="카테고리를 불러오는 중입니다." />
    <CommonBaseState v-else-if="loadError" state="error" :message="loadError" action-label="다시 시도" @action="load" />
    <ul v-else-if="categories.length" class="category-list">
      <li v-for="category in categories" :key="category.categoryUuid">
        <span>{{ category.name }}</span>
        <div class="page-actions">
          <CommonBaseButton size="small" variant="secondary" :disabled="isBusy" :aria-label="`${category.name} 이름 변경`" @click="edit(category)">수정</CommonBaseButton>
          <CommonBaseButton size="small" variant="secondary" :disabled="isBusy" :aria-label="`${category.name} 카테고리 삭제`" @click="deleteTarget = category">삭제</CommonBaseButton>
        </div>
      </li>
    </ul>
    <CommonBaseState v-else state="empty" message="아직 생활 카테고리가 없습니다. 카테고리 없이도 할 일을 등록할 수 있습니다." />
    <div v-if="deleteTarget" class="category-confirm" role="alert">
      <p>‘{{ deleteTarget.name }}’ 카테고리를 삭제할까요? 기존 할 일은 미분류로 남습니다.</p>
      <div class="page-actions">
        <CommonBaseButton variant="secondary" :disabled="isSaving" @click="deleteTarget = null">삭제 취소</CommonBaseButton>
        <CommonBaseButton variant="danger" :loading="isSaving" @click="remove">카테고리 삭제 확인</CommonBaseButton>
      </div>
    </div>
    <template #actions><CommonBaseButton variant="secondary" :disabled="isSaving" @click="emit('close')">닫기</CommonBaseButton></template>
  </CommonBaseModal>
</template>

<style scoped>
.category-editor { display: grid; gap: 8px; min-width: 0; }
.category-list { display: grid; gap: 12px; list-style: none; padding: 0; margin: 0; }
.category-list li { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; padding: 12px 0; border-top: 1px solid var(--color-border); }
.category-list li > span { overflow-wrap: anywhere; min-width: 0; flex: 1 1 120px; }
.category-confirm { padding: var(--card-padding); background: var(--color-surface-muted); overflow-wrap: anywhere; }
.category-error { margin: 0; color: var(--color-danger); }
</style>
