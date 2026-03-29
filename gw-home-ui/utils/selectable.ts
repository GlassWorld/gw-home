export function applySelectableValueFromOptions<T extends string>(
  value: string,
  options: Array<{ value: T }>,
  applyValue: (validValue: T) => void
): void {
  const matchedValue = options.find((option) => option.value === value)?.value

  if (!matchedValue) {
    return
  }

  applyValue(matchedValue)
}
