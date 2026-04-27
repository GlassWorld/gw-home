package com.gw.api.service.file;

import com.gw.api.convert.file.FileConvert;
import com.gw.api.config.FileUploadProperties;
import com.gw.api.dto.file.FileUploadResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.file.FileMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.file.FileVo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.unit.DataSize;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@Transactional
public class FileService {

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Pattern UPLOADER_TYPE_PATTERN = Pattern.compile("^[A-Z_]+$");

    private final FileMapper fileMapper;
    private final AccountLookupService accountLookupService;
    private final FileUploadProperties fileUploadProperties;

    public FileService(
            FileMapper fileMapper,
            AccountLookupService accountLookupService,
            FileUploadProperties fileUploadProperties
    ) {
        this.fileMapper = fileMapper;
            this.accountLookupService = accountLookupService;
        this.fileUploadProperties = fileUploadProperties;
    }

    /** 파일을 업로드한다. */
    public FileUploadResponse uploadFile(String loginId, String uploaderType, MultipartFile file) {
        log.info("파일 업로드를 시작합니다. loginId={}, uploaderType={}", loginId, uploaderType);

        try {
            validateFile(file);
            AcctVo account = accountLookupService.getAccountByLoginId(loginId);

            String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String extension = extractExtension(originalName);
            String savedName = UUID.randomUUID() + extension;
            String normalizedUploaderType = uploaderType.toUpperCase(Locale.ROOT);
            validateUploaderType(normalizedUploaderType);
            String yearMonth = YM_FORMATTER.format(LocalDate.now());
            Path directory = Path.of(fileUploadProperties.getPath(), normalizedUploaderType, yearMonth);
            Path savedPath = directory.resolve(savedName);

            try {
                Files.createDirectories(directory);
                file.transferTo(savedPath);
            } catch (IOException exception) {
                log.error("파일 저장 중 IO 예외가 발생했습니다. loginId={}, uploaderType={}, error={}",
                        loginId, normalizedUploaderType, exception.getMessage(), exception);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
            }

            String fileUrl = buildFileUrl(normalizedUploaderType, yearMonth, savedName);
            FileVo fileVo = FileVo.builder()
                    .orgnlNm(originalName)
                    .strgNm(savedName)
                    .filePath(savedPath.toString())
                    .fileUrl(fileUrl)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .upldrType(normalizedUploaderType)
                    .createdBy(account.getLgnId())
                    .build();
            fileMapper.insertFile(fileVo);

            FileUploadResponse response = FileConvert.toResponse(getFileByIdx(fileVo.getIdx()));
            log.info("파일 업로드를 완료했습니다. loginId={}, fileUuid={}", loginId, response.fileUuid());
            return response;
        } catch (BusinessException exception) {
            log.warn("파일 업로드에 실패했습니다. loginId={}, uploaderType={}, error={}",
                    loginId, uploaderType, exception.getMessage());
            throw exception;
        }
    }

