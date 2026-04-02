<script setup lang="ts">
import WorkGitManagementPage from '~/features/work/components/WorkGitManagementPage.vue'

definePageMeta({
  middleware: 'auth'
})
</script>

<template>
  <WorkGitManagementPage />
</template>
