package com.gw.infra.db.mapper.admin;

import com.gw.share.jvo.admin.AdminBrdPstJvo;
import com.gw.share.jvo.admin.AdminMbrJvo;
import com.gw.share.jvo.admin.AdminSmryJvo;
import com.gw.share.vo.admin.AdminBrdPstListSrchVo;
import com.gw.share.vo.admin.AdminMbrListSrchVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

    List<AdminMbrJvo> selectMemberList(@Param("req") AdminMbrListSrchVo req);

    long countMemberList(@Param("req") AdminMbrListSrchVo req);

    AdminMbrJvo selectMemberByUuid(@Param("uuid") String uuid);

    int forceDeleteMember(@Param("uuid") String uuid, @Param("updatedBy") String updatedBy);

    List<AdminBrdPstJvo> selectBoardPostList(@Param("req") AdminBrdPstListSrchVo req);

    long countBoardPostList(@Param("req") AdminBrdPstListSrchVo req);

    int forceDeleteBoardPost(@Param("uuid") String uuid, @Param("updatedBy") String updatedBy);

    AdminSmryJvo selectSummary();
}
