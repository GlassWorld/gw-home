<script setup lang="ts">
import WorkTodoDetailPage from '~/features/work/components/WorkTodoDetailPage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <WorkTodoDetailPage />
</template>
