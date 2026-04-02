# 개편된 .ai 문서 기준 리팩토링 검수

## Scope

- 대상: `.ai/AGENTS.md`, `.ai/index/routing.md`, `.ai/index/work-docs.md`, `.ai/index/references.md`, `.ai/index/skills.md`, `work/review/common/review-vibe-doc-setting.md`, `work/todo/common/todo-ai-doc-restructure.md`, `work/todo/common/todo-ai-doc-refinement.md`
- 관점: 개편된 문서 기준 부합 여부, 참조 정확도, 작업 문서 정합성, 후속 작업 안전성

## Summary

- 한줄 요약: `.ai` 본체는 개편 방향에 맞게 정리됐지만, 관련 review/todo 문서가 아직 구 구조를 기준으로 서술해 현재 기준의 신뢰도를 떨어뜨린다.
- 전체 판단: `7.9 / 10`

## Findings or Scores

| 항목 | 점수 | 평가 |
|------|------|------|
| 엔트리 구조 | `8.9 / 10` | `AGENTS -> index` 흐름과 역할 분리는 의도대로 정리됐다 |
| 참조 정확도 | `8.7 / 10` | 현재 `.ai/index/skills.md`의 경로 표기는 실제 저장소와 맞는다 |
| 작업 문서 정합성 | `6.4 / 10` | 관련 review/todo 문서에 구 파일명과 구 판단이 남아 있다 |
| 실행 안전성 | `7.6 / 10` | 구현 전 work 문서를 먼저 읽게 되어 있어, stale 문서가 후속 작업 판단을 오염시킬 수 있다 |

### Finding 1

- 항목: 작업 문서 정합성
- 점수: `6.4 / 10`
- 근거:
  - [`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L7)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L11)은 관련 `work/review`, `work/todo` 문서를 먼저 읽도록 안내한다.
  - 그런데 [`todo-ai-doc-restructure.md`](/home/glassworld/workspace/gw-home/work/todo/common/todo-ai-doc-restructure.md#L9)와 [`todo-ai-doc-restructure.md`](/home/glassworld/workspace/gw-home/work/todo/common/todo-ai-doc-restructure.md#L38), [`todo-ai-doc-refinement.md`](/home/glassworld/workspace/gw-home/work/todo/common/todo-ai-doc-refinement.md#L9)와 [`todo-ai-doc-refinement.md`](/home/glassworld/workspace/gw-home/work/todo/common/todo-ai-doc-refinement.md#L34)는 아직 `.ai/TASK_ROUTER.md`, `.ai/SKILL_INDEX.md`를 주요 작업 대상으로 적고 있다.
  - 현재 기준 문서는 [`routing.md`](/home/glassworld/workspace/gw-home/.ai/index/routing.md#L1), [`work-docs.md`](/home/glassworld/workspace/gw-home/.ai/index/work-docs.md#L1), [`skills.md`](/home/glassworld/workspace/gw-home/.ai/index/skills.md#L1)로 이미 분리되어 있다.
- 영향:
  - 다음 작업자가 AGENTS 흐름을 충실히 따를수록 오히려 과거 구조를 현재 할 일로 오인할 가능성이 높다.

### Finding 2

- 항목: stale review 문서
- 점수: `6.8 / 10`
- 근거:
  - 기존 [`review-vibe-doc-setting.md`](/home/glassworld/workspace/gw-home/work/review/common/review-vibe-doc-setting.md#L1)은 개편 전 문제였던 `.ai/TASK_ROUTER.md`, `.ai/SKILL_INDEX.md` 중심 구조를 현재 문제처럼 계속 서술하고 있었다.
  - 하지만 현재 엔트리 문서는 [`AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L24)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L27), [`AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L41)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L46)처럼 이미 `index` 구조를 기준으로 안내한다.
  - 즉 review 문서가 현재 상태를 설명하는 문서가 아니라, 이전 개편 필요성을 설명하는 기록으로 남아 있었다.
- 영향:
  - `##검토` 결과물을 재사용할 때 해결된 이슈와 미해결 이슈가 섞여 보여 의사결정 비용이 올라간다.

### Finding 3

- 항목: 개편 결과물 자체의 완성도
- 점수: `8.7 / 10`
- 근거:
  - [`AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L5)~[`.ai/AGENTS.md`](/home/glassworld/workspace/gw-home/.ai/AGENTS.md#L15)는 진입 순서와 문서 우선순위를 짧게 고정했다.
  - [`routing.md`](/home/glassworld/workspace/gw-home/.ai/index/routing.md#L5)~[`.ai/index/routing.md`](/home/glassworld/workspace/gw-home/.ai/index/routing.md#L12), [`work-docs.md`](/home/glassworld/workspace/gw-home/.ai/index/work-docs.md#L18)~[`.ai/index/work-docs.md`](/home/glassworld/workspace/gw-home/.ai/index/work-docs.md#L27), [`references.md`](/home/glassworld/workspace/gw-home/.ai/index/references.md#L18)~[`.ai/index/references.md`](/home/glassworld/workspace/gw-home/.ai/index/references.md#L24), [`skills.md`](/home/glassworld/workspace/gw-home/.ai/index/skills.md#L5)~[`.ai/index/skills.md`](/home/glassworld/workspace/gw-home/.ai/index/skills.md#L14)는 책임 분리가 선명하다.
  - 특히 [`skills.md`](/home/glassworld/workspace/gw-home/.ai/index/skills.md#L7)~[`.ai/index/skills.md`](/home/glassworld/workspace/gw-home/.ai/index/skills.md#L8)의 경로 표기와 실제 `.claude/skills/*/SKILL.md` 구조가 일치한다.
- 영향:
  - 본체 구조는 후속 유지보수에 유리하며, 지금 병목은 구조 설계보다 연관 산출물 정리 미흡에 가깝다.

## Recommended Next Actions

1. `work/todo/common/todo-ai-doc-restructure.md`와 `work/todo/common/todo-ai-doc-refinement.md`를 현재 `index` 구조 기준으로 갱신하거나 완료 처리 상태를 명시한다.
2. `##검토` 재사용 기준 문서인 이 리뷰 문서는 앞으로도 현재 상태 기준으로만 유지하고, 과거 개편 제안은 별도 기록으로 분리한다.
3. 다음 `##커밋` 때는 `.ai` 본문뿐 아니라 관련 `work/review`, `work/todo` 문서가 새 기준을 같이 가리키는지 함께 점검한다.
