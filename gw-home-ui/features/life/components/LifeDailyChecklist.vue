<script setup lang="ts">
import LifeDailyCalendar from './LifeDailyCalendar.vue'
import { useLifeDaily } from '../composables/use-life-daily'
const { today, selectedDate, month, result, days, selectedItems, completedCount, isCompleted, isLoading, isSaving, busy,
  errorMessage, saveError, title, editingItem, editingTitle, endTarget, refresh, moveMonth, goToday, toggle, saveItem, endItem, openEdit } = useLifeDaily()
</script>

<template>
  <section class="content-panel page-panel-padding-md daily-checklist" aria-label="매일 체크">
    <div class="page-section-header"><div><h2>매일 체크</h2><p class="section-description">작은 일도 하루에 한 번, 했으면 체크하세요.</p></div></div>
    <form class="daily-checklist__add" @submit.prevent="saveItem">
      <label for="daily-title">매일 할 일<input id="daily-title" v-model="title" class="input-field" maxlength="200" placeholder="예: 물 마시기, 산책 20분" :disabled="busy" required></label>
      <CommonBaseButton type="submit" :disabled="busy || !title.trim()">추가</CommonBaseButton>
    </form>
    <p class="section-description">새 항목은 오늘부터 매일 표시됩니다. {{ result?.timeZone || '기기 시간대' }} 기준</p>
    <CommonBaseState v-if="saveError && !editingItem && !endTarget" state="error" :message="saveError" />
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
                <small>{{ item.endDate ? `${item.endDate}까지 · 종료` : isCompleted(item.itemUuid) ? '완료' : '미완료' }}</small>
                <template v-if="!item.endDate">
                  <CommonBaseButton variant="secondary" size="small" :disabled="busy" :aria-label="`${item.title} 제목 수정`" @click="openEdit(item)">수정</CommonBaseButton>
                  <CommonBaseButton variant="secondary" size="small" :disabled="busy" :aria-label="`${item.title} 반복 종료`" @click="endTarget = item; saveError = ''">종료</CommonBaseButton>
                </template>
              </div>
            </li>
          </ul>
        </template>
      </section>
    </div>
    <CommonBaseModal :visible="!!editingItem" title="매일 할 일 수정" width="480px" :dismissible="!isSaving" @close="editingItem = null; saveError = ''">
      <form id="daily-edit" @submit.prevent="saveItem"><label>제목<input v-model="editingTitle" class="input-field" maxlength="200" required :disabled="isSaving"></label></form>
      <CommonBaseState v-if="saveError" state="error" :message="saveError" />
      <template #actions><CommonBaseButton variant="secondary" :disabled="isSaving" @click="editingItem = null; saveError = ''">취소</CommonBaseButton><CommonBaseButton type="submit" form="daily-edit" :loading="isSaving">저장</CommonBaseButton></template>
    </CommonBaseModal>
    <CommonBaseModal :visible="!!endTarget" title="매일 반복 종료" width="480px" :dismissible="!isSaving" @close="endTarget = null; saveError = ''">
      <p class="daily-checklist__end">‘{{ endTarget?.title }}’을 내일부터 표시하지 않습니다. 오늘과 과거 체크 기록은 유지됩니다.</p>
      <CommonBaseState v-if="saveError" state="error" :message="saveError" />
      <template #actions><CommonBaseButton variant="secondary" :disabled="isSaving" @click="endTarget = null; saveError = ''">취소</CommonBaseButton><CommonBaseButton variant="danger" :loading="isSaving" @click="endItem">반복 종료</CommonBaseButton></template>
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
.daily-checklist__items li { padding: 12px 0; border-bottom: 1px solid var(--color-border); }
.daily-checklist__check { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 44px; padding: 4px 0; border: 0; background: transparent; text-align: left; color: var(--color-text); font: inherit; cursor: pointer; overflow-wrap: anywhere; }
.daily-checklist__check:disabled { cursor: default; }
.daily-checklist__box { display: grid; place-items: center; flex: 0 0 24px; width: 24px; height: 24px; border: 1px solid var(--color-accent); border-radius: var(--radius-small); color: var(--color-accent); }
.daily-checklist__completed { text-decoration: line-through; color: var(--color-text-muted); }
.daily-checklist__item-actions { display: flex; align-items: center; gap: 8px; padding-left: 36px; }
.daily-checklist__item-actions small { flex: 1; color: var(--color-text-muted); }
.daily-checklist__end { overflow-wrap: anywhere; }
@media (max-width: 767px) { .daily-checklist__layout { grid-template-columns: minmax(0, 1fr); gap: 16px; } }
</style>
