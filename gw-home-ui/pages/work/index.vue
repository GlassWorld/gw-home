<script setup lang="ts">
import WorkUnitManagementPage from '~/features/work/components/WorkUnitManagementPage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <WorkUnitManagementPage />
</template>
