# Work: 개인 자격증명 관리 (Vault)

> 연관 문서: [review/vault/01-credential-manager.md](../../review/vault/01-credential-manager.md), [todo/vault/01-credential-manager.md](../../todo/vault/01-credential-manager.md)

## 작업 범위

- `tb_vlt_crd` 신규 테이블 및 롤백 SQL 작성
- `vault` 도메인 Backend/Frontend 신규 구현
- `/vault` 라우트 및 헤더 네비게이션 연결
- `tb_vlt_crd` 의 `dsc`, `url` 제거 및 `memo` 단일 필드 통합
- 필드 통합에 따른 `vault` 도메인 Backend/Frontend 연쇄 수정

## 구현 순서

1. DDL 및 롤백 SQL 작성
2. VO, Mapper, Service, Controller 구현
3. Frontend 타입, composable, 컴포넌트, 페이지 구현
4. 네비게이션 연결
5. 필드 통합 반영 (`dsc`, `url` 제거 / `memo` 단일화)
6. todo 체크 및 검증

## 운영 반영 순서

1. `tb_vlt_crd` 생성 SQL 반영
2. 애플리케이션 배포
3. 기능 점검
4. 문제 발생 시 애플리케이션 롤백 후 `DROP TABLE tb_vlt_crd` 실행
