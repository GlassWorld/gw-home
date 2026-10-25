<script setup lang="ts">
import LifeTodoBoardPage from '~/features/life/components/LifeTodoBoardPage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template><LifeTodoBoardPage /></template>
