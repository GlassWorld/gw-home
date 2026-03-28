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
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.credential-detail__memo:deep(p:first-child) {
  margin-top: 0;
}

.credential-detail__memo:deep(p:last-child) {
  margin-bottom: 0;
}

.credential-detail__memo:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.92em;
}

.credential-detail__memo:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.credential-detail__memo:deep(pre code) {
  padding: 0;
  background: transparent;
}

.credential-detail__memo:deep(a) {
  color: #8bd0ff;
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
}
</style>
