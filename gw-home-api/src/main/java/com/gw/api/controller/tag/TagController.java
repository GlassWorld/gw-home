package com.gw.api.controller.tag;

import com.gw.api.dto.tag.AttachTagRequest;
import com.gw.api.dto.tag.TagResponse;
import com.gw.api.service.tag.TagService;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    // 전체 태그 목록을 조회한다.
    @GetMapping("/tags")
    public ApiResponse<List<TagResponse>> getAllTags() {
        return ApiResponse.ok(tagService.getAllTags());
    }

    // 키워드로 태그를 검색한다.
    @GetMapping("/tags/search")
    public ApiResponse<List<TagResponse>> searchTags(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(tagService.searchTags(keyword));
    }

    // 로그인 사용자가 게시글에 태그를 연결한다.
    @PostMapping("/boards/{boardPostUuid}/tags")
    public ApiResponse<List<TagResponse>> attachTag(
            Principal principal,
            @PathVariable String boardPostUuid,
            @Valid @RequestBody AttachTagRequest request
    ) {
        return ApiResponse.ok(tagService.attachTag(getLoginId(principal), boardPostUuid, request.name()));
    }

    // 로그인 사용자가 게시글에서 태그를 해제한다.
    @DeleteMapping("/boards/{boardPostUuid}/tags/{tagUuid}")
    public ApiResponse<List<TagResponse>> detachTag(
            Principal principal,
            @PathVariable String boardPostUuid,
            @PathVariable String tagUuid
    ) {
        getLoginId(principal);
        return ApiResponse.ok(tagService.detachTag(boardPostUuid, tagUuid));
    }

    private String getLoginId(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return principal.getName();
    }
}
