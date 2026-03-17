# Skill: create-service

## 입력

```
DOMAIN: {도메인명}
PURPOSE: {서비스가 수행하는 작업}
METHODS: {메서드 목록}
```

## 출력

- `com.gw.api.{domain}.service.{Domain}Service.java`

## 규칙

- [ ] `@Service` + `@RequiredArgsConstructor`
- [ ] 조회 메서드: `@Transactional(readOnly = true)`
- [ ] 변경 메서드: `@Transactional`
- [ ] Mapper는 생성자 주입 (`@RequiredArgsConstructor`)
- [ ] 결과 없음 → `BusinessException(ErrorCode.NOT_FOUND)` throw
- [ ] `_idx`를 반환 DTO에 포함 금지

## 예시

```java
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;

    @Transactional(readOnly = true)
    public BoardResponse findBoard(String boardUuid) {
        Board board = boardMapper.selectBoardByUuid(boardUuid);
        if (board == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return BoardResponse.from(board);
    }

    @Transactional
    public void saveBoard(CreateBoardRequest req, String loginId) {
        boardMapper.insertBoard(req.toEntity(loginId));
    }
}
```
