<script setup lang="ts">
import type { DailyReport } from '~/types/work'
import DailyReportWorkspace from './DailyReportWorkspace.vue'

const props = withDefaults(defineProps<{
  visible: boolean
  dailyReportUuid?: string
}>(), {
  dailyReportUuid: ''
})

const emit = defineEmits<{
  close: []
  saved: [report: DailyReport]
}>()
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    eyebrow="Daily Report"
    :title="dailyReportUuid ? '일일보고 수정' : '일일보고 작성'"
    width="calc(100vw - 24px)"
    immersive
    :z-index="40"
    @close="emit('close')"
  >
    <DailyReportWorkspace
      :daily-report-uuid="dailyReportUuid"
      modal-mode
      :modal-z-index-base="40"
      @cancel="emit('close')"
      @saved="emit('saved', $event)"
    />
  </CommonBaseModal>
</template>
