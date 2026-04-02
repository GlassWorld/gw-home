<script setup lang="ts">
import DailyReportListPage from '~/features/work/components/DailyReportListPage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <DailyReportListPage />
</template>
