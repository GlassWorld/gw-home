# 12. Domain: admin

## 목표
관리자 전용 회원/게시글 관리 및 통계 조회

## 생성 파일

```
api/src/main/java/com/gw/api/admin/
├── controller/AdminController.java
├── service/AdminService.java
├── mapper/AdminMapper.java
└── dto/
    ├── AdminMemberResponse.java
    ├── AdminBoardPostResponse.java
    ├── AdminSummaryResponse.java
    └── AdminMemberListRequest.java

infra-db/src/main/resources/mapper/admin/AdminMapper.xml
```

## API 엔드포인트 (모든 API `ROLE_ADMIN` 필요)

### 회원 관리
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/admin/members` | 회원 목록 (페이징/검색) |
| GET | `/api/v1/admin/members/{memberAccountUuid}` | 회원 상세 |
| DELETE | `/api/v1/admin/members/{memberAccountUuid}` | 회원 강제 탈퇴 |

### 게시글 관리
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/admin/boards` | 게시글 목록 (전체, 삭제 포함) |
| DELETE | `/api/v1/admin/boards/{boardPostUuid}` | 게시글 강제 삭제 |

### 통계
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/admin/summary` | 전체 통계 요약 |

## Mapper 메서드

```java
// 회원
List<AdminMemberDto> selectMemberList(@Param("req") AdminMemberListRequest req);
long countMemberList(@Param("req") AdminMemberListRequest req);
AdminMemberDto selectMemberByUuid(@Param("uuid") String uuid);
int forceDeleteMember(@Param("uuid") String uuid);

// 게시글
List<AdminBoardPostDto> selectAllBoardPostList(@Param("req") AdminBoardPostListRequest req);
int forceDeleteBoardPost(@Param("uuid") String uuid);

// 통계
AdminSummaryDto selectSummary();
```

## AdminSummaryResponse

```
totalMemberCount, activeMemberCount,
totalBoardPostCount, totalCommentCount,
totalFileCount
```

## AdminMemberListRequest

```
keyword: String (loginId or email, nullable)
role: String (nullable)
deleted: boolean (탈퇴 회원 포함 여부, default false)
page, size
```

## 서비스 규칙

- 모든 엔드포인트: `@PreAuthorize("hasRole('ADMIN')")` 적용
- `summary` 쿼리: 단일 SELECT로 모든 카운트 집계
- 강제 삭제: `deleted_at` = now() (소프트 삭제)

## SecurityConfig 추가

```java
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

## 완료 체크

- [ ] AdminMapper (interface + XML)
- [ ] DTO 생성
- [ ] AdminService
- [ ] AdminController
- [ ] ADMIN 권한 체크 동작 확인
- [ ] summary 통계 쿼리 확인
