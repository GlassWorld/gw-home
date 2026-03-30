<script setup lang="ts">
const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const { fetchMemo, saveMemo } = useMemoApi()
const { showToast } = useToast()

const memo = ref('')
const isLoading = ref(false)
const isSaving = ref(false)
const errorMessage = ref('')
const lastLoadedVisible = ref(false)

async function loadMemo() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    memo.value = await fetchMemo()
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    errorMessage.value = fetchError.data?.message ?? fetchError.message ?? '메모를 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

async function handleSaveMemo() {
  if (isSaving.value) {
    return
  }

  isSaving.value = true
  errorMessage.value = ''

  try {
    await saveMemo(memo.value)
    showToast('메모가 저장되었습니다.', { variant: 'success' })
    emit('close')
  } catch (error) {
    const fetchError = error as { data?: { message?: string }; message?: string }
    errorMessage.value = fetchError.data?.message ?? fetchError.message ?? '메모를 저장하지 못했습니다.'
  } finally {
    isSaving.value = false
  }
}

watch(() => props.visible, async (visible) => {
  if (visible && !lastLoadedVisible.value) {
    await loadMemo()
  }

  lastLoadedVisible.value = visible
})
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    title="빠른 메모"
    eyebrow="Header"
    width="min(1040px, 92vw)"
    @close="emit('close')"
  >
    <div class="header-memo-modal">
      <p class="message-muted">
        헤더에서 바로 확인하고 수정할 수 있는 개인 메모 공간입니다.
      </p>

      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <p v-else-if="isLoading" class="message-muted">
        메모를 불러오는 중입니다.
      </p>

      <textarea
        v-model="memo"
        class="header-memo-modal__textarea"
        placeholder="해야 할 일, 떠오른 아이디어, 잠깐 적어둘 내용을 남겨 보세요."
        maxlength="2000"
        :disabled="isLoading || isSaving"
      />

      <p class="header-memo-modal__count">
        {{ memo.length }} / 2000
      </p>
    </div>

    <template #actions>
      <CommonBaseButton variant="secondary" :disabled="isSaving" @click="emit('close')">
        닫기
      </CommonBaseButton>
      <CommonBaseButton :disabled="isLoading || isSaving" @click="handleSaveMemo">
        {{ isSaving ? '저장 중...' : '저장' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.header-memo-modal {
  display: grid;
  gap: 14px;
  min-height: min(72vh, 760px);
}

.header-memo-modal__textarea {
  width: 100%;
  min-height: min(56vh, 560px);
  resize: vertical;
  border: 1px solid rgba(147, 210, 255, 0.18);
  border-radius: 20px;
  background: rgba(7, 21, 39, 0.72);
  color: #eff8ff;
  padding: 20px 22px;
  font: inherit;
  line-height: 1.6;
}

.header-memo-modal__textarea:focus {
  outline: 2px solid rgba(95, 186, 255, 0.28);
  border-color: rgba(124, 209, 255, 0.88);
  box-shadow:
    0 0 0 4px rgba(15, 92, 158, 0.16),
    0 10px 24px rgba(5, 28, 52, 0.24);
}

.header-memo-modal__count {
  margin: 0;
  text-align: right;
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

@media (max-width: 768px) {
  .header-memo-modal {
    min-height: auto;
  }

  .header-memo-modal__textarea {
    min-height: min(46vh, 360px);
  }
}
</style>
