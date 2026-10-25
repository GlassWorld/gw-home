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
  <section class="content-panel page-panel-padding-lg page-stack">
    <CommonBasePageHeader title="공지사항" eyebrow="Notice" description="중요한 운영 안내를 한곳에서 확인할 수 있습니다." />

    <form class="notice-search-hero__form" @submit.prevent="emit('submit')">
      <input
        class="input-field"
        type="search"
        aria-label="공지 제목 검색"
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
.notice-search-hero__form { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-3); }
</style>
