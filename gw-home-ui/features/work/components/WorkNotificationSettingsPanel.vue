<script setup lang="ts">
import { useWorkNotificationSettings } from '../composables/use-work-notification-settings'
import { formatNotificationTime } from '../utils/notification-time'
import WorkNotificationHistory from './WorkNotificationHistory.vue'

const { saved, form, code, busy, loading, error, verificationMessage, verified, canActivate, timeZones,
  history, historyError, historyLoading, load, save, requestCode, verify, loadHistory } = useWorkNotificationSettings()
const zoneOptions = computed(() => [...new Set([form.timeZone, ...timeZones.value])].map(value => ({ value, label: value })))
</script>

<template>
  <section id="work-notifications" class="content-panel page-panel-padding-lg page-stack">
    <div>
      <h2>내 할 일 메일 알림</h2>
      <p class="message-muted">앱을 닫아도 원하는 시각에 오늘 할 일과 개별 알림을 받아보세요.</p>
    </div>
    <CommonBaseState v-if="loading" state="loading" message="메일 알림 설정을 불러오고 있습니다." />
    <CommonBaseState v-else-if="!saved" state="error" :message="error" action-label="다시 불러오기" @action="load" />
    <template v-else>
      <CommonBaseState v-if="!saved.serverReady" state="empty" message="메일 발송 준비 중입니다. 주소와 시간대를 먼저 저장할 수 있습니다." />
      <form class="page-stack" @submit.prevent="save">
        <fieldset class="notification-fields page-stack" :disabled="busy">
          <label class="form-field">
            <span>내 수신 이메일 <span class="message-muted">· {{ verified ? '확인 완료' : '확인 필요' }}</span></span>
            <input v-model.trim="form.email" class="input-field" type="email" autocomplete="email" maxlength="254" required>
          </label>
          <p class="message-muted">주소를 바꾸면 기존 알림 예약이 취소됩니다. 저장 후 새 주소를 확인하고 알림을 다시 켜주세요.</p>
          <div v-if="!verified" class="page-stack">
            <div class="page-actions">
              <CommonBaseButton type="button" variant="secondary" :disabled="!saved.serverReady || !saved.email || form.email !== saved.email" @click="requestCode">확인 코드 받기</CommonBaseButton>
            </div>
            <div class="notification-code">
              <label class="form-field">
                <span>이메일 확인 코드</span>
                <input v-model="code" class="input-field" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="6자리 숫자">
              </label>
              <CommonBaseButton type="button" variant="secondary" :disabled="code.length !== 6 || form.email !== saved.email" @click="verify">주소 확인</CommonBaseButton>
            </div>
          </div>
          <p v-if="verificationMessage" role="status" class="message-muted">{{ verificationMessage }}</p>
          <div class="form-field">
            <span>발송 시간대</span>
            <CommonSearchableSelect v-model="form.timeZone" :options="zoneOptions" label="발송 시간대" input-class="input-field" />
          </div>
          <div class="page-info-card">
            <label class="notification-toggle">
              <input v-model="form.digestEnabled" type="checkbox" :disabled="!canActivate && !form.digestEnabled">
              <span><strong>정기 요약</strong><small>오늘 할 일 · 3일 이내 마감 · 지연된 일</small></span>
            </label>
            <div class="notification-grid">
              <label class="form-field"><span>요약 주기</span><select v-model="form.digestCycle" class="input-field"><option value="DAILY">매일</option><option value="WEEKDAYS">평일 (월–금)</option></select></label>
              <label class="form-field"><span>발송 시각</span><input v-model="form.digestTime" class="input-field" type="time" required></label>
            </div>
            <p class="message-muted">저장된 다음 발송: {{ formatNotificationTime(saved.nextDigestAt, saved.timeZone) }} · {{ saved.timeZone }}<br>대상 할 일이 없으면 요약 메일을 보내지 않습니다.</p>
          </div>
          <div class="page-info-card">
            <label class="notification-toggle">
              <input v-model="form.reminderEnabled" type="checkbox" :disabled="!canActivate && !form.reminderEnabled">
              <span><strong>할 일별 개별 알림</strong><small>할 일 상세에서 날짜와 시간을 지정하세요.</small></span>
            </label>
            <p class="message-muted">끄고 저장하면 대기 중인 개별 예약이 취소됩니다. 다시 켠 뒤 필요한 할 일에 새로 예약하세요.</p>
          </div>
          <p v-if="!verified" class="message-muted">수신 주소를 확인하면 두 종류의 알림을 켤 수 있습니다.</p>
          <CommonBaseState v-if="error" state="error" :message="error" />
          <div class="page-actions"><CommonBaseButton type="submit" :disabled="busy">{{ busy ? '처리 중…' : '메일 알림 설정 저장' }}</CommonBaseButton></div>
        </fieldset>
      </form>
      <WorkNotificationHistory :history="history" :loading="historyLoading" :error="historyError" :time-zone="saved.timeZone" @load="loadHistory" />
    </template>
  </section>
</template>

<style scoped>
h2, p { margin: 0; }
.notification-code { display: flex; gap: 12px; align-items: end; flex-wrap: wrap; }
.notification-code .form-field { flex: 1 1 180px; }
</style>
