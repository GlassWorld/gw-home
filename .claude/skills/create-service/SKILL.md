---
name: create-service
description: Create a backend Service class with @Transactional, Mapper injection, and BusinessException handling.
tags:
  - backend
  - service
  - spring
  - transactional
---

# create-service

## Use When

- A new Service class is needed for a domain
- Business logic must be added to an existing Service
- Mapper injection and transaction boundaries need to be set up

## Read First

- `docs/backend/backend-rules.md`

## Input

```
DOMAIN: {도메인명}
PURPOSE: {서비스가 수행하는 작업}
METHODS: {메서드 목록}
```

## Output

Provide implementation:

- `{project}-api/src/main/java/com/gw/api/service/{domain}/{Domain}Service.java`

## Must Follow

- `@Service` + `@RequiredArgsConstructor`
- Read methods: `@Transactional(readOnly = true)`
- Write methods: `@Transactional`
- Inject Mapper via constructor (`@RequiredArgsConstructor`)
- Null result → throw `BusinessException(ErrorCode.NOT_FOUND)`

## Never

- ❌ Include `_idx` in any return DTO
- ❌ Use JPA or `@Repository`
- ❌ Call other Services in a circular manner
- ❌ Add HTTP-layer logic (status codes, HttpServletRequest) to Service

## Example

```java
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;

    @Transactional(readOnly = true)
    public BoardResponse findBoard(String boardUuid) {
        BrdPstVo board = boardMapper.selectBoardByUuid(boardUuid);
        if (board == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return BoardResponse.from(board);
    }

    @Transactional
    public void saveBoard(CreateBoardRequest req, String loginId) {
        boardMapper.insertBoard(BrdPstVo.builder()
            .ttl(req.getTitle())
            .cntnt(req.getContent())
            .createdBy(loginId)
            .build());
    }
}
```
