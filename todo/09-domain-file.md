# 09. Domain: file

## 목표
파일/이미지 업로드 및 메타데이터 관리 (독립 도메인)

## DDL

```sql
-- sql/ddl/file/tb_file.sql
CREATE TABLE tb_file (
    file_idx        BIGSERIAL     PRIMARY KEY,
    file_uuid       UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    original_name   VARCHAR(500)  NOT NULL,
    stored_name     VARCHAR(500)  NOT NULL,  -- UUID 기반 저장명
    file_path       VARCHAR(1000) NOT NULL,
    file_url        VARCHAR(1000) NOT NULL,
    mime_type       VARCHAR(100)  NOT NULL,
    file_size       BIGINT        NOT NULL,
    uploader_type   VARCHAR(50)   NOT NULL,  -- PROFILE, BOARD, ETC
    created_by      VARCHAR(100)  NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_file_uploader ON tb_file (created_by);
```

## 생성 파일

```
api/src/main/java/com/gw/api/file/
├── controller/FileController.java
├── service/FileService.java
├── mapper/FileMapper.java
└── dto/
    ├── FileUploadResponse.java
    └── FileDto.java

api/src/main/java/com/gw/api/config/
└── FileStorageConfig.java

infra-db/src/main/resources/mapper/file/FileMapper.xml
infra-db/src/main/resources/sql/ddl/file/tb_file.sql
```

## API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/files` | 파일 업로드 | 필요 |
| DELETE | `/api/v1/files/{fileUuid}` | 파일 삭제 | 필요 (본인) |

## Mapper 메서드

```java
void insertFile(FileDto file);
FileDto selectFileByUuid(@Param("uuid") String uuid);
int deleteFile(@Param("uuid") String uuid);  // deleted_at = now()
```

## FileUploadResponse

```
fileUuid, fileUrl, originalName, mimeType, fileSize
```

## 파일 저장 설정 (application-local.yml 추가)

```yaml
file:
  upload:
    path: /tmp/gw-home/uploads
    base-url: http://localhost:8080/files
    max-size: 10MB
    allowed-types: image/jpeg, image/png, image/gif, image/webp
```

## 서비스 규칙

- 저장 경로: `{upload-path}/{uploader-type}/{yyyyMM}/{uuid}.{ext}`
- 파일명: UUID로 변환 저장 (원본명은 DB에만 보관)
- 허용 MIME 타입만 업로드 허용
- 삭제: `deleted_at` = now() + 실제 파일 삭제
- 다른 도메인은 `fileUrl`만 참조, 이 테이블 직접 참조 금지

## 완료 체크

- [ ] DDL 생성
- [ ] FileMapper (interface + XML)
- [ ] FileStorageConfig 생성
- [ ] FileService (업로드/삭제 로직)
- [ ] FileController
- [ ] 이미지 업로드 → URL 반환 테스트
