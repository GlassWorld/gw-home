<script setup lang="ts">
import DashboardWorkspacePage from '~/features/dashboard/components/DashboardWorkspacePage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template>
  <DashboardWorkspacePage />
</template>
