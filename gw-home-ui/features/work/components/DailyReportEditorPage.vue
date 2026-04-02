<script setup lang="ts">
import DailyReportWorkspace from '~/components/work/DailyReportWorkspace.vue'

defineProps<{
  dailyReportUuid?: string
}>()
</script>

<template>
  <DailyReportWorkspace :daily-report-uuid="dailyReportUuid" />
</template>
