<script setup lang="ts">
import { useWorkTodoReminder } from '../composables/use-work-todo-reminder'
import { formatNotificationTime } from '../utils/notification-time'
const props = defineProps<{ todoUuid: string; completed: boolean; disabled: boolean }>()
const { reminder, localTime, loading, busy, error, timeZone, ready, load, save, cancel } = useWorkTodoReminder(props.todoUuid)
</script>

<template>
  <section class="page-info-card">
    <h3>개별 메일 알림</h3>
    <CommonBaseState v-if="loading" state="loading" message="알림 예약을 확인하고 있습니다." />
    <template v-else>
      <CommonBaseState v-if="error" state="error" :message="error" :action-label="!reminder ? '다시 불러오기' : ''" @action="load" />
      <p v-if="completed" class="message-muted">완료한 할 일의 대기 예약은 취소됩니다. 완료를 취소한 후 새로 예약할 수 있습니다.</p>
      <template v-else-if="reminder">
        <p v-if="!ready" class="message-muted"><NuxtLink to="/settings#work-notifications">설정에서 수신 주소를 확인하고 개별 알림을 켜주세요.</NuxtLink></p>
        <form v-else class="page-stack" @submit.prevent="save">
          <fieldset class="notification-fields page-stack" :disabled="disabled || busy">
            <p v-if="reminder.enabled" class="message-muted">지정된 시각: {{ formatNotificationTime(reminder.sendAt, timeZone) }}</p>
            <label class="form-field"><span>발송 날짜와 시간 · {{ timeZone }}</span><input v-model="localTime" class="input-field" type="datetime-local" required></label>
            <p class="message-muted">완료·삭제하면 대기 예약이 취소됩니다. 일정 변경 시 지정한 발송 시각은 유지됩니다. 서머타임 중복 시각은 먼저 오는 시각을 사용합니다.</p>
            <div class="page-actions">
              <CommonBaseButton v-if="reminder.enabled" type="button" variant="secondary" :disabled="busy || disabled" @click="cancel">예약 취소</CommonBaseButton>
              <CommonBaseButton type="submit" :disabled="busy || disabled || !localTime">{{ busy ? '처리 중…' : '알림 예약 저장' }}</CommonBaseButton>
            </div>
          </fieldset>
        </form>
      </template>
    </template>
  </section>
</template>

<style scoped>
h3, p { margin: 0; }
</style>
