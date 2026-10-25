<script setup lang="ts">
import { useId, type CSSProperties } from 'vue'

interface SearchableSelectOption { value: string; label: string }
const props = withDefaults(defineProps<{
  options: SearchableSelectOption[]
  modelValue: string
  placeholder?: string
  inputClass?: string
  teleportToBody?: boolean
  dropdownZIndex?: number
  label?: string
}>(), { placeholder: '선택하세요', teleportToBody: true, dropdownZIndex: 1100 })
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const rootElement = ref<HTMLElement | null>(null)
const inputElement = ref<HTMLInputElement | null>(null)
const dropdownElement = ref<HTMLElement | null>(null)
const searchKeyword = ref('')
const isOpen = ref(false)
const highlightedIndex = ref(-1)
const listId = `select-options-${useId()}`
const dropdownStyle = ref<CSSProperties>({})
const filteredOptions = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return keyword ? props.options.filter(option => option.label.toLowerCase().includes(keyword)) : props.options
})
const selectedOption = computed(() => props.options.find(option => option.value === props.modelValue))
const inputValue = computed(() => isOpen.value ? searchKeyword.value : selectedOption.value?.label ?? '')
const activeOptionId = computed(() => isOpen.value && filteredOptions.value[highlightedIndex.value] ? `${listId}-${highlightedIndex.value}` : undefined)

// 가상 키보드와 화면 가장자리를 고려해 목록을 위 또는 아래에 배치한다.
function updateDropdownPosition() {
  if (!inputElement.value || !isOpen.value) return
  const rectangle = inputElement.value.getBoundingClientRect()
  const viewport = window.visualViewport
  const top = viewport?.offsetTop ?? 0
  const height = viewport?.height ?? window.innerHeight
  const left = viewport?.offsetLeft ?? 0
  const width = viewport?.width ?? document.documentElement.clientWidth
  const spaceBelow = Math.max(0, top + height - rectangle.bottom - 12)
  const spaceAbove = Math.max(0, rectangle.top - top - 12)
  const opensAbove = spaceBelow < 180 && spaceAbove > spaceBelow
  const maximumHeight = Math.min(256, opensAbove ? spaceAbove : spaceBelow)
  dropdownStyle.value = {
    position: 'fixed', zIndex: props.dropdownZIndex,
    width: `${Math.min(rectangle.width, width - 24)}px`,
    left: `${Math.max(left + 12, Math.min(rectangle.left, left + width - rectangle.width - 12))}px`,
    top: `${opensAbove ? rectangle.top - 6 : rectangle.bottom + 6}px`,
    transform: opensAbove ? 'translateY(-100%)' : undefined,
    maxHeight: `${maximumHeight}px`
  }
}

// 선택한 항목이 키보드 탐색 중에도 목록 안에 보이게 한다.
async function revealHighlightedOption() {
  await nextTick()
  const option = dropdownElement.value?.querySelector<HTMLElement>(`[id="${listId}-${highlightedIndex.value}"]`)
  if (!option || !dropdownElement.value) return
  const viewport = dropdownElement.value
  if (option.offsetTop < viewport.scrollTop) viewport.scrollTop = option.offsetTop
  else if (option.offsetTop + option.offsetHeight > viewport.scrollTop + viewport.clientHeight) viewport.scrollTop = option.offsetTop + option.offsetHeight - viewport.clientHeight
}

// 검색 상태를 초기화하고 선택 목록을 연다.
function openDropdown() {
  if (isOpen.value) return
  isOpen.value = true
  searchKeyword.value = ''
  highlightedIndex.value = Math.max(0, filteredOptions.value.findIndex(option => option.value === props.modelValue))
  updateDropdownPosition()
  void revealHighlightedOption()
}

// 검색을 종료하고 현재 선택 값을 다시 표시한다.
function closeDropdown() {
  isOpen.value = false
  searchKeyword.value = ''
  highlightedIndex.value = -1
}

// 입력 필드의 포커스를 유지한 채 선택 값을 전달한다.
function selectOption(option: SearchableSelectOption) {
  emit('update:modelValue', option.value)
  closeDropdown()
}

// 닫힌 상태에서 입력한 첫 글자도 검색어로 유지한다.
function handleInput(event: Event) {
  if (!isOpen.value) openDropdown()
  searchKeyword.value = (event.target as HTMLInputElement).value
  highlightedIndex.value = 0
  updateDropdownPosition()
}

