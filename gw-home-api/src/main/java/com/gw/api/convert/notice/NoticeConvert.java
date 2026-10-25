package com.gw.api.convert.notice;

import com.gw.api.dto.notice.NoticeDetailResponse;
import com.gw.api.dto.notice.CreateNoticeRequest;
import com.gw.api.dto.notice.UpdateNoticeRequest;
import com.gw.api.dto.notice.NoticeListRequest;
import com.gw.api.dto.notice.NoticeSummaryResponse;
import com.gw.share.jvo.notice.NtcJvo;
import com.gw.share.jvo.notice.NtcSmryJvo;
import com.gw.share.util.ConvertUtil;
import com.gw.share.util.StringUtil;
import com.gw.share.common.query.PageSortSupport;
import com.gw.share.common.query.SortDirection;
import com.gw.share.vo.notice.NtcListSrchVo;
import com.gw.share.vo.notice.NtcVo;
import java.util.Map;

public final class NoticeConvert {

    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            "createdAt", "created_at",
            "viewCount", "view_cnt",
            "title", "ttl"
    );

    private NoticeConvert() {
    }

    // 공지 목록 요청의 공백·페이징·정렬을 기존 규칙에 맞게 변환한다.
    public static NtcListSrchVo toSearchVo(NoticeListRequest request) {
        return NtcListSrchVo.builder()
                .kwd(StringUtil.normalizeBlank(request.keyword()))
                .page(PageSortSupport.normalizePage(request.page()))
                .size(PageSortSupport.normalizeSize(request.size()))
                .sortBy(request.sortBy())
                .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                .orderByClause(PageSortSupport.resolveOrderByClause(
                        request.sortBy(), request.sortDirection(), SORT_FIELD_MAP, "createdAt", SortDirection.DESC))
                .build();
    }

    // 공지 등록 요청과 작성자를 저장 모델로 변환한다.
    public static NtcVo toCreateVo(String loginId, CreateNoticeRequest request) {
        return NtcVo.builder()
                .ttl(request.title())
                .cntnt(request.content())
                .createdBy(loginId)
                .build();
    }

    // 기존 식별자와 감사 정보를 보존하면서 공지 수정 모델을 구성한다.
    public static NtcVo toUpdateVo(String loginId, UpdateNoticeRequest request, NtcJvo savedNotice) {
        return NtcVo.builder()
                .idx(savedNotice.getIdx())
                .uuid(savedNotice.getUuid())
                .ttl(request.title())
                .cntnt(request.content())
                .viewCnt(savedNotice.getViewCnt())
                .createdBy(savedNotice.getCreatedBy())
                .createdAt(savedNotice.getCreatedAt())
                .updatedBy(loginId)
                .updatedAt(savedNotice.getUpdatedAt())
                .build();
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
