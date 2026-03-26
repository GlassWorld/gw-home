# Work: Vault UI 개선

> 연관 문서: [review/vault/04-vault-ui-improvements.md](../../review/vault/04-vault-ui-improvements.md), [todo/vault/03-vault-ui-improvements.md](../../todo/vault/03-vault-ui-improvements.md)

## 작업 범위

- `CredentialDetailModal.vue` 메모 마크다운 렌더링 및 카테고리 badge 색상 적용
- `SearchableSelect.vue` 신규 생성 및 `CredentialFormModal.vue` 카테고리 선택 교체
- Vault 카테고리 color 컬럼 DDL, Backend DTO/Service/Mapper, Frontend 타입/페이지 반영

## 구현 순서

1. 프론트 의존성 및 UI 컴포넌트 추가
2. 메모 마크다운 렌더링과 카테고리 검색 셀렉트 적용
3. `tb_vlt_cat` color 컬럼 및 롤백 SQL 반영
4. Backend category/credential 응답에 color 연결
5. 관리자 카테고리 화면과 vault badge 색상 적용
6. todo 체크 및 타입 검증

## 운영 반영 순서

1. `tb_vlt_cat` color 컬럼 추가 SQL 반영
2. Backend 배포
3. Frontend 배포
4. 관리자 카테고리 색상 등록 및 vault 화면 점검
5. 문제 발생 시 애플리케이션 롤백 후 color 컬럼 롤백 SQL 실행
