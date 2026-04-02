<script setup lang="ts">
import DailyReportEditorPage from '~/features/work/components/DailyReportEditorPage.vue'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const dailyReportUuid = computed(() => String(route.params.dailyReportUuid ?? ''))
</script>

<template>
  <DailyReportEditorPage :daily-report-uuid="dailyReportUuid" />
</template>
