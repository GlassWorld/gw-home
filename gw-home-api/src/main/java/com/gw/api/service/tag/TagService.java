package com.gw.api.service.tag;

import com.gw.api.convert.tag.TagConvert;
import com.gw.api.dto.tag.TagResponse;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.tag.TagMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.tag.TagVo;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class TagService {

    private final TagMapper tagMapper;
    private final BoardMapper boardMapper;

    public TagService(TagMapper tagMapper, BoardMapper boardMapper) {
        this.tagMapper = tagMapper;
        this.boardMapper = boardMapper;
    }

    /** 전체 태그 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        log.info("전체 태그 목록 조회를 시작합니다.");
        List<TagResponse> tagList = tagMapper.selectAllTags().stream()
                .map(TagConvert::toResponse)
                .toList();
        log.info("전체 태그 목록 조회를 완료했습니다. count={}", tagList.size());
        return tagList;
    }

    /** 키워드로 태그를 검색한다. */
    @Transactional(readOnly = true)
    public List<TagResponse> searchTags(String keyword) {
        log.info("태그 검색을 시작합니다. keyword={}", keyword);
        if (keyword == null || keyword.isBlank()) {
            List<TagResponse> tagList = getAllTags();
            log.info("태그 검색어가 비어 있어 전체 태그 목록으로 대체했습니다. count={}", tagList.size());
            return tagList;
        }

        List<TagResponse> tagList = tagMapper.selectTagsByKeyword(keyword.trim().toLowerCase(Locale.ROOT)).stream()
                .map(TagConvert::toResponse)
                .toList();
        log.info("태그 검색을 완료했습니다. keyword={}, count={}", keyword, tagList.size());
        return tagList;
    }

    /** 게시글에 연결된 태그 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<TagResponse> getTagsByBrdPstUuid(String brdPstUuid) {
        log.info("게시글 태그 목록 조회를 시작합니다. boardPostUuid={}", brdPstUuid);
        BrdPstJvo brdPst = getBrdPstByUuid(brdPstUuid);
        List<TagResponse> tagList = tagMapper.selectTagsByBrdPstIdx(brdPst.getIdx()).stream()
                .map(TagConvert::toResponse)
                .toList();
        log.info("게시글 태그 목록 조회를 완료했습니다. boardPostUuid={}, count={}", brdPstUuid, tagList.size());
        return tagList;
    }

    /** 게시글에 태그를 연결한다. */
    public List<TagResponse> attachTag(String loginId, String brdPstUuid, String name) {
        log.info("게시글 태그 연결을 시작합니다. loginId={}, boardPostUuid={}, tagName={}", loginId, brdPstUuid, name);

        try {
            BrdPstJvo brdPst = getBrdPstByUuid(brdPstUuid);
            String normalizedName = normalizeName(name);
            TagVo tag = tagMapper.selectTagByNm(normalizedName);

            if (tag == null) {
                TagVo newTag = TagVo.builder()
                        .nm(normalizedName)
                        .createdBy(loginId)
                        .build();
                tagMapper.insertTag(newTag);
                tag = tagMapper.selectTagByIdx(newTag.getIdx());
            }

            tagMapper.insertBrdPstTag(brdPst.getIdx(), tag.getIdx());
            List<TagResponse> tagList = getTagsByBrdPstUuid(brdPstUuid);
            log.info("게시글 태그 연결을 완료했습니다. loginId={}, boardPostUuid={}, count={}", loginId, brdPstUuid, tagList.size());
            return tagList;
        } catch (BusinessException exception) {
            log.warn("게시글 태그 연결에 실패했습니다. loginId={}, boardPostUuid={}, tagName={}, error={}",
                    loginId, brdPstUuid, name, exception.getMessage());
            throw exception;
        }
    }

    /** 게시글에서 태그를 해제한다. */
    public List<TagResponse> detachTag(String brdPstUuid, String tagUuid) {
        log.info("게시글 태그 해제를 시작합니다. boardPostUuid={}, tagUuid={}", brdPstUuid, tagUuid);

        try {
            BrdPstJvo brdPst = getBrdPstByUuid(brdPstUuid);
            TagVo tag = getTagByUuid(tagUuid);

            int deletedCount = tagMapper.deleteBrdPstTag(brdPst.getIdx(), tag.getIdx());

            if (deletedCount == 0) {
                log.warn("게시글 태그 매핑 삭제 대상이 없습니다. boardPostUuid={}, tagUuid={}", brdPstUuid, tagUuid);
                throw new BusinessException(ErrorCode.NOT_FOUND, "게시글 태그 매핑을 찾을 수 없습니다.");
            }

            List<TagResponse> tagList = getTagsByBrdPstUuid(brdPstUuid);
            log.info("게시글 태그 해제를 완료했습니다. boardPostUuid={}, tagUuid={}, count={}", brdPstUuid, tagUuid, tagList.size());
            return tagList;
        } catch (BusinessException exception) {
            log.warn("게시글 태그 해제에 실패했습니다. boardPostUuid={}, tagUuid={}, error={}",
                    brdPstUuid, tagUuid, exception.getMessage());
            throw exception;
        }
    }

    private BrdPstJvo getBrdPstByUuid(String brdPstUuid) {
        BrdPstJvo brdPst = boardMapper.selectBoardPostByUuid(brdPstUuid);

        if (brdPst == null) {
            log.warn("게시글 조회에 실패했습니다. boardPostUuid={}", brdPstUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return brdPst;
    }

    private TagVo getTagByUuid(String tagUuid) {
        TagVo tag = tagMapper.selectTagByUuid(tagUuid);

        if (tag == null) {
            log.warn("태그 조회에 실패했습니다. tagUuid={}", tagUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "태그를 찾을 수 없습니다.");
        }

        return tag;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            log.warn("태그명이 비어 있습니다.");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "태그명이 비어 있습니다.");
        }

        return name.trim().toLowerCase(Locale.ROOT);
    }
}
