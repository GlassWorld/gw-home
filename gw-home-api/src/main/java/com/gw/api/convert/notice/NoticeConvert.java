package com.gw.api.convert.notice;

import com.gw.api.dto.notice.NoticeDetailResponse;
import com.gw.api.dto.notice.NoticeSummaryResponse;
import com.gw.share.jvo.notice.NtcJvo;
import com.gw.share.jvo.notice.NtcSmryJvo;
import com.gw.share.util.ConvertUtil;

public final class NoticeConvert {

    private NoticeConvert() {
    }

    // 공지사항 요약 JVO를 목록 응답으로 변환한다.
    public static NoticeSummaryResponse toSummaryResponse(NtcSmryJvo notice) {
        return new NoticeSummaryResponse(
                notice.getUuid(),
                notice.getTtl(),
                ConvertUtil.toInteger(notice.getViewCnt(), 0),
                notice.getCreatedBy(),
                notice.getCreatedAt()
        );
    }

    // 공지사항 JVO를 상세 응답으로 변환한다.
    public static NoticeDetailResponse toDetailResponse(NtcJvo notice) {
        return new NoticeDetailResponse(
                notice.getUuid(),
                notice.getTtl(),
                notice.getCntnt(),
                ConvertUtil.toInteger(notice.getViewCnt(), 0),
                notice.getCreatedBy(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
