<script setup lang="ts">
import type { NoticeDetail } from '~/types/api/notice'
import { formatDateTime } from '~/utils/date'
import { renderMarkdown } from '~/utils/markdown'

const props = defineProps<{
  visible: boolean
  notice: NoticeDetail | null
  isLoading: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  close: []
}>()

const renderedContent = computed(() => renderMarkdown(props.notice?.content))
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    :title="props.notice?.title ?? '공지사항 상세'"
    eyebrow="Notice"
    width="min(920px, 100%)"
    @close="emit('close')"
  >
    <p v-if="props.errorMessage" class="message-error">
      {{ props.errorMessage }}
    </p>

    <p v-else-if="props.isLoading" class="message-muted">
      공지사항 상세를 불러오는 중입니다.
    </p>

    <template v-else-if="props.notice">
      <header class="notice-detail-modal__header">
        <div class="meta-row">
          <span>작성 {{ props.notice.createdBy || '관리자' }}</span>
          <span>등록일 {{ formatDateTime(props.notice.createdAt) }}</span>
          <span>조회 {{ props.notice.viewCount }}</span>
        </div>
      </header>

      <article class="notice-detail-modal__content" v-html="renderedContent" />
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.notice-detail-modal__header {
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.14);
}

.notice-detail-modal__content {
  line-height: 1.7;
  word-break: break-word;
}

.notice-detail-modal__content:deep(p:first-child) {
  margin-top: 0;
}

.notice-detail-modal__content:deep(p:last-child) {
  margin-bottom: 0;
}

.notice-detail-modal__content:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.notice-detail-modal__content:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.notice-detail-modal__content:deep(pre code) {
  padding: 0;
  background: transparent;
}

.notice-detail-modal__content:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.notice-detail-modal__content:deep(ul),
.notice-detail-modal__content:deep(ol) {
  padding-left: 20px;
}
</style>
