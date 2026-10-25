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
  padding: var(--panel-padding);
  background: var(--color-surface);
  border-color: var(--color-border);
  box-shadow:
    var(--shadow-soft);
  backdrop-filter: blur(18px) saturate(125%);
  -webkit-backdrop-filter: blur(18px) saturate(125%);
}

.dashboard-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--section-gap);
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--section-gap);
  padding: 10px var(--card-padding);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface);
  box-shadow: var(--shadow-soft);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.dashboard-compact-list__item strong {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.96rem;
  line-height: 1.4;
}

.dashboard-compact-list__item span {
  flex-shrink: 0;
  color: var(--color-text-muted);
}

.dashboard-compact-list__item:hover {
  border-color: var(--color-border);
  background: var(--color-surface);
}

.dashboard-compact-list__item:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

@media (max-width: 768px) {
  .dashboard-section__header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
