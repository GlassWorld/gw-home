package com.gw.api.controller.file;

import com.gw.api.dto.file.FileUploadResponse;
import com.gw.api.service.file.FileService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import java.security.Principal;
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

    @PostMapping
    public ApiResponse<FileUploadResponse> uploadFile(
            Principal principal,
            @RequestParam("upldrType") String uploaderType,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(fileService.uploadFile(getLoginId(principal), uploaderType, file));
    }

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
