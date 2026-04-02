<script setup lang="ts">
import DailyReportEditorPage from '~/features/work/components/DailyReportEditorPage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <DailyReportEditorPage />
</template>
