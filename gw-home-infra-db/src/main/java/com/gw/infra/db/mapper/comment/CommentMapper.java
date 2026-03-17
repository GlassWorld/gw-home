package com.gw.infra.db.mapper.comment;

import com.gw.share.jvo.comment.BrdCmtJvo;
import com.gw.share.vo.comment.BrdCmtVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {

    List<BrdCmtJvo> selectCommentsByBrdPstIdx(@Param("brdPstIdx") Long brdPstIdx);

    BrdCmtJvo selectCommentByUuid(@Param("uuid") String uuid);

    BrdCmtJvo selectCommentByIdx(@Param("idx") Long idx);

    void insertComment(BrdCmtVo cmt);

    int updateComment(BrdCmtVo cmt);

    int deleteComment(@Param("uuid") String uuid);

    long countCommentsByBrdPstIdx(@Param("brdPstIdx") Long brdPstIdx);
}
