package com.gw.api.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.gw.api.dto.tag.TagResponse;
import com.gw.api.service.tag.TagService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.tag.TagMapper;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.tag.TagVo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private BoardMapper boardMapper;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagMapper, boardMapper);
    }

    @Test
    void attachTagCreatesNormalizedTagAndReturnsTagList() {
        when(boardMapper.selectBoardPostByUuid("board-uuid")).thenReturn(
                BrdPstJvo.builder().idx(1L).uuid("board-uuid").build()
        );
        when(tagMapper.selectTagByNm("spring")).thenReturn(null);
        doAnswer(invocation -> {
            TagVo tag = invocation.getArgument(0);
            tag.setIdx(10L);
            return null;
        }).when(tagMapper).insertTag(org.mockito.ArgumentMatchers.any(TagVo.class));
        when(tagMapper.selectTagByIdx(10L)).thenReturn(
                TagVo.builder().idx(10L).uuid("tag-uuid").nm("spring").build()
        );
        when(tagMapper.selectTagsByBrdPstIdx(anyLong())).thenReturn(
                List.of(TagVo.builder().idx(10L).uuid("tag-uuid").nm("spring").build())
        );

        List<TagResponse> responses = tagService.attachTag("tester_01", "board-uuid", " Spring ");

        assertEquals(1, responses.size());
        assertEquals("spring", responses.get(0).name());
    }
}
