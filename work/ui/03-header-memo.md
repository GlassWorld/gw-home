# Work: 헤더 메모 기능

> 연관 문서: [review/ui/03-header-memo.md](../../review/ui/03-header-memo.md), [todo/ui/03-header-memo.md](../../todo/ui/03-header-memo.md)

## 작업 범위

- `tb_mbr_prfl` memo 컬럼 추가 및 롤백 SQL 작성
- profile 도메인에 메모 조회/저장 API 추가
- 헤더에서 메모 버튼과 메모 모달 제공

## 구현 순서

1. DDL, `PrflVo`, `ProfileMapper`에 memo 필드와 SQL 반영
2. memo DTO, `ProfileService`, `ProfileController`에 조회/저장 API 반영
3. 프론트 타입, composable, 헤더 메모 모달, `AppHeader` 반영
4. todo 체크 및 가능한 범위 검증 수행

## 운영 반영 순서

1. `tb_mbr_prfl_add_memo.sql` 반영
2. Backend 배포
3. Frontend 배포
4. 헤더 메모 조회/저장 동작 점검
5. 문제 발생 시 애플리케이션 롤백 후 memo 컬럼 롤백 SQL 실행
