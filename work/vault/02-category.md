# Work: Vault 카테고리 기능 추가

> 연관 문서: [review/vault/02-category.md](../../review/vault/02-category.md), [todo/vault/02-category.md](../../todo/vault/02-category.md)

## 작업 범위

- `tb_vlt_cat` 신규 테이블 및 롤백 SQL 작성
- `tb_vlt_crd` 에 `vlt_cat_idx` 컬럼 추가 및 인덱스 반영
- `vault` / `admin` 도메인 Backend API 구현
- Vault 목록 카테고리 필터 및 등록/수정 모달 카테고리 선택 구현
- 관리자 카테고리 관리 페이지 및 네비게이션 연결

## 구현 순서

1. DDL 및 롤백 SQL 작성
2. VO, Mapper, DTO, Service, Controller 구현
3. Frontend 타입, composable, vault 페이지/모달 수정
4. 관리자 카테고리 페이지 및 네비게이션 연결
5. todo 체크 및 검증

## 운영 반영 순서

1. `tb_vlt_cat` 생성 SQL 반영
2. `tb_vlt_crd` 에 `vlt_cat_idx` 컬럼 및 인덱스 반영
3. 애플리케이션 배포
4. 기능 점검
5. 문제 발생 시 애플리케이션 롤백 후 스키마 롤백 SQL 실행
