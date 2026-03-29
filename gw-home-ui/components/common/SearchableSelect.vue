<script setup lang="ts">
import type { CSSProperties } from 'vue'

interface SearchableSelectOption {
  value: string
  label: string
}

const props = withDefaults(defineProps<{
  options: SearchableSelectOption[]
  modelValue: string
  placeholder?: string
  inputClass?: string
  teleportToBody?: boolean
  dropdownZIndex?: number
}>(), {
  placeholder: '선택하세요',
  teleportToBody: true,
  dropdownZIndex: 1100
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

interface DropdownPosition {
  top: number
  left: number
  width: number
}

const rootElement = ref<HTMLElement | null>(null)
const inputElement = ref<HTMLInputElement | null>(null)
const dropdownElement = ref<HTMLElement | null>(null)
const searchKeyword = ref('')
const isOpen = ref(false)
const highlightedIndex = ref(-1)
const dropdownPosition = ref<DropdownPosition>({ top: 0, left: 0, width: 0 })

const filteredOptions = computed(() => {
  const normalizedKeyword = searchKeyword.value.trim().toLowerCase()
  if (!normalizedKeyword) {
    return props.options
  }

  return props.options.filter((option) => option.label.toLowerCase().includes(normalizedKeyword))
})

const selectedOption = computed(() => {
  return props.options.find((option) => option.value === props.modelValue) ?? null
})

const inputValue = computed(() => {
  if (isOpen.value) {
    return searchKeyword.value
  }

  return selectedOption.value?.label ?? ''
})

const portalDropdownStyle = computed<CSSProperties>(() => ({
  top: `${dropdownPosition.value.top}px`,
  left: `${dropdownPosition.value.left}px`,
  width: `${dropdownPosition.value.width}px`,
  zIndex: props.dropdownZIndex,
  position: 'fixed',
  maxHeight: '240px'
}))

function updateDropdownPosition() {
  if (!inputElement.value) {
    return
  }

  const rect = inputElement.value.getBoundingClientRect()
  dropdownPosition.value = {
    top: rect.bottom + 8,
    left: rect.left,
    width: rect.width
  }
}

function openDropdown() {
  isOpen.value = true
  searchKeyword.value = ''
  highlightedIndex.value = filteredOptions.value.findIndex((option) => option.value === props.modelValue)
  if (highlightedIndex.value === -1 && filteredOptions.value.length > 0) {
    highlightedIndex.value = 0
  }
  if (props.teleportToBody) {
    updateDropdownPosition()
  }
}

function closeDropdown() {
  isOpen.value = false
  searchKeyword.value = ''
  highlightedIndex.value = -1
}

function selectOption(option: SearchableSelectOption) {
  emit('update:modelValue', option.value)
  closeDropdown()
}

function handleInput(event: Event) {
  searchKeyword.value = (event.target as HTMLInputElement).value
  if (!isOpen.value) {
    openDropdown()
  }
  highlightedIndex.value = filteredOptions.value.length > 0 ? 0 : -1
}

function handleKeydown(event: KeyboardEvent) {
  if (!isOpen.value && ['ArrowDown', 'ArrowUp', 'Enter'].includes(event.key)) {
    openDropdown()
    return
  }

  if (event.key === 'Escape') {
    closeDropdown()
    return
  }

  if (!isOpen.value || filteredOptions.value.length === 0) {
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    highlightedIndex.value = (highlightedIndex.value + 1) % filteredOptions.value.length
    return
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    highlightedIndex.value = highlightedIndex.value <= 0 ? filteredOptions.value.length - 1 : highlightedIndex.value - 1
    return
  }

  if (event.key === 'Enter' && highlightedIndex.value >= 0) {
    event.preventDefault()
    const highlightedOption = filteredOptions.value[highlightedIndex.value]
    if (highlightedOption) {
      selectOption(highlightedOption)
    }
  }
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node
  if (rootElement.value?.contains(target) || dropdownElement.value?.contains(target)) {
    return
  }

  closeDropdown()
}

function handleWindowUpdate() {
  if (isOpen.value && props.teleportToBody) {
    updateDropdownPosition()
  }
}

watch(isOpen, (nextOpen) => {
  if (!nextOpen) {
    window.removeEventListener('scroll', handleWindowUpdate, true)
    window.removeEventListener('resize', handleWindowUpdate)
    return
  }

  if (props.teleportToBody) {
    updateDropdownPosition()
    window.addEventListener('scroll', handleWindowUpdate, true)
    window.addEventListener('resize', handleWindowUpdate)
  }
})

watch(
  () => props.modelValue,
  () => {
    if (!isOpen.value) {
      searchKeyword.value = ''
    }
  }
)

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentClick)
  window.removeEventListener('scroll', handleWindowUpdate, true)
  window.removeEventListener('resize', handleWindowUpdate)
})
</script>

