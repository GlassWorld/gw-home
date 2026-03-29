---
name: apply-searchable-select
description: Use SearchableSelect for modal-safe select patterns with stable options and explicit fallback.
tags:
  - frontend
  - nuxt
  - vue
  - ui-pattern
  - select
---

# apply-searchable-select

## Use When

- 업무 등록/필터/설정 화면에서 선택 컴포넌트를 공통화할 때
- 모달에서 드롭다운이 잘리거나 레이어가 깨지는 이슈가 우려될 때
- 문자열 기반 option 목록을 안전하게 바인딩하고 싶을 때

## Read First

- `docs/frontend/frontend-rules.md`
- `docs/frontend/pages.md`
- `gw-home-ui/components/common/SearchableSelect.vue`

## Input

```
CONTEXT: {페이지명/컴포넌트명}
USE_IN_MODAL: true | false
OPTIONS: [{ value: string, label: string }]
MODEL_KEY: {바인딩 키}
FALLBACK_LABEL: {전체 / 선택 항목 등}
```

## Output

- `SearchableSelect`를 공통 컴포넌트로 적용한 템플릿/로직
- 모달에서는 기본 `teleportToBody` 유지
- option 변경 시 `applySelectableValueFromOptions`로 안전하게 반영

## Must Follow

- `SearchableSelect`는 `options: Array<{ value: string, label: string }>` 형태로 구성
- `modelValue`는 문자열 타입으로 유지하고, 바인딩 타입에 맞게 `SaveWorkUnitPayload` 등에서 사용
- `@update:modelValue` 이벤트명을 사용 (`@update:model-value` X)
- 모달에서 사용하는 경우 기본값을 `teleport-to-body: true`로 두고, 필요 시 `dropdown-z-index`를 올려 레이어 충돌을 방지
- 기본값(전체/미지정) 항목은 options에 포함해 예상치 못한 공백 선택을 방지

## Never

- ❌ 모달 내부에서 내부 스크롤 컨테이너 기준 absolute 드롭다운만 강제 사용
- ❌ option 값 검증 없이 modelValue를 직접 대입
- ❌ 다른 도메인 규칙을 깨는 식별자(`_idx`) 사용

## Pattern Example

```vue
<SearchableSelect
  :options="statusFilterOptions"
  :model-value="filters.status"
  placeholder="상태"
  input-class="input-field"
  teleport-to-body
  :dropdown-z-index="1200"
  @update:modelValue="(value) => applySelectableValueFromOptions(
    value,
    statusFilterOptions,
    (validValue) => { filters.status = validValue as WorkUnitStatus | '' }
  )"
/>
```

## Implementation Notes

- 모달 깨짐이 자주 반복되면 `teleport-to-body`를 기본으로 두고도 문제가 있으면 실제 z-index 값을 기존 모달 z-index(기본 30~40+)보다 높게 조정
- 포털 렌더링 시에도 `position: fixed` 좌표 계산이 유효하도록 입력 영역이 렌더된 뒤 최초 open 시점에 위치를 갱신
