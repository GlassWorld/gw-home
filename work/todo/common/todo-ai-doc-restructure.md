# 문서 세팅 재구성 계획 — 완료

- 정리일: 2026-10-25
- 상태: 인덱스 재구성 반영 완료. 관련 작업 문서의 구 경로·현재 상태 정정 완료.
- 구현 근거: `0aea459` (인덱스 재구성), `6057324` (요청 작업 기준 문서 역할).
- 검토: [문서 세팅 검수](../../review/common/review-vibe-doc-setting.md)

## Goal

- 얇은 진입점과 필요한 인덱스만 읽는 구조로 문서 책임과 참조를 정리한다.

## Scope

- 실제 적용 경로는 [AGENTS](../../../.ai/AGENTS.md), [routing](../../../.ai/index/routing.md), [work-docs](../../../.ai/index/work-docs.md), [references](../../../.ai/index/references.md), [skills](../../../.ai/index/skills.md), [doc-ops](../../../.ai/index/doc-ops.md)다.
- 과거 `TASK_ROUTER.md`·`SKILL_INDEX.md` 수정 계획은 종료한다. 해당 파일을 다시 만들지 않는다.
- CORE_RULES·애플리케이션·실제 스킬 구현은 이번 문서 정리 범위가 아니다.

## Current State

- AGENTS가 진입 순서와 우선순위를 정하고 각 인덱스가 작업 라우팅·문서 수명·참조·스킬을 분담한다.
- 과거 점수 8.3/10과 고정 선독 비용 진단은 재구성 전 기록이다. 현재 수치나 재검증 결과로 사용하지 않는다.

## Action Items

- [x] 진입점과 인덱스 책임을 분리한다.
- [x] 작업 문서 규칙은 work-docs, 참조 매핑은 references, 스킬 경로·후보는 skills로 모은다.
- [x] doc-ops 진입점과 기존 트리거를 유지한다.
- [x] review/todo의 폐기 경로와 완료 상태를 현재 구조에 맞게 정리한다.

## Done Criteria

- 기존 구조 재개편을 새 미완료 작업으로 오인하지 않는다.
- 관련 문서는 현재 인덱스를 가리킨다.
- 스킬은 후보로만 남고 실제 구현이나 새로운 성능·토큰 점수를 주장하지 않는다.