// 콤보박스의 방향키·선택·닫기 동작을 처리한다.
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Tab') { closeDropdown(); return }
  if (event.key === 'Escape' && isOpen.value) {
    event.preventDefault()
    event.stopPropagation()
    closeDropdown()
    return
  }
  if (!isOpen.value && ['ArrowDown', 'ArrowUp', 'Enter'].includes(event.key)) {
    event.preventDefault()
    openDropdown()
    return
  }
  if (!isOpen.value) return
  if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key) && filteredOptions.value.length) {
    event.preventDefault()
    const count = filteredOptions.value.length
    if (event.key === 'Home') highlightedIndex.value = 0
    else if (event.key === 'End') highlightedIndex.value = count - 1
    else highlightedIndex.value = (highlightedIndex.value + (event.key === 'ArrowDown' ? 1 : -1) + count) % count
    void revealHighlightedOption()
  } else if (event.key === 'Enter') {
    event.preventDefault()
    const option = filteredOptions.value[highlightedIndex.value]
    if (option) selectOption(option)
  }
}

// 필드와 포털 목록 외부를 누르면 목록을 닫는다.
function handleOutsidePointer(event: PointerEvent) {
  const target = event.target as Node
  if (!rootElement.value?.contains(target) && !dropdownElement.value?.contains(target)) closeDropdown()
}

watch(() => props.options, () => {
  highlightedIndex.value = filteredOptions.value.length ? 0 : -1
  updateDropdownPosition()
})
onMounted(() => {
  document.addEventListener('pointerdown', handleOutsidePointer)
  window.addEventListener('scroll', updateDropdownPosition, true)
  window.addEventListener('resize', updateDropdownPosition)
  window.visualViewport?.addEventListener('resize', updateDropdownPosition)
  window.visualViewport?.addEventListener('scroll', updateDropdownPosition)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleOutsidePointer)
  window.removeEventListener('scroll', updateDropdownPosition, true)
  window.removeEventListener('resize', updateDropdownPosition)
  window.visualViewport?.removeEventListener('resize', updateDropdownPosition)
  window.visualViewport?.removeEventListener('scroll', updateDropdownPosition)
})
</script>

<template>
  <div ref="rootElement" class="searchable-select" :class="{ 'searchable-select--open': isOpen }">
    <input
      ref="inputElement" :value="inputValue" :class="['input-field', 'searchable-select__input', inputClass]"
      type="text" :placeholder="placeholder" :aria-label="label || placeholder" autocomplete="off"
      role="combobox" aria-autocomplete="list" aria-haspopup="listbox" :aria-expanded="isOpen"
      :aria-controls="isOpen ? listId : undefined" :aria-activedescendant="activeOptionId"
      @focus="openDropdown" @click="openDropdown" @input="handleInput" @keydown="handleKeydown" @blur="closeDropdown"
    >
    <span class="searchable-select__indicator" aria-hidden="true">⌄</span>
    <Teleport to="body" :disabled="!teleportToBody">
      <Transition name="popover" @before-leave="disableLeavingElement">
        <div v-if="isOpen" :id="listId" ref="dropdownElement" class="searchable-select__dropdown" role="listbox" :aria-label="label || placeholder" :style="dropdownStyle">
          <button
            v-for="(option, optionIndex) in filteredOptions" :id="`${listId}-${optionIndex}`" :key="option.value"
            class="searchable-select__option" :class="{ 'searchable-select__option--active': optionIndex === highlightedIndex }"
            type="button" role="option" tabindex="-1" :aria-selected="option.value === modelValue"
            @mousedown.prevent @mouseenter="highlightedIndex = optionIndex" @click="selectOption(option)"
          >{{ option.label }}</button>
          <p v-if="!filteredOptions.length" class="searchable-select__empty" role="status">검색 결과가 없습니다.</p>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.searchable-select { position: relative; width: 100%; min-width: 0; }
.searchable-select__input { padding-right: 40px; }
.searchable-select__indicator { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); color: var(--color-text-muted); pointer-events: none; }
.searchable-select__dropdown { display: grid; gap: 4px; padding: 6px; overflow-y: auto; overscroll-behavior: contain; border: 1px solid var(--color-border-strong); border-radius: var(--radius-small); background: var(--color-surface); box-shadow: var(--shadow-raised); }
.searchable-select__option { display: block; width: 100%; min-height: 44px; padding: 10px 12px; border: 0; border-radius: 6px; background: transparent; color: var(--color-text); text-align: left; overflow-wrap: anywhere; }
.searchable-select__option--active, .searchable-select__option[aria-selected="true"] { background: var(--color-primary-soft); color: var(--color-primary); }
.searchable-select__empty { margin: 0; padding: 12px; color: var(--color-text-muted); }
</style>
