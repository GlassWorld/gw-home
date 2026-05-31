<script setup lang="ts">
defineProps<{
  errorMessage: string
  isLoading: boolean
  isSubmitting: boolean
  linked: boolean
  googleEmail: string | null
  linkedAt: string | null
}>()

const emit = defineEmits<{
  link: []
  unlink: []
}>()

function formatLinkedAt(linkedAt: string | null): string {
  if (!linkedAt) {
    return ''
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(linkedAt))
}
</script>

<template>
  <section class="settings-section content-panel page-panel-padding-lg">
    <div class="page-section-header">
      <div>
        <h2>Google 계정 연동</h2>
        <p class="message-muted">연동된 Google 계정으로 로그인할 수 있습니다.</p>
      </div>
    </div>

    <p v-if="errorMessage" class="message-error">
      {{ errorMessage }}
    </p>

    <p v-else-if="isLoading" class="message-muted">
      Google 계정 연동 상태를 확인하고 있습니다.
    </p>

    <div v-else class="settings-google-section__card">
      <div>
        <h3>{{ linked ? 'Google 계정 연동됨' : 'Google 계정 미연동' }}</h3>
        <p class="message-muted">
          <template v-if="linked">
            {{ googleEmail }}<span v-if="linkedAt"> · {{ formatLinkedAt(linkedAt) }}</span>
          </template>
          <template v-else>
            현재 계정에 Google 계정을 연결하면 로그인 화면에서 Google 로그인을 사용할 수 있습니다.
          </template>
        </p>
      </div>

      <CommonBaseButton
        :variant="linked ? 'secondary' : 'primary'"
        :disabled="isSubmitting"
        @click="linked ? emit('unlink') : emit('link')"
      >
        {{ isSubmitting ? '처리 중...' : linked ? '연동 해제' : 'Google 연동' }}
      </CommonBaseButton>
    </div>
  </section>
</template>

<style scoped>
.settings-google-section__card {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 22px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
}

.settings-google-section__card h3 {
  margin: 0;
}

@media (max-width: 900px) {
  .settings-google-section__card {
    grid-template-columns: 1fr;
    align-items: stretch;
  }
}
</style>
