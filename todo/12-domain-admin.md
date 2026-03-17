# 12. Domain: admin

## 목표
관리자 전용 회원/게시글 관리 및 통계 조회

## 생성 파일

```
{project}-api/src/main/java/com/gw/api/controller/admin/AdminController.java
{project}-api/src/main/java/com/gw/api/service/admin/AdminService.java
{project}-api/src/main/java/com/gw/api/dto/admin/AdminMemberResponse.java
{project}-api/src/main/java/com/gw/api/dto/admin/AdminBoardPostResponse.java
{project}-api/src/main/java/com/gw/api/dto/admin/AdminSummaryResponse.java
{project}-api/src/main/java/com/gw/api/dto/admin/AdminMemberListRequest.java
{project}-api/src/main/java/com/gw/api/dto/admin/AdminBoardPostListRequest.java
{project}-share/src/main/java/com/gw/share/jvo/admin/AdminMbrJvo.java
{project}-share/src/main/java/com/gw/share/jvo/admin/AdminBrdPstJvo.java
{project}-share/src/main/java/com/gw/share/jvo/admin/AdminSmryJvo.java
{project}-share/src/main/java/com/gw/share/vo/admin/AdminMbrListSrchVo.java
{project}-share/src/main/java/com/gw/share/vo/admin/AdminBrdPstListSrchVo.java
{project}-infra-db/src/main/java/com/gw/infra/db/mapper/admin/AdminMapper.java
{project}-infra-db/src/main/resources/mapper/admin/AdminMapper.xml
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
List<AdminMbrJvo> selectMemberList(@Param("req") AdminMbrListSrchVo req);
long countMemberList(@Param("req") AdminMbrListSrchVo req);
AdminMbrJvo selectMemberByUuid(@Param("uuid") String uuid);
int forceDeleteMember(@Param("uuid") String uuid, @Param("updatedBy") String updatedBy);

// 게시글
List<AdminBrdPstJvo> selectBoardPostList(@Param("req") AdminBrdPstListSrchVo req);
long countBoardPostList(@Param("req") AdminBrdPstListSrchVo req);
int forceDeleteBoardPost(@Param("uuid") String uuid, @Param("updatedBy") String updatedBy);

// 통계
AdminSmryJvo selectSummary();
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
page, size, sortBy, sortDirection
```

## 서비스 규칙

- 모든 엔드포인트: `@PreAuthorize("hasRole('ADMIN')")` 적용
- `summary` 쿼리: 단일 SELECT로 모든 카운트 집계
- 강제 삭제: `del_at` = now() (소프트 삭제)
- 목록 정렬/페이징은 `PageSortSupport` 공용 처리 사용

## SecurityConfig 추가

```java
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

## 완료 체크

- [x] AdminMapper (interface + XML)
- [x] DTO 생성
- [x] AdminService
- [x] AdminController
- [x] ADMIN 권한 체크 동작 확인
- [x] summary 통계 쿼리 확인
