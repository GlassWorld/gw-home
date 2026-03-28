<script setup lang="ts">
import type { Credential, SaveCredentialPayload, VaultCategory } from '~/types/vault'

const props = defineProps<{
  visible: boolean
  credential?: Credential | null
}>()

const emit = defineEmits<{
  saved: []
  close: []
}>()

const { createCredential, updateCredential } = useVaultApi()
const { fetchCategoryList } = useVaultCategoryApi()
const { showToast } = useToast()
const categoryList = ref<VaultCategory[]>([])
const categorySearchKeyword = ref('')

const filteredCategoryList = computed(() => {
  const normalizedKeyword = categorySearchKeyword.value.trim().toLowerCase()

  if (!normalizedKeyword) {
    return categoryList.value
  }

  return categoryList.value.filter((category) => category.name.toLowerCase().includes(normalizedKeyword))
})

const formState = reactive<SaveCredentialPayload>({
  title: '',
  categoryUuids: [],
  loginId: '',
  password: '',
  memo: ''
})

const isSubmitting = ref(false)

function resetForm() {
  formState.title = props.credential?.title ?? ''
  formState.categoryUuids = props.credential?.categories.map((category) => category.categoryUuid) ?? []
  formState.loginId = props.credential?.loginId ?? ''
  formState.password = props.credential?.password ?? ''
  formState.memo = props.credential?.memo ?? ''
  categorySearchKeyword.value = ''
}

async function loadCategoryList() {
  try {
    categoryList.value = await fetchCategoryList()
  } catch {
    categoryList.value = []
  }
}

function isCategorySelected(categoryUuid: string): boolean {
  return (formState.categoryUuids ?? []).includes(categoryUuid)
}

function toggleCategory(categoryUuid: string) {
  const selectedCategoryUuids = new Set(formState.categoryUuids ?? [])

  if (selectedCategoryUuids.has(categoryUuid)) {
    selectedCategoryUuids.delete(categoryUuid)
  } else {
    selectedCategoryUuids.add(categoryUuid)
  }

  formState.categoryUuids = [...selectedCategoryUuids]
}

async function handleSave() {
  if (isSubmitting.value) {
    return
  }

  if (!formState.title.trim() || !formState.password.trim()) {
    showToast('필수 항목을 확인해주세요.', { variant: 'error' })
    return
  }

  isSubmitting.value = true

  try {
    const payload: SaveCredentialPayload = {
      ...formState,
      categoryUuids: [...(formState.categoryUuids ?? [])],
      title: formState.title.trim(),
      password: formState.password.trim()
    }

    if (props.credential?.credentialUuid) {
      await updateCredential(props.credential.credentialUuid, payload)
    } else {
      await createCredential(payload)
    }

    emit('saved')
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '저장에 실패했습니다.', { variant: 'error' })
  } finally {
    isSubmitting.value = false
  }
}

watch(
  () => [props.visible, props.credential],
  () => {
    if (props.visible) {
      void loadCategoryList()
      resetForm()
    }
  },
  { immediate: true }
)
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    eyebrow="Vault"
    :title="credential ? '자격증명 수정' : '자격증명 등록'"
    width="800px"
    @close="emit('close')"
  >
    <div class="vault-modal__form">
      <div class="vault-modal__primary-fields">
        <label class="vault-modal__field">
          <span>제목 <strong class="vault-modal__required">*</strong></span>
          <input v-model="formState.title" class="input-field" type="text" maxlength="200">
        </label>
        <label class="vault-modal__field">
          <span>아이디</span>
          <input v-model="formState.loginId" class="input-field" type="text" maxlength="200">
        </label>
        <label class="vault-modal__field">
          <span>비밀번호 <strong class="vault-modal__required">*</strong></span>
          <input v-model="formState.password" class="input-field" type="text">
        </label>
      </div>
      <label class="vault-modal__field vault-modal__field--category">
        <span>카테고리</span>
        <div class="vault-modal__category-picker">
          <input
            v-model="categorySearchKeyword"
            class="input-field"
            type="text"
            placeholder="카테고리를 검색하세요"
          >
          <div class="vault-modal__category-list">
            <button
              v-for="category in filteredCategoryList"
              :key="category.categoryUuid"
              class="vault-modal__category-chip"
              :class="{ 'vault-modal__category-chip--selected': isCategorySelected(category.categoryUuid) }"
              type="button"
              @click="toggleCategory(category.categoryUuid)"
            >
              {{ category.name }}
            </button>
            <p v-if="filteredCategoryList.length === 0" class="vault-modal__category-empty">
              검색 결과가 없습니다.
            </p>
          </div>
        </div>
      </label>
      <label class="vault-modal__field vault-modal__field--wide">
        <span>메모</span>
        <textarea v-model="formState.memo" class="vault-modal__textarea" rows="5"></textarea>
      </label>
    </div>

    <template #actions>
      <CommonBaseButton variant="secondary" @click="emit('close')">
        취소
      </CommonBaseButton>
      <CommonBaseButton :disabled="isSubmitting" @click="handleSave">
        {{ isSubmitting ? '저장 중...' : '저장' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.vault-modal__form {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(260px, 320px);
  gap: 16px;
  align-items: start;
}

.vault-modal__primary-fields {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.vault-modal__field {
  display: grid;
  gap: 8px;
}

.vault-modal__field span {
  color: rgba(232, 244, 255, 0.88);
  font-weight: 600;
}

.vault-modal__required {
  color: #ff8f8f;
  font-weight: 700;
}

.vault-modal__category-picker {
  display: grid;
  gap: 10px;
  height: 100%;
}

.vault-modal__category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 48px;
  padding: 12px;
  border: 1px solid rgba(147, 210, 255, 0.22);
  border-radius: var(--radius-small);
  background: rgba(8, 23, 42, 0.72);
}

.vault-modal__field--category {
  min-height: 100%;
  min-width: 0;
}

.vault-modal__category-chip {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(147, 210, 255, 0.22);
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.08);
  color: var(--color-text);
  cursor: pointer;
}

.vault-modal__category-chip--selected {
  border-color: rgba(124, 209, 255, 0.92);
  background: rgba(110, 193, 255, 0.18);
  color: var(--color-accent);
}

.vault-modal__category-empty {
  margin: 0;
  color: var(--color-text-muted);
}

.vault-modal__textarea {
  width: 100%;
  min-height: 120px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.28);
  border-radius: var(--radius-small);
  background: rgba(8, 23, 42, 0.72);
  color: var(--color-text);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  resize: vertical;
}

.vault-modal__form :deep(.input-field) {
  border-color: rgba(147, 210, 255, 0.28);
  background: rgba(8, 23, 42, 0.72);
  color: var(--color-text);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.vault-modal__form :deep(.input-field::placeholder),
.vault-modal__textarea::placeholder {
  color: rgba(147, 210, 255, 0.48);
}

.vault-modal__form :deep(.input-field:focus),
.vault-modal__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.2);
  border-color: rgba(124, 209, 255, 0.92);
  box-shadow:
    0 0 0 4px rgba(15, 92, 158, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.vault-modal__field--wide {
  grid-column: 1 / -1;
}

@media (max-width: 768px) {
  .vault-modal__form {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .vault-modal__primary-fields {
    gap: 12px;
  }
}
</style>
