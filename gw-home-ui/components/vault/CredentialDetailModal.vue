<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
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

const parsedMemo = computed(() => {
  const rawMemo = props.credential?.memo?.trim()

  if (!rawMemo) {
    return '-'
  }

  const parsedHtml = marked.parse(rawMemo, { async: false })

  if (!import.meta.client) {
    return parsedHtml
  }

  return DOMPurify.sanitize(parsedHtml)
})

function getCategoryStyle(categoryColor: string | null) {
  if (!categoryColor) {
    return {
      background: 'rgba(110, 193, 255, 0.12)',
      color: 'var(--color-accent)'
    }
  }

  return {
    background: `${categoryColor}22`,
    color: categoryColor
  }
}

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
      <div class="credential-detail__category-list">
        <span
          v-for="category in credential.categories"
          :key="category.categoryUuid"
          class="credential-detail__category-badge"
          :style="getCategoryStyle(category.color)"
        >
          {{ category.name }}
        </span>
        <span v-if="credential.categories.length === 0" class="credential-detail__category-badge">
          미분류
        </span>
      </div>
    </template>

    <dl class="credential-detail">
      <div class="credential-detail__item credential-detail__item--wide">
        <dt>메모</dt>
        <dd class="credential-detail__memo" v-html="parsedMemo"></dd>
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
      <div class="credential-detail__actions">
        <CommonBaseButton variant="secondary" @click="copyPassword">
          복사
        </CommonBaseButton>
        <CommonBaseButton variant="secondary" @click="emit('edit', credential)">
          수정
        </CommonBaseButton>
        <CommonBaseButton variant="danger" :disabled="isDeleting" @click="handleDelete">
          {{ isDeleting ? '삭제 중...' : '삭제' }}
        </CommonBaseButton>
      </div>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.credential-detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--section-gap);
  margin: 0;
  min-width: 0;
}

.credential-detail__category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.credential-detail__category-badge {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  flex: none;
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.credential-detail__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}

.credential-detail__item {
  min-width: 0;
  padding: var(--card-padding);
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-soft);
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
  overflow-wrap: anywhere;
  word-break: break-word;
}

.credential-detail__password {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  word-break: break-all;
}

.credential-detail__memo:deep(p:first-child) {
  margin-top: 0;
}

.credential-detail__memo:deep(p:last-child) {
  margin-bottom: 0;
}

.credential-detail__memo:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.92em;
}

.credential-detail__memo:deep(pre) {
  max-width: 100%;
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-border);
}

.credential-detail__memo:deep(pre code) {
  padding: 0;
  background: transparent;
  white-space: pre;
}

.credential-detail__memo:deep(a) {
  color: var(--color-primary);
  text-decoration: underline;
}

.credential-detail__memo:deep(ul),
.credential-detail__memo:deep(ol) {
  padding-left: 20px;
}

@media (max-width: 768px) {
  .credential-detail {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .credential-detail__category-list {
    gap: 6px;
  }

  .credential-detail__category-badge {
    max-width: min(100%, 180px);
    font-size: 0.72rem;
  }

  .credential-detail__item {
    padding: 10px;
  }

  .credential-detail__item dt {
    margin-bottom: 6px;
    font-size: 0.82rem;
  }

  .credential-detail__item dd {
    font-size: 0.9rem;
  }

  .credential-detail__actions {
    justify-content: flex-start;
    gap: 6px;
  }

  .credential-detail__actions :deep(.base-button) {
    width: auto;
    min-height: 30px;
    padding: 0 10px;
    font-size: 0.74rem;
    letter-spacing: 0;
  }
}

@media (max-width: 420px) {
  .credential-detail__item {
    padding: 12px;
  }

  .credential-detail__category-badge {
    max-width: min(100%, 140px);
  }

  .credential-detail__actions {
    gap: 5px;
  }

  .credential-detail__actions :deep(.base-button) {
    padding: 0 8px;
    font-size: 0.7rem;
  }
}
</style>
