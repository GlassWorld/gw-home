<script setup lang="ts">
import WeeklyReportBrowsePage from '~/features/work/components/WeeklyReportBrowsePage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <WeeklyReportBrowsePage />
</template>
