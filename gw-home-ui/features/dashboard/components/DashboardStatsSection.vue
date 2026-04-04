<script setup lang="ts">
defineProps<{
  currentWeekLabel: string
  stats: { label: string; value: string }[]
}>()
</script>

<template>
  <article class="dashboard-section content-panel">
    <div class="dashboard-section__header">
      <div>
        <h2>보고통계</h2>
        <p class="message-muted">{{ currentWeekLabel }}</p>
      </div>
    </div>

    <div class="dashboard-stat-grid">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="dashboard-stat-grid__card"
      >
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </div>
    </div>
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

.dashboard-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-stat-grid__card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.dashboard-stat-grid__card span {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.dashboard-stat-grid__card strong {
  font-size: 1.32rem;
}

@media (max-width: 768px) {
  .dashboard-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
