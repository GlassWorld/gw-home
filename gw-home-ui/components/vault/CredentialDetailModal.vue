<script setup lang="ts">
import type { Credential } from '~/types/vault'

const props = defineProps<{
  visible: boolean
  credential: Credential | null
}>()

const emit = defineEmits<{
  edit: [credential: Credential]
  deleted: []
  close: []
}>()

const { removeCredential } = useVaultApi()
const { showToast } = useToast()
const { confirm } = useDialog()
const isDeleting = ref(false)

async function copyPassword() {
  if (!props.credential) {
    return
  }

  try {
    await navigator.clipboard.writeText(props.credential.password)
    showToast('비밀번호를 복사했습니다.', { variant: 'success' })
  } catch {
    showToast('클립보드 복사에 실패했습니다.', { variant: 'error' })
  }
}

async function handleDelete() {
  if (!props.credential || isDeleting.value) {
    return
  }

  const shouldDelete = await confirm('이 자격증명을 삭제할까요?', {
    title: '자격증명 삭제',
    confirmText: '삭제',
    cancelText: '취소'
  })

  if (!shouldDelete) {
    return
  }

  isDeleting.value = true

  try {
    await removeCredential(props.credential.credentialUuid)
    emit('deleted')
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '삭제에 실패했습니다.', { variant: 'error' })
  } finally {
    isDeleting.value = false
  }
}
</script>

<template>
  <CommonBaseModal
    v-if="credential"
    :visible="visible"
    eyebrow="Credential Detail"
    :title="credential.title"
    width="760px"
    @close="emit('close')"
  >
    <template #title-extra>
      <span class="credential-detail__category-badge">
        {{ credential.categoryName || '미분류' }}
      </span>
    </template>

    <dl class="credential-detail">
      <div class="credential-detail__item credential-detail__item--wide">
        <dt>메모</dt>
        <dd>{{ credential.memo || '-' }}</dd>
      </div>
      <div class="credential-detail__item">
        <dt>아이디</dt>
        <dd>{{ credential.loginId || '-' }}</dd>
      </div>
      <div class="credential-detail__item">
        <dt>비밀번호</dt>
        <dd class="credential-detail__password">{{ credential.password }}</dd>
      </div>
    </dl>

    <template #actions>
      <CommonBaseButton variant="secondary" @click="copyPassword">
        복사
      </CommonBaseButton>
      <CommonBaseButton variant="secondary" @click="emit('edit', credential)">
        수정
      </CommonBaseButton>
      <CommonBaseButton variant="danger" :disabled="isDeleting" @click="handleDelete">
        {{ isDeleting ? '삭제 중...' : '삭제' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.credential-detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 0;
}

.credential-detail__category-badge {
  display: inline-flex;
  align-items: center;
  flex: none;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
}

.credential-detail__item {
  padding: 16px;
  border-radius: var(--radius-medium);
  background: rgba(7, 21, 39, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.credential-detail__item--wide {
  grid-column: 1 / -1;
}

.credential-detail__item dt {
  margin-bottom: 8px;
  color: var(--color-text-muted);
  font-weight: 700;
}

.credential-detail__item dd {
  margin: 0;
  color: var(--color-text);
  line-height: 1.6;
  word-break: break-word;
}

.credential-detail__password {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

@media (max-width: 768px) {
  .credential-detail {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>
