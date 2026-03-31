<script setup lang="ts">
definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const dailyReportUuid = computed(() => String(route.params.dailyReportUuid ?? ''))
</script>

<template>
  <WorkDailyReportWorkspace :daily-report-uuid="dailyReportUuid" />
</template>
