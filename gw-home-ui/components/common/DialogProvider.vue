<script setup lang="ts">
const { dialogState, closeDialog } = useDialog()
</script>

<template>
  <CommonBaseModal
    :visible="dialogState.visible"
    eyebrow="Dialog"
    :title="dialogState.title"
    width="420px"
    @close="closeDialog(false)"
  >
    <p class="dialog-provider__message">
      {{ dialogState.message }}
    </p>

    <template #actions>
      <CommonBaseButton
        v-if="dialogState.kind === 'confirm'"
        variant="secondary"
        @click="closeDialog(false)"
      >
        {{ dialogState.cancelText }}
      </CommonBaseButton>
      <CommonBaseButton @click="closeDialog(true)">
        {{ dialogState.confirmText }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.dialog-provider__message {
  margin: 0;
  color: var(--color-text);
  line-height: 1.6;
  word-break: break-word;
}
</style>
