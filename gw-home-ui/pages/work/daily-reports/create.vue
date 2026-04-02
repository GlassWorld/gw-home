<script setup lang="ts">
import DailyReportWorkspace from '~/features/work/components/DailyReportWorkspace.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <DailyReportWorkspace />
</template>
