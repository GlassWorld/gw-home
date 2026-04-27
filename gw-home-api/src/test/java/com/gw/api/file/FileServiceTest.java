package com.gw.api.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.gw.api.config.FileUploadProperties;
import com.gw.api.dto.file.FileUploadResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.file.FileService;
import com.gw.infra.db.mapper.file.FileMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.file.FileVo;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Mock
    private FileMapper fileMapper;

    @Mock
    private AccountLookupService accountLookupService;

    @TempDir
    java.nio.file.Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setPath(tempDir.toString());
        properties.setBaseUrl("http://localhost:8080/files");
        properties.setAllowedTypes(java.util.List.of("image/png"));
        fileService = new FileService(fileMapper, accountLookupService, properties);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void uploadFileReturnsStoredMetadata() throws Exception {
        String yearMonth = YM_FORMATTER.format(LocalDate.now());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.addHeader("Host", "localhost:8080");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(accountLookupService.getAccountByLoginId("tester_01")).thenReturn(
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
                        .filePath(tempDir.resolve("PROFILE/" + yearMonth + "/saved.png").toString())
                        .fileUrl("http://localhost:8080/files/PROFILE/" + yearMonth + "/saved.png")
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
        assertTrue(Files.exists(tempDir.resolve("PROFILE/" + yearMonth)));
    }

    @Test
    void uploadFileRejectsInvalidUploaderType() {
        when(accountLookupService.getAccountByLoginId("tester_01")).thenReturn(
                AcctVo.builder().idx(1L).lgnId("tester_01").build()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "test".getBytes()
        );

        assertThrows(BusinessException.class, () -> fileService.uploadFile("tester_01", "../profile", file));
    }

    @Test
    void uploadFileUsesForwardedDomainForFileUrl() throws Exception {
        String yearMonth = YM_FORMATTER.format(LocalDate.now());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("backend");
        request.setServerPort(8080);
        request.addHeader("Host", "backend:8080");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "home.glassworld.co.kr");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(accountLookupService.getAccountByLoginId("tester_01")).thenReturn(
                AcctVo.builder().idx(1L).lgnId("tester_01").build()
        );
        doAnswer(invocation -> {
            FileVo fileVo = invocation.getArgument(0);
            fileVo.setIdx(2L);
            return null;
        }).when(fileMapper).insertFile(any(FileVo.class));
        when(fileMapper.selectFileByIdx(2L)).thenReturn(
                FileVo.builder()
                        .idx(2L)
                        .uuid("file-uuid-2")
                        .orgnlNm("sample.png")
                        .strgNm("saved.png")
                        .filePath(tempDir.resolve("BOARD_IMAGE/" + yearMonth + "/saved.png").toString())
                        .fileUrl("https://home.glassworld.co.kr/files/BOARD_IMAGE/" + yearMonth + "/saved.png")
                        .mimeType("image/png")
                        .fileSize(4L)
                        .upldrType("BOARD_IMAGE")
                        .createdBy("tester_01")
                        .build()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "test".getBytes()
        );

        FileUploadResponse response = fileService.uploadFile("tester_01", "board_image", file);
        ArgumentCaptor<FileVo> fileCaptor = ArgumentCaptor.forClass(FileVo.class);
        verify(fileMapper).insertFile(fileCaptor.capture());

        assertEquals("https://home.glassworld.co.kr/files/BOARD_IMAGE/" + yearMonth + "/saved.png", response.fileUrl());
        assertTrue(fileCaptor.getValue().getFileUrl().startsWith("https://home.glassworld.co.kr/files/BOARD_IMAGE/" + yearMonth + "/"));
    }

}
