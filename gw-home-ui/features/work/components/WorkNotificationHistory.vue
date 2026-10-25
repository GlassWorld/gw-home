<script setup lang="ts">
import type { PageResponse } from '~/types/api/common'
import type { WorkNotificationDelivery, WorkDeliveryStatus } from '../types/work-notification.types'
import { formatNotificationTime } from '../utils/notification-time'
defineProps<{ history: PageResponse<WorkNotificationDelivery> | null; loading: boolean; error: string; timeZone: string }>()
defineEmits<{ load: [page: number] }>()
const labels: Record<WorkDeliveryStatus, string> = { PENDING: '예약됨', CLAIMED: '발송 준비', SENDING: '발송 중', SENT: '발송 완료', RETRY: '재시도 대기', FAILED: '발송 실패', CANCELLED: '취소됨', UNKNOWN: '접수 확인 필요' }
</script>

<template>
  <section class="page-stack" aria-label="메일 발송 내역">
    <div class="page-section-header">
      <h3>발송 내역</h3>
      <CommonBaseButton variant="secondary" :disabled="loading" @click="$emit('load', history?.page ?? 1)">{{ history ? '새로고침' : '내역 보기' }}</CommonBaseButton>
    </div>
    <CommonBaseState v-if="loading" state="loading" message="발송 내역을 불러오고 있습니다." />
    <CommonBaseState v-else-if="error" state="error" :message="error" />
    <template v-else-if="history">
      <CommonBaseState v-if="!history.content.length" state="empty" message="아직 예약·발송 내역이 없습니다." />
      <ul v-else class="notification-history">
        <li v-for="item in history.content" :key="item.deliveryUuid" class="page-info-card">
          <div class="page-section-header"><strong>{{ item.kind === 'DIGEST' ? '정기 요약' : '개별 알림' }}</strong><span>{{ labels[item.status] }}</span></div>
          <p>예약: {{ formatNotificationTime(item.dueAt, timeZone) }}</p>
          <p v-if="item.sentAt">발송: {{ formatNotificationTime(item.sentAt, timeZone) }}</p>
          <p v-if="item.status === 'RETRY'">다음 시도: {{ formatNotificationTime(item.nextTryAt, timeZone) }}</p>
          <p v-if="item.status === 'UNKNOWN'">중복 발송을 막기 위해 자동으로 다시 보내지 않습니다. 수신함을 확인해 주세요.</p>
          <p v-if="item.resultCode === 'EMPTY_DIGEST'">요약에 포함할 할 일이 없어 건너뛰었습니다.</p>
          <p class="message-muted">발송 시도 {{ item.attempts }}회 · {{ timeZone }}</p>
        </li>
      </ul>
      <CommonBasePagination v-if="history.totalPages > 1" :page="history.page" :total-pages="history.totalPages" :disabled="loading" @previous="$emit('load', history.page - 1)" @next="$emit('load', history.page + 1)" />
    </template>
  </section>
</template>

<style scoped>
.notification-history { display: grid; gap: 12px; }
.notification-history p { margin: 0; overflow-wrap: anywhere; }
.notification-history .page-section-header { margin: 0; }
</style>
