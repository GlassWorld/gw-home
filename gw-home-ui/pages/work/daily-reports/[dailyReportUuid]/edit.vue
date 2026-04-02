<script setup lang="ts">
import DailyReportWorkspace from '~/features/work/components/DailyReportWorkspace.vue'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const dailyReportUuid = computed(() => String(route.params.dailyReportUuid ?? ''))
</script>

<template>
  <DailyReportWorkspace :daily-report-uuid="dailyReportUuid" />
</template>
