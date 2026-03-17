package com.gw.api.service.tag;

import com.gw.api.dto.tag.TagResponse;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.tag.TagMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.tag.TagVo;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TagService {

    private final TagMapper tagMapper;
    private final BoardMapper boardMapper;

    public TagService(TagMapper tagMapper, BoardMapper boardMapper) {
        this.tagMapper = tagMapper;
        this.boardMapper = boardMapper;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagMapper.selectAllTags().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> searchTags(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllTags();
        }

        return tagMapper.selectTagsByKeyword(keyword.trim().toLowerCase(Locale.ROOT)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTagsByBrdPstUuid(String brdPstUuid) {
        BrdPstJvo brdPst = getBrdPstByUuid(brdPstUuid);
        return tagMapper.selectTagsByBrdPstIdx(brdPst.getIdx()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TagResponse> attachTag(String loginId, String brdPstUuid, String name) {
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
        return getTagsByBrdPstUuid(brdPstUuid);
    }

    public List<TagResponse> detachTag(String brdPstUuid, String tagUuid) {
        BrdPstJvo brdPst = getBrdPstByUuid(brdPstUuid);
        TagVo tag = getTagByUuid(tagUuid);

        int deletedCount = tagMapper.deleteBrdPstTag(brdPst.getIdx(), tag.getIdx());

        if (deletedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글 태그 매핑을 찾을 수 없습니다.");
        }

        return getTagsByBrdPstUuid(brdPstUuid);
    }

    private BrdPstJvo getBrdPstByUuid(String brdPstUuid) {
        BrdPstJvo brdPst = boardMapper.selectBoardPostByUuid(brdPstUuid);

        if (brdPst == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return brdPst;
    }

    private TagVo getTagByUuid(String tagUuid) {
        TagVo tag = tagMapper.selectTagByUuid(tagUuid);

        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "태그를 찾을 수 없습니다.");
        }

        return tag;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "태그명이 비어 있습니다.");
        }

        return name.trim().toLowerCase(Locale.ROOT);
    }

    private TagResponse toResponse(TagVo tag) {
        return new TagResponse(tag.getUuid(), tag.getNm());
    }
}
