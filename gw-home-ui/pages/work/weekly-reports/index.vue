<script setup lang="ts">
import WeeklyReportListPage from '~/features/work/components/WeeklyReportListPage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <WeeklyReportListPage />
</template>
