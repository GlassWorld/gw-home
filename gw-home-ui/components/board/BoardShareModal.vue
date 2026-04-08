<script setup lang="ts">
import type { BoardShareSettings, SaveBoardSharePayload } from '~/types/api/board-share'
import { formatDateTime } from '~/utils/date'

const props = defineProps<{
  visible: boolean
  boardPostUuid: string
  shareSettings: BoardShareSettings | null
}>()

const emit = defineEmits<{
  close: []
  saved: [settings: BoardShareSettings]
}>()

const { showToast } = useToast()
const {
  deactivateBoardShare,
  reissueBoardShare,
  saveBoardShare
} = useBoardShare()

const shareEnabled = ref(true)
const expirationType = ref<'1' | '7' | '30' | 'custom'>('7')
const customExpiresAt = ref('')
const passwordEnabled = ref(false)
const password = ref('')
const isSubmitting = ref(false)
const isReissuing = ref(false)

const statusLabelMap: Record<BoardShareSettings['status'], string> = {
  INACTIVE: '공유 해제',
  SHARING: '공유중',
  EXPIRING_SOON: '만료 예정',
  EXPIRED: '만료됨',
  REVOKED: '공유 해제'
}

const shareLink = computed(() => {
  if (!props.shareSettings?.shareToken || !import.meta.client) {
    return ''
  }

  return `${window.location.origin}/share/boards/${props.shareSettings.shareToken}`
})

const currentStatusLabel = computed(() => {
  return props.shareSettings ? statusLabelMap[props.shareSettings.status] : '공유 해제'
})

const canReissue = computed(() => {
  return Boolean(props.shareSettings?.createdAt)
})

watch(
  () => [props.visible, props.shareSettings] as const,
  () => {
    if (!props.visible) {
      return
    }

    shareEnabled.value = props.shareSettings?.shareEnabled ?? false
    passwordEnabled.value = props.shareSettings?.passwordEnabled ?? false
    password.value = ''

    const currentExpiresAt = props.shareSettings?.expiresAt

    if (!currentExpiresAt) {
      expirationType.value = '7'
      customExpiresAt.value = ''
      return
    }

    const expiresAtDate = new Date(currentExpiresAt)
    const now = new Date()
    const diffDays = Math.round((expiresAtDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))

    if (diffDays === 1) {
      expirationType.value = '1'
      customExpiresAt.value = ''
      return
    }

    if (diffDays === 7) {
      expirationType.value = '7'
      customExpiresAt.value = ''
      return
    }

    if (diffDays === 30) {
      expirationType.value = '30'
      customExpiresAt.value = ''
      return
    }

    expirationType.value = 'custom'
    customExpiresAt.value = toDateTimeLocalValue(currentExpiresAt)
  },
  { immediate: true }
)

