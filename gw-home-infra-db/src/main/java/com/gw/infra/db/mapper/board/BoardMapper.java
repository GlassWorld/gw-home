package com.gw.infra.db.mapper.board;

import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.board.BrdPstFileJvo;
import com.gw.share.jvo.board.BrdPstSmryJvo;
import com.gw.share.vo.board.BrdCtgrVo;
import com.gw.share.vo.board.BrdPstFileVo;
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

    List<BrdPstFileJvo> selectBoardAttachments(@Param("brdPstIdx") Long brdPstIdx);

    void insertBoardAttachment(BrdPstFileVo boardAttachment);

    int deleteBoardAttachments(@Param("brdPstIdx") Long brdPstIdx);

    List<BrdCtgrVo> selectAllCategories();

    BrdCtgrVo selectBoardCategoryByIdx(@Param("idx") Long idx);

    BrdCtgrVo selectBoardCategoryByUuid(@Param("uuid") String uuid);

    boolean existsBoardCategoryByName(@Param("name") String name, @Param("excludeUuid") String excludeUuid);

    void insertBoardCategory(BrdCtgrVo category);

    int updateBoardCategory(BrdCtgrVo category);

    int deleteBoardCategory(@Param("uuid") String uuid);

    long countBoardPostsByCategoryIdx(@Param("brdCtgrIdx") Long brdCtgrIdx);
}
