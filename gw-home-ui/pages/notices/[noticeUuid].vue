<script setup lang="ts">
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import type { NoticeDetail } from '~/types/api/notice'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const { fetchNotice } = useNoticeApi()

const notice = ref<NoticeDetail | null>(null)
const errorMessage = ref('')

function renderMarkdown(rawValue: string | null | undefined): string {
  const normalizedValue = rawValue?.trim()

  if (!normalizedValue) {
    return '<p>내용이 없습니다.</p>'
  }

  const parsedHtml = marked.parse(normalizedValue, { async: false })

  if (!import.meta.client) {
    return parsedHtml
  }

  return DOMPurify.sanitize(parsedHtml)
}

const renderedContent = computed(() => renderMarkdown(notice.value?.content))

try {
  notice.value = await fetchNotice(String(route.params.noticeUuid))
} catch (error) {
  const fetchError = error as { data?: { message?: string } }
  errorMessage.value = fetchError.data?.message ?? '공지사항 상세를 불러오지 못했습니다.'
}
</script>

<template>
  <main class="page-container notice-detail-page">
    <section class="content-panel notice-detail-page__panel">
      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <template v-else-if="notice">
        <header class="notice-detail-page__header">
          <p class="notice-detail-page__eyebrow">Notice</p>
          <h1 class="section-title">{{ notice.title }}</h1>
          <div class="meta-row">
            <span>작성 {{ notice.createdBy || '관리자' }}</span>
            <span>등록일 {{ new Date(notice.createdAt).toLocaleString() }}</span>
            <span>조회 {{ notice.viewCount }}</span>
          </div>
        </header>

        <article class="notice-detail-page__content" v-html="renderedContent" />
      </template>

      <p v-else class="message-muted">
        공지사항을 찾는 중입니다.
      </p>
    </section>
  </main>
</template>

<style scoped>
.notice-detail-page__panel {
  padding: 32px;
  display: grid;
  gap: 24px;
}

.notice-detail-page__header {
  display: grid;
  gap: 12px;
}

.notice-detail-page__eyebrow {
  margin: 0;
  color: var(--color-accent);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.notice-detail-page__content {
  padding-top: 24px;
  border-top: 1px solid rgba(147, 210, 255, 0.14);
  line-height: 1.7;
  word-break: break-word;
}

.notice-detail-page__content:deep(p:first-child) {
  margin-top: 0;
}

.notice-detail-page__content:deep(p:last-child) {
  margin-bottom: 0;
}

.notice-detail-page__content:deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(147, 210, 255, 0.12);
  color: #a9dcff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.notice-detail-page__content:deep(pre) {
  margin: 12px 0 0;
  padding: 14px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(3, 12, 24, 0.82);
  border: 1px solid rgba(147, 210, 255, 0.14);
}

.notice-detail-page__content:deep(pre code) {
  padding: 0;
  background: transparent;
}

.notice-detail-page__content:deep(a) {
  color: #8bd0ff;
  text-decoration: underline;
}

.notice-detail-page__content:deep(ul),
.notice-detail-page__content:deep(ol) {
  padding-left: 20px;
}
</style>