function toDateTimeLocalValue(value: string): string {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function resolvePayload(): SaveBoardSharePayload {
  const payload: SaveBoardSharePayload = {
    shareEnabled: shareEnabled.value,
    passwordEnabled: passwordEnabled.value,
    password: passwordEnabled.value ? password.value : undefined
  }

  if (!shareEnabled.value) {
    return payload
  }

  if (expirationType.value === 'custom') {
    payload.expiresAt = customExpiresAt.value ? new Date(customExpiresAt.value).toISOString() : undefined
    return payload
  }

  payload.expirationDays = Number(expirationType.value)
  return payload
}

async function handleSave() {
  if (isSubmitting.value) {
    return
  }

  if (shareEnabled.value && expirationType.value === 'custom' && !customExpiresAt.value) {
    showToast('직접 지정 만료 일시를 입력해 주세요.', { variant: 'error' })
    return
  }

  if (shareEnabled.value && passwordEnabled.value && !password.value.trim() && !props.shareSettings?.passwordEnabled) {
    showToast('비밀번호를 사용할 경우 값을 입력해 주세요.', { variant: 'error' })
    return
  }

  isSubmitting.value = true

  try {
    const nextSettings = shareEnabled.value
      ? await saveBoardShare(props.boardPostUuid, resolvePayload())
      : await deactivateBoardShare(props.boardPostUuid)

    emit('saved', nextSettings)
    showToast(shareEnabled.value ? '공유 설정이 저장되었습니다.' : '공유가 해제되었습니다.')
  } catch (error) {
    const fetchError = error as { data?: { message?: string }, message?: string }
    showToast(fetchError.data?.message ?? fetchError.message ?? '공유 설정 저장에 실패했습니다.', { variant: 'error' })
  } finally {
    isSubmitting.value = false
  }
}

async function handleReissue() {
  if (isReissuing.value || !canReissue.value) {
    return
  }

  isReissuing.value = true

  try {
    const nextSettings = await reissueBoardShare(props.boardPostUuid)
    emit('saved', nextSettings)
    showToast('공유 링크를 재발급했습니다.')
  } catch (error) {
    const fetchError = error as { data?: { message?: string }, message?: string }
    showToast(fetchError.data?.message ?? fetchError.message ?? '공유 링크 재발급에 실패했습니다.', { variant: 'error' })
  } finally {
    isReissuing.value = false
  }
}

async function handleCopyLink() {
  if (!shareLink.value) {
    showToast('복사할 공유 링크가 없습니다.', { variant: 'error' })
    return
  }

  try {
    await navigator.clipboard.writeText(shareLink.value)
    showToast('공유 링크를 복사했습니다.')
  } catch {
    showToast('공유 링크 복사에 실패했습니다.', { variant: 'error' })
  }
}
</script>

<template>
  <CommonBaseModal
    :visible="visible"
    title="게시글 외부 공유"
    eyebrow="Share"
    width="680px"
    @close="emit('close')"
  >
    <div class="board-share-modal">
      <section class="board-share-modal__section">
        <div class="board-share-modal__status-row">
          <div>
            <h3>현재 상태</h3>
            <p class="message-muted">이 링크는 로그인 없이 접근 가능한 읽기 전용 페이지입니다.</p>
          </div>
          <span class="board-share-modal__status-badge">{{ currentStatusLabel }}</span>
        </div>

        <label class="board-share-modal__toggle">
          <input v-model="shareEnabled" type="checkbox">
          <span>외부 공유 활성화</span>
        </label>
      </section>

      <section class="board-share-modal__section" :class="{ 'board-share-modal__section--disabled': !shareEnabled }">
        <h3>만료 기간</h3>
        <div class="board-share-modal__option-grid">
          <label><input v-model="expirationType" type="radio" value="1"> 1일</label>
          <label><input v-model="expirationType" type="radio" value="7"> 7일</label>
          <label><input v-model="expirationType" type="radio" value="30"> 30일</label>
          <label><input v-model="expirationType" type="radio" value="custom"> 직접 지정</label>
        </div>
        <input
          v-if="expirationType === 'custom'"
          v-model="customExpiresAt"
          class="input-field"
          type="datetime-local"
          :disabled="!shareEnabled"
        >
        <p class="message-muted">기본 만료 기간은 7일이며, 최대 30일까지 설정할 수 있습니다.</p>
      </section>

      <section class="board-share-modal__section" :class="{ 'board-share-modal__section--disabled': !shareEnabled }">
        <h3>비밀번호</h3>
        <label class="board-share-modal__toggle">
          <input v-model="passwordEnabled" type="checkbox" :disabled="!shareEnabled">
          <span>비밀번호 사용</span>
        </label>
        <input
          v-if="passwordEnabled"
          v-model="password"
          class="input-field"
          type="password"
          placeholder="4자 이상 비밀번호 입력"
          :disabled="!shareEnabled"
        >
        <p class="message-muted">설정한 기간이 지나면 자동으로 만료되며, 공유 해제 시 기존 링크는 즉시 사용할 수 없습니다.</p>
      </section>

      <section class="board-share-modal__section">
        <div class="board-share-modal__link-row">
          <div>
            <h3>공유 링크</h3>
            <p class="message-muted">
              공유 페이지는 읽기 전용이며 수정이나 댓글 작성은 지원하지 않습니다.
            </p>
          </div>
          <CommonBaseButton
            variant="secondary"
            size="small"
            :disabled="!shareLink"
            @click="handleCopyLink"
          >
            링크 복사
          </CommonBaseButton>
        </div>

        <div class="board-share-modal__link-box">
          <code>{{ shareLink || '공유 링크를 생성하면 여기에 표시됩니다.' }}</code>
        </div>

        <p v-if="shareSettings?.expiresAt" class="message-muted">
          만료 시각: {{ formatDateTime(shareSettings.expiresAt) }}
        </p>
      </section>
    </div>

    <template #actions>
      <CommonBaseButton variant="secondary" type="button" @click="emit('close')">
        닫기
      </CommonBaseButton>
      <CommonBaseButton
        v-if="canReissue"
        variant="secondary"
        type="button"
        :disabled="isReissuing"
        @click="handleReissue"
      >
        {{ isReissuing ? '재발급 중...' : '재발급' }}
      </CommonBaseButton>
      <CommonBaseButton type="button" :disabled="isSubmitting" @click="handleSave">
        {{ isSubmitting ? '저장 중...' : '저장' }}
      </CommonBaseButton>
    </template>
  </CommonBaseModal>
</template>

<style scoped>
.board-share-modal {
  display: grid;
  gap: 16px;
}

.board-share-modal__section {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.18);
}

.board-share-modal__section--disabled {
  opacity: 0.68;
}

.board-share-modal__section h3 {
  margin: 0;
}

.board-share-modal__status-row,
.board-share-modal__link-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.board-share-modal__status-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.16);
  color: #7dd3fc;
  font-size: 0.85rem;
  font-weight: 700;
}

.board-share-modal__toggle {
  display: flex;
  align-items: center;
  gap: 10px;
}

.board-share-modal__option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.board-share-modal__option-grid label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.board-share-modal__link-box {
  padding: 14px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.36);
  overflow-x: auto;
}

.board-share-modal__link-box code {
  white-space: nowrap;
}

@media (max-width: 768px) {
  .board-share-modal__status-row,
  .board-share-modal__link-row {
    flex-direction: column;
  }

  .board-share-modal__option-grid {
    grid-template-columns: 1fr;
  }
}
</style>