<template>
  <div ref="rootElement" class="searchable-select">
    <input
      ref="inputElement"
      :value="inputValue"
      :class="['searchable-select__input', 'input-field', 'vault-modal__select', inputClass]"
      type="text"
      :placeholder="placeholder"
      autocomplete="off"
      role="combobox"
      :aria-expanded="isOpen"
      aria-autocomplete="list"
      @focus="openDropdown"
      @input="handleInput"
      @keydown="handleKeydown"
    >

    <div
      v-if="isOpen && !teleportToBody"
      ref="dropdownElement"
      class="searchable-select__dropdown"
      role="listbox"
    >
      <button
        v-for="(option, optionIndex) in filteredOptions"
        :key="option.value || 'empty-option'"
        class="searchable-select__option"
        :class="{ 'searchable-select__option--active': optionIndex === highlightedIndex }"
        type="button"
        role="option"
        :aria-selected="option.value === modelValue"
        @mouseenter="highlightedIndex = optionIndex"
        @click="selectOption(option)"
      >
        {{ option.label }}
      </button>
      <p v-if="filteredOptions.length === 0" class="searchable-select__empty">
        검색 결과가 없습니다.
      </p>
    </div>

    <teleport to="body">
      <div
        v-if="isOpen && teleportToBody"
        ref="dropdownElement"
        class="searchable-select__dropdown searchable-select__dropdown--portal"
        role="listbox"
        :style="portalDropdownStyle"
      >
        <button
          v-for="(option, optionIndex) in filteredOptions"
          :key="option.value || 'empty-option'"
          class="searchable-select__option"
          :class="{ 'searchable-select__option--active': optionIndex === highlightedIndex }"
          type="button"
          role="option"
          :aria-selected="option.value === modelValue"
          @mouseenter="highlightedIndex = optionIndex"
          @click="selectOption(option)"
        >
          {{ option.label }}
        </button>
        <p v-if="filteredOptions.length === 0" class="searchable-select__empty">
          검색 결과가 없습니다.
        </p>
      </div>
    </teleport>
  </div>
</template>

<style scoped>
.searchable-select {
  position: relative;
  width: 100%;
}

.searchable-select__input {
  width: 100%;
  min-height: 48px;
  padding-right: 42px;
}

.searchable-select__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 20;
  display: grid;
  gap: 4px;
  max-height: 240px;
  padding: 8px;
  overflow-y: auto;
  border: 1px solid rgba(147, 210, 255, 0.22);
  border-radius: var(--radius-small);
  background: rgba(8, 23, 42, 0.98);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
}

.searchable-select__dropdown--portal {
  right: auto;
}

.searchable-select__option {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 40px;
  padding: 0 12px;
  border: 0;
  border-radius: calc(var(--radius-small) - 4px);
  background: transparent;
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
}

.searchable-select__option--active,
.searchable-select__option:hover {
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
}

.searchable-select__empty {
  margin: 0;
  padding: 10px 12px;
  color: var(--color-text-muted);
}
</style>
