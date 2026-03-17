package com.gw.infra.db.mapper.tag;

import com.gw.share.vo.tag.TagVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TagMapper {

    List<TagVo> selectAllTags();

    List<TagVo> selectTagsByKeyword(@Param("kwd") String kwd);

    List<TagVo> selectTagsByBrdPstIdx(@Param("brdPstIdx") Long brdPstIdx);

    TagVo selectTagByNm(@Param("nm") String nm);

    TagVo selectTagByUuid(@Param("uuid") String uuid);

    TagVo selectTagByIdx(@Param("idx") Long idx);

    void insertTag(TagVo tag);

    void insertBrdPstTag(@Param("brdPstIdx") Long brdPstIdx, @Param("tagIdx") Long tagIdx);

    int deleteBrdPstTag(@Param("brdPstIdx") Long brdPstIdx, @Param("tagIdx") Long tagIdx);
}
