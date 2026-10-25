# 개편된 .ai 문서 기준 작업 이력 정리 검수

- 정리일: 2026-10-25
- 상태: 기존 검토의 구 경로·오래된 작업 상태 문제 정리 완료. .ai 규칙 자체는 변경하지 않았다.

## Scope

- [재구성 계획](../../todo/common/todo-ai-doc-restructure.md), [2차 압축 계획](../../todo/common/todo-ai-doc-refinement.md)과 이 검토 문서의 현재 상태·참조를 정리한다.

## Summary

- 인덱스 구조는 `0aea459`에 이미 반영됐지만 계획 문서가 구 TASK_ROUTER·SKILL_INDEX 작업을 미완료처럼 설명하고 있었다. 재구성 완료와 구 압축안의 승계를 명시했다.
- 이전 Linux 절대경로·오래된 줄 번호 링크를 저장소 상대경로로 바꿨다. 과거 7.9/10 평가는 당시 작업 문서 불일치를 평가한 기록이며 현재 점수로 재사용하지 않는다.

## Findings or Scores

- [AGENTS](../../../.ai/AGENTS.md)는 진입 순서와 우선순위를 정한다.
- [routing](../../../.ai/index/routing.md), [work-docs](../../../.ai/index/work-docs.md), [references](../../../.ai/index/references.md), [skills](../../../.ai/index/skills.md), [doc-ops](../../../.ai/index/doc-ops.md)의 실제 경로를 확인했다.
- 작업 문서는 현재 경로를 가리키며 폐기 파일의 재생성·수정 계획을 종료했다.
- 코어 문서 토큰 수·효율과 새 점수는 이번에 측정하지 않았다. 스킬 후보를 실제 구현 완료로 표시하지 않는다.

## Recommended Next Actions

1. 후속 작업은 현재 AGENTS → index 진입 순서를 따른다.
2. 커밋 시 관련 review/todo의 구현·검증·배포 상태를 함께 정리한다.
3. 추가 구조 변경이나 스킬 구현은 별도 요청 범위에서 검토한다.
