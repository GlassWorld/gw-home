<script setup lang="ts">
import type { WorkUnit } from '~/features/work/types/work.types'
import { formatDate } from '~/utils/date'

defineProps<{
  workUnits: WorkUnit[]
  errorMessage: string
  getWorkStatusLabel: (status: WorkUnit['status']) => string
}>()
</script>

<template>
  <article class="dashboard-section content-panel">
    <div class="dashboard-section__header">
      <div>
        <h2>활성화된 업무내역</h2>
        <p class="message-muted">최근 수정 기준 4건</p>
      </div>
      <CommonBaseButton variant="secondary" to="/work">
        전체
      </CommonBaseButton>
    </div>

    <p v-if="errorMessage" class="message-error">
      {{ errorMessage }}
    </p>
    <div v-else-if="workUnits.length" class="dashboard-work-list">
      <article
        v-for="workUnit in workUnits"
        :key="workUnit.workUnitUuid"
        class="dashboard-work-list__item"
      >
        <div class="dashboard-work-list__header">
          <strong>{{ workUnit.title }}</strong>
          <span>{{ getWorkStatusLabel(workUnit.status) }}</span>
        </div>
        <p>{{ workUnit.category || '카테고리 없음' }}</p>
        <small>최근 수정 {{ formatDate(workUnit.updatedAt) }}</small>
      </article>
    </div>
    <p v-else class="message-muted">
      활성화된 업무가 없습니다.
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

.dashboard-work-list {
  display: grid;
  gap: 12px;
}

.dashboard-work-list__item {
  display: grid;
  gap: 6px;
  padding: 10px var(--card-padding);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  background: var(--color-surface);
  box-shadow: var(--shadow-soft);
}

.dashboard-work-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dashboard-work-list__item strong {
  font-size: 0.96rem;
  line-height: 1.4;
}

.dashboard-work-list__item p,
.dashboard-work-list__item small {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .dashboard-section__header,
  .dashboard-work-list__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
