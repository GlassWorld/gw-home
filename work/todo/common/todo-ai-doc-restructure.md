# 문서 세팅 재구성 계획

## Goal

- 목적: 현재 `.ai` 문서 세팅을 "얇은 엔트리 + 세분화 인덱스 + 필요 시 하위 문서 로드" 구조로 재구성하고, 반복 산출물 작성 흐름은 스킬 전환 후보까지 정리해 바이브코딩 효율과 유지보수성을 함께 높인다.

## Scope

- 포함:
  - `.ai/AGENTS.md`
  - `.ai/TASK_ROUTER.md`
  - `.ai/SKILL_INDEX.md`
  - `.ai/project/summary.md`
  - `.ai/index/` 하위 신규 인덱스 문서
  - `work/review/common/review-vibe-doc-setting.md`
  - 문서형 규칙 중 스킬 전환 후보 정리
- 제외:
  - `.ai/CORE_RULES.md`의 절대 규칙 개편
  - 애플리케이션 코드 변경
  - 기존 도메인 개발 문서 본문 개편
  - 실제 스킬 구현 본문 작성

## Current State

- 현재 상태:
  - 리뷰 기준 전체 평가는 `8.3 / 10`이다.
  - 강점은 진입 흐름, 트리거, SIMPLE/NORMAL/HEAVY 기반 작업 개시 속도다.
  - 주요 병목은 `SKILL_INDEX`의 참조 경로 오표기, `TASK_ROUTER`와 `SKILL_INDEX` 사이 work 문서 책임 중복, `AGENTS`의 고정 선독 비용이다.
  - 추가 논의 결과, 세분화 인덱스 구조와 일부 문서 규칙의 스킬 전환이 다음 개선 방향으로 합의됐다.
- 제약:
  - `CORE_RULES`는 절대 규칙 문서로 유지해야 한다.
  - 문서 수를 늘리더라도 인덱스가 또 다른 장문 설명 문서가 되면 안 된다.
  - 기존 트리거 흐름인 `##검토`, `##계획`, `##작업`, `##커밋`은 유지해야 한다.
  - 스킬 후보를 정리하더라도 실제 규범 문서와 충돌하지 않게 역할 경계를 분명히 해야 한다.

## Action Items

1. `.ai` 문서의 책임을 재정의하고, `AGENTS`, `routing`, `references`, `skills`, `work-docs`, `doc-ops`로 나뉘는 목표 구조를 확정한다.
2. `AGENTS`에서 context loading과 세부 work 문서 규칙을 걷어내고, 엔트리 문서 역할만 남도록 축약 설계를 만든다.
3. `TASK_ROUTER`의 task routing 중심 내용과 work 문서 운영 규칙을 분리해 `index/routing.md`, `index/work-docs.md`로 이관할 내용을 정리한다.
4. `SKILL_INDEX`의 실제 경로 오표기를 수정하고, skill 목록과 work template reference를 분리해 `index/skills.md` 기준으로 재작성한다.
5. `index/references.md`, `index/doc-ops.md`를 신설해 작업 유형별 참조 문서 매핑과 문서 유지보수 작업 전용 진입점을 만든다.
6. `##검토`, `##계획`, task classification 흐름 중 스킬화 이점이 큰 항목을 `work-review-writer`, `work-todo-writer`, `doc-setting-review`, `task-classifier` 후보로 정리한다.
7. 개편 후에는 "짧은 요청 1건 처리 시 실제로 읽는 파일 수", "분기 후 추가로 읽는 인덱스 수", "중복 관리 지점 수"를 기준으로 재검토한다.

## Done Criteria

- `.ai/index/` 기준의 목표 구조와 각 문서 책임이 명확히 정의돼 있다.
- `AGENTS`, `TASK_ROUTER`, `SKILL_INDEX`에서 어떤 내용을 이동하고 어떤 내용을 남길지 이관 기준이 문서화돼 있다.
- work 문서 규칙과 template 책임이 단일 문서로 모이는 방향이 정리돼 있다.
- skill reference path 오류 수정과 스킬 전환 후보 우선순위가 계획에 포함돼 있다.
- 이 계획 문서만 읽어도 다음 `##작업`에서 실제 문서 개편을 바로 시작할 수 있다.
