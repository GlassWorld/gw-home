<script setup lang="ts">
import type { NoticeSummary } from '~/types/api/notice'

defineProps<{
  notices: NoticeSummary[]
  totalCount: number
  isLoading: boolean
  errorMessage: string
  page: number
  totalPages: number
}>()

const emit = defineEmits<{
  open: [noticeUuid: string]
  previous: []
  next: []
}>()
</script>

<template>
  <section class="content-panel page-panel-padding-lg notice-list-panel">
    <div class="notice-list-panel__header">
      <p class="message-muted">총 {{ totalCount }}건</p>
    </div>

    <CommonBaseState v-if="errorMessage" state="error" :message="errorMessage" />
    <CommonBaseState v-else-if="isLoading" state="loading" message="공지사항을 불러오는 중입니다." />
    <div v-else-if="notices.length" class="notice-list-panel__list">
      <button
        v-for="notice in notices"
        :key="notice.noticeUuid"
        class="notice-list-panel__item"
        type="button"
        @click="emit('open', notice.noticeUuid)"
      >
        <div class="notice-list-panel__item-main">
          <strong>{{ notice.title }}</strong>
          <span>작성 {{ notice.createdBy || '관리자' }}</span>
        </div>
        <div class="notice-list-panel__item-meta">
          <span>{{ new Date(notice.createdAt).toLocaleDateString() }}</span>
          <span>조회 {{ notice.viewCount }}</span>
        </div>
      </button>
    </div>
    <CommonBaseState v-else state="empty" message="등록된 공지사항이 없습니다." />

    <CommonBasePagination :page="page" :total-pages="totalPages" @previous="emit('previous')" @next="emit('next')" />
  </section>
</template>

<style scoped>
.notice-list-panel {
  display: grid;
  gap: var(--section-gap);
}

.notice-list-panel__list {
  display: grid;
  gap: 14px;
}

.notice-list-panel__list {
  min-width: 0;
}

.notice-list-panel__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
  padding: var(--card-padding) var(--card-padding);
  border-radius: var(--radius-medium);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-border);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.notice-list-panel__item-main,
.notice-list-panel__item-meta {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.notice-list-panel__item-main strong {
  font-size: 1.02rem;
}

.notice-list-panel__item-main span,
.notice-list-panel__item-meta {
  color: var(--color-text-muted);
}

.notice-list-panel__item-meta {
  justify-items: end;
  white-space: nowrap;
}

.notice-list-panel__item:hover {
  transform: translateY(-1px);
  border-color: var(--color-border);
}

.notice-list-panel__item:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

@media (max-width: 768px) {
  .notice-list-panel__item {
    flex-direction: column;
    align-items: flex-start;
  }

  .notice-list-panel__item-meta {
    justify-items: start;
  }
}
</style>
