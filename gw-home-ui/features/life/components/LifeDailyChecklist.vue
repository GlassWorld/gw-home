<script setup lang="ts">
import LifeDailyCalendar from './LifeDailyCalendar.vue'
import { useLifeDaily } from '../composables/use-life-daily'
const { today, selectedDate, month, result, days, selectedItems, completedCount, isCompleted, isLoading, isSaving, busy,
  errorMessage, saveError, title, editingItem, editingTitle, deleteTarget, refresh, moveMonth, goToday, toggle, saveItem, deleteItem, openEdit } = useLifeDaily()
</script>

<template>
  <section class="content-panel page-panel-padding-md daily-checklist" aria-label="매일 체크">
    <div class="page-section-header"><div><h2>매일 체크</h2><p class="section-description">항목을 직접 등록하고, 한 일을 체크하세요.</p></div>
    </div>
    <form class="daily-checklist__add" @submit.prevent="saveItem()">
      <label for="daily-title">매일 할 일<input id="daily-title" v-model="title" class="input-field" maxlength="200" placeholder="예: 벤치프레스, 스쿼트" :disabled="busy" required></label>
      <CommonBaseButton type="submit" :disabled="busy || !title.trim()">추가</CommonBaseButton>
    </form>
    <p class="section-description">새 항목은 오늘부터 매일 표시됩니다. {{ result?.timeZone || '기기 시간대' }} 기준</p>
    <CommonBaseState v-if="saveError && !editingItem && !deleteTarget" state="error" :message="saveError" />
    <div class="daily-checklist__layout">
      <LifeDailyCalendar :month="month" :selected-date="selectedDate" :today="today" :days="days" :disabled="isSaving" :loading="isLoading"
        @move="moveMonth" @today="goToday" @select="selectedDate = $event" />
      <section class="daily-checklist__items" aria-label="선택 날짜 체크리스트" :aria-busy="busy">
        <h3>{{ selectedDate }} <span v-if="selectedDate === today">오늘</span></h3>
        <CommonBaseState v-if="isLoading" state="loading" message="체크 기록을 불러오는 중입니다." />
        <CommonBaseState v-else-if="errorMessage" state="error" :message="errorMessage" action-label="다시 불러오기" @action="refresh" />
        <template v-else>
          <p v-if="selectedDate > today" class="section-description">미래 날짜는 조회만 가능합니다.</p>
          <p v-else class="daily-checklist__progress" aria-live="polite">{{ selectedItems.length }}개 중 {{ completedCount }}개 완료</p>
          <CommonBaseState v-if="!selectedItems.length" state="empty" title="이 날짜에 할 일이 없습니다." message="매일 할 일을 추가하면 오늘부터 체크할 수 있습니다." />
          <ul v-else>
            <li v-for="item in selectedItems" :key="item.itemUuid">
              <button class="daily-checklist__check" type="button" role="checkbox" :aria-checked="isCompleted(item.itemUuid)"
                :aria-label="item.title" :disabled="busy || selectedDate > today" @click="toggle(item)">
                <span class="daily-checklist__box" aria-hidden="true">{{ isCompleted(item.itemUuid) ? '✓' : '' }}</span>
                <span :class="{ 'daily-checklist__completed': isCompleted(item.itemUuid) }">{{ item.title }}</span>
              </button>
              <div class="daily-checklist__item-actions">
                <template v-if="!item.endDate">
                  <CommonBaseButton variant="secondary" size="small" :disabled="busy" :aria-label="`${item.title} 제목 수정`" @click="openEdit(item)">수정</CommonBaseButton>
                </template>
                  <CommonBaseButton variant="secondary" size="small" :disabled="busy" :aria-label="`${item.title} 목록에서 삭제`" @click="deleteTarget = item; saveError = ''">삭제</CommonBaseButton>
              </div>
            </li>
          </ul>
        </template>
      </section>
    </div>
    <CommonBaseModal :visible="!!editingItem" title="매일 할 일 수정" width="480px" :dismissible="!isSaving" @close="editingItem = null; saveError = ''">
      <form id="daily-edit" @submit.prevent="saveItem()"><label>제목<input v-model="editingTitle" class="input-field" maxlength="200" required :disabled="isSaving"></label></form>
      <CommonBaseState v-if="saveError" state="error" :message="saveError" />
      <template #actions><CommonBaseButton variant="secondary" :disabled="isSaving" @click="editingItem = null; saveError = ''">취소</CommonBaseButton><CommonBaseButton type="submit" form="daily-edit" :loading="isSaving">저장</CommonBaseButton></template>
    </CommonBaseModal>
    <CommonBaseModal :visible="!!deleteTarget" title="체크 목록에서 삭제" width="480px" :dismissible="!isSaving" @close="deleteTarget = null; saveError = ''">
      <p class="daily-checklist__end">‘{{ deleteTarget?.title }}’을 삭제할까요? 모든 날짜의 목록과 체크 기록에서 사라집니다.</p>
      <CommonBaseState v-if="saveError" state="error" :message="saveError" />
      <template #actions><CommonBaseButton variant="secondary" :disabled="isSaving" @click="deleteTarget = null; saveError = ''">취소</CommonBaseButton><CommonBaseButton variant="danger" :loading="isSaving" @click="deleteItem">삭제</CommonBaseButton></template>
    </CommonBaseModal>
  </section>
</template>

<style scoped>
.daily-checklist__add { display: flex; align-items: end; gap: 8px; }
.daily-checklist__add label { display: grid; gap: 8px; flex: 1; min-width: 0; }
.daily-checklist .input-field { width: 100%; }
.daily-checklist__layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 24px; margin-top: 20px; }
.daily-checklist__items { min-width: 0; }
.daily-checklist__items h3 { margin: 0; font-size: 1.05rem; }
.daily-checklist__items h3 span, .daily-checklist__progress { color: var(--color-accent); }
.daily-checklist__items ul { list-style: none; padding: 0; margin: 0; }
.daily-checklist__items li { display: flex; align-items: center; gap: 8px; padding: 2px 0; border-bottom: 1px solid var(--color-border); }
.daily-checklist__check { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; min-height: 44px; padding: 4px 0; border: 0; background: transparent; text-align: left; color: var(--color-text); font: inherit; cursor: pointer; overflow-wrap: anywhere; }
.daily-checklist__check:disabled { cursor: default; }
.daily-checklist__box { display: grid; place-items: center; flex: 0 0 24px; width: 24px; height: 24px; border: 1px solid var(--color-accent); border-radius: var(--radius-small); color: var(--color-accent); }
.daily-checklist__completed { text-decoration: line-through; color: var(--color-text-muted); }
.daily-checklist__item-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.daily-checklist__item-actions :deep(button) { min-height: 36px; }
.daily-checklist__end { overflow-wrap: anywhere; }
@media (max-width: 767px) { .daily-checklist__layout { grid-template-columns: minmax(0, 1fr); gap: 16px; } }
</style>
