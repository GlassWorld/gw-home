<script setup lang="ts">
defineProps<{
  keyword: string
}>()

const emit = defineEmits<{
  updateKeyword: [value: string]
  submit: []
}>()
</script>

<template>
  <section class="content-panel notice-search-hero">
    <div>
      <p class="notice-search-hero__eyebrow">Notice</p>
      <h1 class="section-title">공지사항</h1>
      <p class="section-description">중요한 운영 안내를 한곳에서 확인할 수 있습니다.</p>
    </div>

    <form class="notice-search-hero__form" @submit.prevent="emit('submit')">
      <input
        class="input-field"
        type="search"
        placeholder="공지 제목 검색"
        :value="keyword"
        @input="emit('updateKeyword', ($event.target as HTMLInputElement).value)"
      >
      <CommonBaseButton type="submit">
        검색
      </CommonBaseButton>
    </form>
  </section>
</template>

<style scoped>
.notice-search-hero {
  display: grid;
  gap: 18px;
  padding: 28px;
}

.notice-search-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--color-accent);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.notice-search-hero__form {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

@media (max-width: 768px) {
  .notice-search-hero__form {
    grid-template-columns: 1fr;
  }
}
</style>
