<script setup lang="ts">
definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <main class="page-container">
    <section class="content-panel create-board-page">
      <h1 class="section-title">게시글 작성</h1>
      <p class="section-description">
        대시보드의 작성 링크가 끊기지 않도록 기본 페이지를 먼저 연결했습니다.
      </p>
      <p class="message-muted">
        작성 폼은 다음 단계에서 확장할 수 있습니다.
      </p>
    </section>
  </main>
</template>

<style scoped>
.create-board-page {
  padding: 28px;
}
</style>
