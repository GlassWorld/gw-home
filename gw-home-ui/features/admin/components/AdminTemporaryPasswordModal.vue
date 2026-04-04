<script setup lang="ts">
const props = defineProps<{
  visible: boolean
  temporaryPassword: string
  temporaryPasswordTargetLoginId: string
}>()

const emit = defineEmits<{
  copy: []
  close: []
}>()
</script>

<template>
  <CommonBaseModal
    :visible="props.visible"
    eyebrow="Admin"
    title="임시 비밀번호 발급"
    width="560px"
    @close="emit('close')"
  >
    <div class="admin-temporary-password-modal">
      <p class="message-muted">
        아래 임시 비밀번호를 <strong>{{ props.temporaryPasswordTargetLoginId }}</strong> 계정 사용자에게 직접 전달해 주세요.
      </p>
      <code class="admin-temporary-password-modal__code">{{ props.temporaryPassword }}</code>
    </div>

    <template #actions>
      <CommonBaseButton variant="secondary" @click="emit('copy')">
        복사
      </CommonBaseButton>
      <CommonBaseButton @click="emit('close')">
        확인
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.admin-temporary-password-modal {
  display: grid;
  gap: 14px;
}

.admin-temporary-password-modal__code {
  display: block;
  padding: 16px;
  border-radius: 16px;
  background: rgba(7, 18, 32, 0.88);
  color: rgba(232, 244, 255, 0.96);
  font-size: 1.05rem;
  word-break: break-all;
}
</style>
