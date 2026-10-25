# .ai 문서 세팅 2차 압축 계획 — 재구성으로 대체

- 정리일: 2026-10-25
- 상태: 구 구조의 압축 계획 종료. [인덱스 재구성](todo-ai-doc-restructure.md)으로 승계.
- 검토: [문서 세팅 검수](../../review/common/review-vibe-doc-setting.md)

## Goal

- 진입점의 반복 설명과 고정 선독 비용을 줄이려던 목적을 현재 인덱스 구조로 이어간다.

## Scope

- 현재 진입점은 [AGENTS](../../../.ai/AGENTS.md), 세부 규칙은 [routing](../../../.ai/index/routing.md)·[work-docs](../../../.ai/index/work-docs.md)·[skills](../../../.ai/index/skills.md)를 따른다.
- 과거 TASK_ROUTER·SKILL_INDEX 본문 압축을 별도 작업으로 재개하지 않는다. CORE_RULES와 앱 코드는 변경하지 않는다.

## Current State

- 인덱스 분리로 문서 책임이 재정의되어 기존 파일 기준 압축안은 더 이상 실행 대상이 아니다.
- 과거 8.6/10, 토큰 효율 7.6/10, 1911→1973 words 및 목표 8.8~8.9/10은 당시 평가다. 현재 측정값·완료 판정으로 사용하지 않는다.

## Action Items

- [x] 구 파일명 기반 압축 항목을 종료하고 재구성 계획과 연결한다.
- [x] 현재 진입점과 문서 단일 출처를 참조한다.
- [x] 구현 완료와 미측정 토큰 효율을 구분한다.

## Done Criteria

- 폐기된 경로를 수정하거나 동일 규칙을 중복 생성하지 않는다.
- 추가 압축은 실제 필요와 측정 근거가 있는 별도 요청에서 검토한다.
