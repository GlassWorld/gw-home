package com.gw.api.controller.file;

import com.gw.api.dto.file.FileUploadResponse;
import com.gw.api.service.file.FileService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // 로그인 사용자의 파일 업로드 요청을 처리한다.
    @PostMapping
    public ApiResponse<FileUploadResponse> uploadFile(
            Principal principal,
            @RequestParam("upldrType") String uploaderType,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(fileService.uploadFile(getLoginId(principal), uploaderType, file));
    }

    // 로그인 사용자의 파일 다운로드 요청을 처리한다.
    @GetMapping("/{fileUuid}/download")
    public ResponseEntity<Resource> downloadFile(Principal principal, @PathVariable String fileUuid) {
        getLoginId(principal);
        Resource resource = fileService.downloadFile(fileUuid);
        String encodedFileName = URLEncoder.encode(resource.getFilename() == null ? "download" : resource.getFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // 로그인 사용자가 파일을 삭제한다.
    @DeleteMapping("/{fileUuid}")
    public ApiResponse<Void> deleteFile(Principal principal, @PathVariable String fileUuid) {
        fileService.deleteFile(getLoginId(principal), fileUuid);
        return ApiResponse.ok();
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
