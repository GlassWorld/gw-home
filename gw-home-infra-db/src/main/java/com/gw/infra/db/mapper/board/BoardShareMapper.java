package com.gw.infra.db.mapper.board;

import com.gw.share.jvo.board.BrdPstShrJvo;
import com.gw.share.vo.board.BrdPstShrVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BoardShareMapper {

    BrdPstShrVo selectLatestBoardShareByBoardPostIdx(@Param("brdPstIdx") Long brdPstIdx);

    BrdPstShrVo selectActiveBoardShareByBoardPostIdx(@Param("brdPstIdx") Long brdPstIdx);

    BrdPstShrJvo selectBoardShareByToken(@Param("shareToken") String shareToken);

    void insertBoardShare(BrdPstShrVo boardShare);

    int deactivateBoardSharesByBoardPostIdx(@Param("brdPstIdx") Long brdPstIdx, @Param("updatedBy") String updatedBy);
}
