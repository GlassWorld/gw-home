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

const formState = reactive<SaveCredentialPayload>({
  title: '',
  categoryUuid: '',
  loginId: '',
  password: '',
  memo: ''
})

const isSubmitting = ref(false)

function resetForm() {
  formState.title = props.credential?.title ?? ''
  formState.categoryUuid = props.credential?.categoryUuid ?? ''
  formState.loginId = props.credential?.loginId ?? ''
  formState.password = props.credential?.password ?? ''
  formState.memo = props.credential?.memo ?? ''
}

async function loadCategoryList() {
  try {
    categoryList.value = await fetchCategoryList()
  } catch {
    categoryList.value = []
  }
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
    if (props.credential?.credentialUuid) {
      await updateCredential(props.credential.credentialUuid, {
        ...formState,
        title: formState.title.trim(),
        password: formState.password.trim()
      })
    } else {
      await createCredential({
        ...formState,
        title: formState.title.trim(),
        password: formState.password.trim()
      })
    }

    showToast('저장되었습니다.', { variant: 'success' })
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
    width="720px"
    @close="emit('close')"
  >
    <div class="vault-modal__form">
      <label class="vault-modal__field">
        <span>제목 <strong class="vault-modal__required">*</strong></span>
        <input v-model="formState.title" class="input-field" type="text" maxlength="200">
      </label>
      <label class="vault-modal__field">
        <span>카테고리</span>
        <select v-model="formState.categoryUuid" class="vault-modal__select">
          <option value="">미분류</option>
          <option
            v-for="category in categoryList"
            :key="category.categoryUuid"
            :value="category.categoryUuid"
          >
            {{ category.name }}
          </option>
        </select>
      </label>
      <label class="vault-modal__field">
        <span>아이디</span>
        <input v-model="formState.loginId" class="input-field" type="text" maxlength="200">
      </label>
      <label class="vault-modal__field">
        <span>비밀번호 <strong class="vault-modal__required">*</strong></span>
        <input v-model="formState.password" class="input-field" type="text">
      </label>
      <label class="vault-modal__field">
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
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

.vault-modal__select {
  width: 100%;
  min-height: 48px;
  padding: 0 16px;
  border: 1px solid rgba(147, 210, 255, 0.28);
  border-radius: var(--radius-small);
  background: rgba(8, 23, 42, 0.72);
  color: var(--color-text);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
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
.vault-modal__select:focus,
.vault-modal__textarea:focus {
  outline: 2px solid rgba(110, 193, 255, 0.2);
  border-color: rgba(124, 209, 255, 0.92);
  box-shadow:
    0 0 0 4px rgba(15, 92, 158, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.vault-modal__field:last-child {
  grid-column: 1 / -1;
}

@media (max-width: 768px) {
  .vault-modal__form {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>
