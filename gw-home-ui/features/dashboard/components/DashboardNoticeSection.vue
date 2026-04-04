<script setup lang="ts">
import type { NoticeSummary } from '~/types/api/notice'
import { formatDate } from '~/utils/date'

const props = defineProps<{
  notices: NoticeSummary[]
  errorMessage: string
}>()

const emit = defineEmits<{
  open: [noticeUuid: string]
}>()
</script>

<template>
  <article class="dashboard-section content-panel">
    <div class="dashboard-section__header">
      <div>
        <h2>공지사항</h2>
        <p class="message-muted">최신 안내 4건</p>
      </div>
      <CommonBaseButton variant="secondary" to="/notices">
        전체
      </CommonBaseButton>
    </div>

    <p v-if="props.errorMessage" class="message-error">
      {{ props.errorMessage }}
    </p>
    <div v-else-if="props.notices.length" class="dashboard-compact-list">
      <button
        v-for="notice in props.notices"
        :key="notice.noticeUuid"
        class="dashboard-compact-list__item"
        type="button"
        @click="emit('open', notice.noticeUuid)"
      >
        <strong>{{ notice.title }}</strong>
        <span>{{ formatDate(notice.createdAt, { month: 'numeric', day: 'numeric' }) }}</span>
      </button>
    </div>
    <p v-else class="message-muted">
      공지사항이 없습니다.
    </p>
  </article>
</template>

<style scoped>
.dashboard-section {
  padding: 24px;
}

.dashboard-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-section__header h2 {
  margin: 0;
  font-size: 1.05rem;
}

.dashboard-section__header p {
  margin: 6px 0 0;
}

.dashboard-compact-list {
  display: grid;
  gap: 12px;
}

.dashboard-compact-list__item {
  width: 100%;
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.dashboard-compact-list__item strong {
  font-size: 0.96rem;
  line-height: 1.4;
}

.dashboard-compact-list__item span {
  color: var(--color-text-muted);
}

.dashboard-compact-list__item:hover {
  border-color: rgba(147, 210, 255, 0.24);
}

.dashboard-compact-list__item:focus-visible {
  outline: 2px solid rgba(110, 193, 255, 0.24);
  outline-offset: 2px;
}

@media (max-width: 768px) {
  .dashboard-section__header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