    private void validateUploaderType(String uploaderType) {
        if (uploaderType == null || !UPLOADER_TYPE_PATTERN.matcher(uploaderType).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "유효하지 않은 업로더 타입입니다.");
        }
    }

    /** 파일을 삭제한다. */
    public void deleteFile(String loginId, String fileUuid) {
        log.info("파일 삭제를 시작합니다. loginId={}, fileUuid={}", loginId, fileUuid);

        try {
            FileVo file = getFileByUuid(fileUuid);

            if (!loginId.equals(file.getCreatedBy())) {
                log.warn("파일 삭제 권한이 없습니다. loginId={}, fileUuid={}", loginId, fileUuid);
                throw new BusinessException(ErrorCode.FORBIDDEN, "본인 파일만 삭제할 수 있습니다.");
            }

            int deletedCount = fileMapper.deleteFile(fileUuid);

            if (deletedCount == 0) {
                log.warn("파일 삭제 대상이 없습니다. fileUuid={}", fileUuid);
                throw new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
            }

            try {
                Files.deleteIfExists(Path.of(file.getFilePath()));
            } catch (IOException exception) {
                log.error("파일 삭제 중 IO 예외가 발생했습니다. loginId={}, fileUuid={}, error={}",
                        loginId, fileUuid, exception.getMessage(), exception);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 삭제에 실패했습니다.");
            }

            log.info("파일 삭제를 완료했습니다. loginId={}, fileUuid={}", loginId, fileUuid);
        } catch (BusinessException exception) {
            log.warn("파일 삭제에 실패했습니다. loginId={}, fileUuid={}, error={}",
                    loginId, fileUuid, exception.getMessage());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    /** 파일 다운로드 리소스를 조회한다. */
    public Resource downloadFile(String fileUuid) {
        log.info("파일 다운로드 조회를 시작합니다. fileUuid={}", fileUuid);
        FileVo file = getFileByUuid(fileUuid);
        Resource resource = new FileSystemResource(file.getFilePath());

        if (!resource.exists()) {
            log.warn("파일 다운로드 대상이 존재하지 않습니다. fileUuid={}", fileUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }

        log.info("파일 다운로드 조회를 완료했습니다. fileUuid={}", fileUuid);
        return resource;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("업로드할 파일이 비어 있습니다.");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }

        if (fileUploadProperties.getMaxSize() != null && !fileUploadProperties.getMaxSize().isBlank()) {
            long maxFileSize = DataSize.parse(fileUploadProperties.getMaxSize()).toBytes();

            if (file.getSize() > maxFileSize) {
                log.warn("업로드 파일 용량이 제한을 초과했습니다. fileSize={}, maxFileSize={}", file.getSize(), maxFileSize);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "업로드 가능한 최대 파일 용량을 초과했습니다.");
            }
        }

        if (file.getContentType() == null || !fileUploadProperties.getAllowedTypes().contains(file.getContentType())) {
            log.warn("허용되지 않은 파일 형식입니다. contentType={}", file.getContentType());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "허용되지 않은 파일 형식입니다.");
        }
    }

    private String extractExtension(String originalName) {
        int lastDotIndex = originalName.lastIndexOf('.');

        if (lastDotIndex < 0) {
            return "";
        }

        return originalName.substring(lastDotIndex);
    }

    private String buildFileUrl(String uploaderType, String yearMonth, String savedName) {
        String baseUrl = resolveFileBaseUrl();
        return baseUrl + "/" + uploaderType + "/" + yearMonth + "/" + savedName;
    }

    private String resolveFileBaseUrl() {
        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            String scheme = getFirstForwardedValue(request.getHeader("X-Forwarded-Proto"));
            String host = getFirstForwardedValue(request.getHeader("X-Forwarded-Host"));

            if (scheme == null || scheme.isBlank()) {
                scheme = request.getScheme();
            }

            if (host == null || host.isBlank()) {
                host = request.getHeader("Host");
            }

            if (host == null || host.isBlank()) {
                host = request.getServerName();
                int port = request.getServerPort();

                if (port > 0 && !isDefaultPort(scheme, port)) {
                    host = host + ":" + port;
                }
            }

            if (host != null && !host.isBlank()) {
                String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
                return scheme + "://" + host + contextPath + "/files";
            }
        }

        String configuredBaseUrl = fileUploadProperties.getBaseUrl();

        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            return "/files";
        }

        return configuredBaseUrl.endsWith("/") ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1) : configuredBaseUrl;
    }

    private String getFirstForwardedValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.split(",")[0].trim();
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
    }

    private FileVo getFileByUuid(String fileUuid) {
        FileVo file = fileMapper.selectFileByUuid(fileUuid);

        if (file == null) {
            log.warn("파일 조회에 실패했습니다. fileUuid={}", fileUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }

        return file;
    }

    private FileVo getFileByIdx(Long idx) {
        FileVo file = fileMapper.selectFileByIdx(idx);

        if (file == null) {
            log.warn("파일 식별자 조회에 실패했습니다. fileIdx={}", idx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }

        return file;
    }
}
