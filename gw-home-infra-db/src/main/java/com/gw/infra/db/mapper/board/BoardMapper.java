package com.gw.infra.db.mapper.board;

import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.board.BrdPstSmryJvo;
import com.gw.share.vo.board.BrdCtgrVo;
import com.gw.share.vo.board.BrdPstListSrchVo;
import com.gw.share.vo.board.BrdPstVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BoardMapper {

    BrdPstJvo selectBoardPostByUuid(@Param("uuid") String uuid);

    BrdPstJvo selectBoardPostByIdx(@Param("idx") Long idx);

    List<BrdPstSmryJvo> selectBoardPostList(@Param("req") BrdPstListSrchVo req);

    long countBoardPostList(@Param("req") BrdPstListSrchVo req);

    void insertBoardPost(BrdPstVo post);

    int updateBoardPost(BrdPstVo post);

    int deleteBoardPost(@Param("uuid") String uuid);

    int incrementViewCount(@Param("uuid") String uuid);

    List<BrdCtgrVo> selectAllCategories();

    BrdCtgrVo selectBoardCategoryByUuid(@Param("uuid") String uuid);
}
