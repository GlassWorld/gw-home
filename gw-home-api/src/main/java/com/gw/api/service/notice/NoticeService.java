package com.gw.api.service.notice;

import com.gw.api.dto.notice.CreateNoticeRequest;
import com.gw.api.dto.notice.NoticeDetailResponse;
import com.gw.api.dto.notice.NoticeListRequest;
import com.gw.api.dto.notice.NoticeSummaryResponse;
import com.gw.api.dto.notice.UpdateNoticeRequest;
import com.gw.infra.db.mapper.notice.NoticeMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.query.SortDirection;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.notice.NtcJvo;
import com.gw.share.jvo.notice.NtcSmryJvo;
import com.gw.share.vo.notice.NtcListSrchVo;
import com.gw.share.vo.notice.NtcVo;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NoticeService {

    private static final Map<String, String> NOTICE_SORT_FIELD_MAP = Map.of(
            "createdAt", "created_at",
            "viewCount", "view_cnt",
            "title", "ttl"
    );

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> getNoticeList(NoticeListRequest request) {
        NtcListSrchVo query = buildSearchVo(request);
        List<NoticeSummaryResponse> content = noticeMapper.selectNoticeList(query).stream()
                .map(this::toSummaryResponse)
                .toList();
        long totalCount = noticeMapper.countNoticeList(query);
        int totalPages = (int) Math.ceil((double) totalCount / query.getSize());

        return new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
    }

    @Transactional(readOnly = true)
    public List<NoticeSummaryResponse> getDashboardNotices(int limit) {
        int normalizedLimit = limit < 1 ? 5 : limit;
        return noticeMapper.selectDashboardNoticeList(normalizedLimit).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public NoticeDetailResponse getNotice(String noticeUuid) {
        NtcJvo notice = getNoticeByUuid(noticeUuid);
        noticeMapper.incrementViewCount(noticeUuid);
        return toDetailResponse(getNoticeByUuid(noticeUuid));
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> getAdminNoticeList(NoticeListRequest request) {
        return getNoticeList(request);
    }

    @Transactional(readOnly = true)
    public NoticeDetailResponse getAdminNotice(String noticeUuid) {
        return toDetailResponse(getNoticeForAdminByUuid(noticeUuid));
    }

    public NoticeDetailResponse createNotice(String loginId, CreateNoticeRequest request) {
        NtcVo notice = NtcVo.builder()
                .ttl(request.title())
                .cntnt(request.content())
                .createdBy(loginId)
                .build();

        noticeMapper.insertNotice(notice);
        return toDetailResponse(getNoticeByIdx(notice.getIdx()));
    }

    public NoticeDetailResponse updateNotice(String loginId, String noticeUuid, UpdateNoticeRequest request) {
        NtcJvo savedNotice = getNoticeByUuid(noticeUuid);
        NtcVo notice = NtcVo.builder()
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

        int updatedCount = noticeMapper.updateNotice(notice);

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        return toDetailResponse(getNoticeByUuid(noticeUuid));
    }

    public void deleteNotice(String loginId, String noticeUuid) {
        getNoticeByUuid(noticeUuid);
        int updatedCount = noticeMapper.deleteNotice(noticeUuid, loginId);

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }
    }

    private NtcListSrchVo buildSearchVo(NoticeListRequest request) {
        return NtcListSrchVo.builder()
                .kwd(trimToNull(request.keyword()))
                .page(PageSortSupport.normalizePage(request.page()))
                .size(PageSortSupport.normalizeSize(request.size()))
                .sortBy(request.sortBy())
                .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                .orderByClause(
                        PageSortSupport.resolveOrderByClause(
                                request.sortBy(),
                                request.sortDirection(),
                                NOTICE_SORT_FIELD_MAP,
                                "createdAt",
                                SortDirection.DESC
                        )
                )
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private NtcJvo getNoticeByUuid(String noticeUuid) {
        NtcJvo notice = noticeMapper.selectNoticeByUuid(noticeUuid);

        if (notice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        return notice;
    }

    private NtcJvo getNoticeForAdminByUuid(String noticeUuid) {
        NtcJvo notice = noticeMapper.selectNoticeForAdminByUuid(noticeUuid);

        if (notice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        return notice;
    }

    private NtcJvo getNoticeByIdx(Long noticeIdx) {
        NtcJvo notice = noticeMapper.selectNoticeByIdx(noticeIdx);

        if (notice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        return notice;
    }

    private NoticeSummaryResponse toSummaryResponse(NtcSmryJvo notice) {
        return new NoticeSummaryResponse(
                notice.getUuid(),
                notice.getTtl(),
                notice.getViewCnt() == null ? 0 : notice.getViewCnt(),
                notice.getCreatedBy(),
                notice.getCreatedAt()
        );
    }

    private NoticeDetailResponse toDetailResponse(NtcJvo notice) {
        return new NoticeDetailResponse(
                notice.getUuid(),
                notice.getTtl(),
                notice.getCntnt(),
                notice.getViewCnt() == null ? 0 : notice.getViewCnt(),
                notice.getCreatedBy(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
