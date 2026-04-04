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
  <section class="content-panel notice-list-panel">
    <div class="notice-list-panel__header">
      <p class="message-muted">총 {{ totalCount }}건</p>
    </div>

    <p v-if="errorMessage" class="message-error">
      {{ errorMessage }}
    </p>
    <p v-else-if="isLoading" class="message-muted">
      공지사항을 불러오는 중입니다.
    </p>
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
    <p v-else class="message-muted">
      등록된 공지사항이 없습니다.
    </p>

    <div class="notice-list-panel__pagination">
      <CommonBaseButton
        variant="secondary"
        :disabled="page <= 1"
        @click="emit('previous')"
      >
        이전
      </CommonBaseButton>
      <span>{{ page }} / {{ totalPages || 1 }}</span>
      <CommonBaseButton
        variant="secondary"
        :disabled="page >= totalPages"
        @click="emit('next')"
      >
        다음
      </CommonBaseButton>
    </div>
  </section>
</template>

<style scoped>
.notice-list-panel {
  padding: 28px;
}

.notice-list-panel__list,
.notice-list-panel__pagination {
  display: grid;
  gap: 14px;
}

.notice-list-panel__list {
  margin: 20px 0;
}

.notice-list-panel__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(147, 210, 255, 0.12);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.notice-list-panel__item-main,
.notice-list-panel__item-meta {
  display: grid;
  gap: 6px;
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
  border-color: rgba(147, 210, 255, 0.26);
}

.notice-list-panel__item:focus-visible {
  outline: 2px solid rgba(110, 193, 255, 0.24);
  outline-offset: 2px;
}

.notice-list-panel__pagination {
  grid-template-columns: repeat(3, auto);
  justify-content: center;
  align-items: center;
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
