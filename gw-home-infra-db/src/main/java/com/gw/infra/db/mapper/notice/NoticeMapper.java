package com.gw.infra.db.mapper.notice;

import com.gw.share.jvo.notice.NtcJvo;
import com.gw.share.jvo.notice.NtcSmryJvo;
import com.gw.share.vo.notice.NtcListSrchVo;
import com.gw.share.vo.notice.NtcVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeMapper {

    NtcJvo selectNoticeByUuid(@Param("uuid") String uuid);

    NtcJvo selectNoticeForAdminByUuid(@Param("uuid") String uuid);

    NtcJvo selectNoticeByIdx(@Param("idx") Long idx);

    List<NtcSmryJvo> selectNoticeList(@Param("req") NtcListSrchVo req);

    long countNoticeList(@Param("req") NtcListSrchVo req);

    List<NtcSmryJvo> selectDashboardNoticeList(@Param("limit") int limit);

    void insertNotice(NtcVo notice);

    int updateNotice(NtcVo notice);

    int deleteNotice(@Param("uuid") String uuid, @Param("updatedBy") String updatedBy);

    int incrementViewCount(@Param("uuid") String uuid);
}
