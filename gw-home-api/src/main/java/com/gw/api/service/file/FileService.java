package com.gw.api.service.file;

import com.gw.api.config.FileUploadProperties;
import com.gw.api.dto.file.FileUploadResponse;
import com.gw.api.service.account.AccountService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class FileService {

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final FileMapper fileMapper;
    private final AccountService accountService;
    private final FileUploadProperties fileUploadProperties;

    public FileService(
            FileMapper fileMapper,
            AccountService accountService,
            FileUploadProperties fileUploadProperties
    ) {
        this.fileMapper = fileMapper;
        this.accountService = accountService;
        this.fileUploadProperties = fileUploadProperties;
    }

    public FileUploadResponse uploadFile(String loginId, String uploaderType, MultipartFile file) {
        validateFile(file);
        AcctVo account = accountService.getAccountByLoginId(loginId);

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String savedName = UUID.randomUUID() + extension;
        String normalizedUploaderType = uploaderType.toUpperCase(Locale.ROOT);
        String yearMonth = YM_FORMATTER.format(LocalDate.now());
        Path directory = Path.of(fileUploadProperties.getPath(), normalizedUploaderType, yearMonth);
        Path savedPath = directory.resolve(savedName);

        try {
            Files.createDirectories(directory);
            file.transferTo(savedPath);
        } catch (IOException exception) {
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

        return toResponse(getFileByIdx(fileVo.getIdx()));
    }

    public void deleteFile(String loginId, String fileUuid) {
        FileVo file = getFileByUuid(fileUuid);

        if (!loginId.equals(file.getCreatedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 파일만 삭제할 수 있습니다.");
        }

        int deletedCount = fileMapper.deleteFile(fileUuid);

        if (deletedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }

        try {
            Files.deleteIfExists(Path.of(file.getFilePath()));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 삭제에 실패했습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }

        if (file.getContentType() == null || !fileUploadProperties.getAllowedTypes().contains(file.getContentType())) {
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
        return fileUploadProperties.getBaseUrl() + "/" + uploaderType + "/" + yearMonth + "/" + savedName;
    }

    private FileVo getFileByUuid(String fileUuid) {
        FileVo file = fileMapper.selectFileByUuid(fileUuid);

        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }

        return file;
    }

    private FileVo getFileByIdx(Long idx) {
        FileVo file = fileMapper.selectFileByIdx(idx);

        if (file == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }

        return file;
    }

    private FileUploadResponse toResponse(FileVo file) {
        return new FileUploadResponse(
                file.getUuid(),
                file.getFileUrl(),
                file.getOrgnlNm(),
                file.getMimeType(),
                file.getFileSize()
        );
    }
}
