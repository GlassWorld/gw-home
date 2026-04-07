<script setup lang="ts">
import WorkTodoBoardPage from '~/features/work/components/WorkTodoBoardPage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <WorkTodoBoardPage />
</template>
