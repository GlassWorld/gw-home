# 02. Share Module

## 목표
모든 모듈이 의존하는 공통 클래스 생성

## 생성 파일

```
share/src/main/java/com/gw/share/
├── common/
│   ├── response/
│   │   ├── ApiResponse.java
│   │   └── PageResponse.java
│   ├── exception/
│   │   ├── BusinessException.java
│   │   └── ErrorCode.java
│   ├── handler/
│   │   └── GlobalExceptionHandler.java
│   └── util/
│       └── UuidUtil.java
└── config/
    └── JacksonConfig.java
```

## 상세 스펙

### ApiResponse<T>
```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message
) {
    public static <T> ApiResponse<T> ok(T data) {...}
    public static ApiResponse<Void> ok() {...}
    public static ApiResponse<Void> fail(String message) {...}
}
```

### PageResponse<T>
```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalCount,
    int totalPages
) {}
```

### ErrorCode (enum)
```
NOT_FOUND(404, "리소스를 찾을 수 없습니다")
UNAUTHORIZED(401, "인증이 필요합니다")
FORBIDDEN(403, "접근 권한이 없습니다")
BAD_REQUEST(400, "잘못된 요청입니다")
DUPLICATE(409, "이미 존재합니다")
INTERNAL_ERROR(500, "서버 오류가 발생했습니다")
```

### BusinessException
```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    public BusinessException(ErrorCode errorCode) {...}
    public BusinessException(ErrorCode errorCode, String message) {...}
}
```

### GlobalExceptionHandler
- `@RestControllerAdvice`
- `BusinessException` → `ApiResponse.fail()` 반환
- `MethodArgumentNotValidException` → 400 반환
- `Exception` → 500 반환

### UuidUtil
```java
public static String generate() // UUID 생성
public static boolean isValid(String uuid)
```

### JacksonConfig
- `LocalDateTime` → ISO 8601
- snake_case 직렬화 설정 (필요시)

## 완료 체크

- [x] ApiResponse 생성
- [x] PageResponse 생성
- [x] ErrorCode 생성
- [x] BusinessException 생성
- [x] GlobalExceptionHandler 생성
- [x] UuidUtil 생성
- [x] JacksonConfig 생성
