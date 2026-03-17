package com.gw.api.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.gw.api.config.FileUploadProperties;
import com.gw.api.dto.file.FileUploadResponse;
import com.gw.api.service.account.AccountService;
import com.gw.api.service.file.FileService;
import com.gw.infra.db.mapper.file.FileMapper;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.file.FileVo;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileMapper fileMapper;

    @Mock
    private AccountService accountService;

    @TempDir
    java.nio.file.Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setPath(tempDir.toString());
        properties.setBaseUrl("http://localhost:8080/files");
        properties.setAllowedTypes(java.util.List.of("image/png"));
        fileService = new FileService(fileMapper, accountService, properties);
    }

    @Test
    void uploadFileReturnsStoredMetadata() throws Exception {
        when(accountService.getAccountByLoginId("tester_01")).thenReturn(
                AcctVo.builder().idx(1L).lgnId("tester_01").build()
        );
        doAnswer(invocation -> {
            FileVo fileVo = invocation.getArgument(0);
            fileVo.setIdx(1L);
            return null;
        }).when(fileMapper).insertFile(any(FileVo.class));
        when(fileMapper.selectFileByIdx(1L)).thenReturn(
                FileVo.builder()
                        .idx(1L)
                        .uuid("file-uuid")
                        .orgnlNm("sample.png")
                        .strgNm("saved.png")
                        .filePath(tempDir.resolve("PROFILE/202603/saved.png").toString())
                        .fileUrl("http://localhost:8080/files/PROFILE/202603/saved.png")
                        .mimeType("image/png")
                        .fileSize(4L)
                        .upldrType("PROFILE")
                        .createdBy("tester_01")
                        .build()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "test".getBytes()
        );

        FileUploadResponse response = fileService.uploadFile("tester_01", "profile", file);

        assertEquals("file-uuid", response.fileUuid());
        assertEquals("sample.png", response.originalName());
        assertTrue(Files.exists(tempDir.resolve("PROFILE/202603")));
    }
}
