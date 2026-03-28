<script setup lang="ts">
import type { Credential } from '~/types/vault'

const props = defineProps<{
  credential: Credential
}>()

const emit = defineEmits<{
  copy: [credential: Credential]
  detail: [credential: Credential]
}>()

const memoPreview = computed(() => {
  return props.credential.memo?.trim() || '메모가 없습니다.'
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
</script>

<template>
  <article class="credential-card content-panel">
    <div class="credential-card__body">
      <div class="credential-card__title-row">
        <h2 class="credential-card__title">{{ credential.title }}</h2>
      </div>
      <div v-if="credential.categories.length" class="credential-card__category-list">
        <p
          v-for="category in credential.categories"
          :key="category.categoryUuid"
          class="credential-card__category"
          :style="getCategoryStyle(category.color)"
        >
          {{ category.name }}
        </p>
      </div>
      <p class="credential-card__description">{{ memoPreview }}</p>
    </div>

    <div class="credential-card__footer">
      <CommonBaseButton variant="secondary" size="small" @click="emit('copy', credential)">
        복사
      </CommonBaseButton>
      <CommonBaseButton size="small" @click="emit('detail', credential)">
        상세
      </CommonBaseButton>
    </div>
  </article>
</template>

<style scoped>
.credential-card {
  display: grid;
  gap: 18px;
  padding: 22px;
  align-self: start;
}

.credential-card__body {
  display: grid;
  gap: 8px;
}

.credential-card__title-row {
  display: flex;
  min-width: 0;
}

.credential-card__title {
  margin: 0;
  font-size: 1.2rem;
  min-width: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.credential-card__category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.credential-card__category {
  margin: 0;
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  color: var(--color-accent);
  font-size: 0.8rem;
  font-weight: 700;
  flex: none;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.credential-card__description {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.credential-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.credential-card__footer :deep(button) {
  flex: 1;
  min-height: 34px;
  align-self: center;
}

@media (max-width: 768px) {
  .credential-card {
    gap: 16px;
    padding: 18px;
  }

  .credential-card__footer {
    justify-content: flex-start;
  }

  .credential-card__footer :deep(button) {
    flex: 0 0 auto;
  }
}
</style>
