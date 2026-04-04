package com.gw.api.service.notice;

import com.gw.api.convert.notice.NoticeConvert;
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
import com.gw.share.vo.notice.NtcListSrchVo;
import com.gw.share.vo.notice.NtcVo;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

    /** 공지사항 목록을 조회한다. */
    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> getNoticeList(NoticeListRequest request) {
        log.info("공지사항 목록 조회를 시작합니다. page={}, size={}, keyword={}",
                request.page(), request.size(), request.keyword());
        NtcListSrchVo query = buildSearchVo(request);
        List<NoticeSummaryResponse> content = noticeMapper.selectNoticeList(query).stream()
                .map(NoticeConvert::toSummaryResponse)
                .toList();
        long totalCount = noticeMapper.countNoticeList(query);
        int totalPages = (int) Math.ceil((double) totalCount / query.getSize());
        PageResponse<NoticeSummaryResponse> response =
                new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
        log.info("공지사항 목록 조회를 완료했습니다. page={}, size={}, totalCount={}",
                query.getPage(), query.getSize(), totalCount);
        return response;
    }

    /** 대시보드 공지사항 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<NoticeSummaryResponse> getDashboardNotices(int limit) {
        int normalizedLimit = limit < 1 ? 5 : limit;
        log.info("대시보드 공지사항 조회를 시작합니다. limit={}", normalizedLimit);
        List<NoticeSummaryResponse> noticeList = noticeMapper.selectDashboardNoticeList(normalizedLimit).stream()
                .map(NoticeConvert::toSummaryResponse)
                .toList();
        log.info("대시보드 공지사항 조회를 완료했습니다. count={}", noticeList.size());
        return noticeList;
    }

    /** 공지사항 상세를 조회한다. */
    public NoticeDetailResponse getNotice(String noticeUuid) {
        log.info("공지사항 상세 조회를 시작합니다. noticeUuid={}", noticeUuid);

        try {
            NtcJvo notice = getNoticeByUuid(noticeUuid);
            noticeMapper.incrementViewCount(noticeUuid);
            NoticeDetailResponse response = NoticeConvert.toDetailResponse(getNoticeByUuid(noticeUuid));
            log.info("공지사항 상세 조회를 완료했습니다. noticeUuid={}, noticeIdx={}", noticeUuid, notice.getIdx());
            return response;
        } catch (BusinessException exception) {
            log.warn("공지사항 상세 조회에 실패했습니다. noticeUuid={}, error={}", noticeUuid, exception.getMessage());
            throw exception;
        }
    }

    /** 관리자 공지사항 목록을 조회한다. */
    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> getAdminNoticeList(NoticeListRequest request) {
        return getNoticeList(request);
    }

    /** 관리자 공지사항 상세를 조회한다. */
    @Transactional(readOnly = true)
    public NoticeDetailResponse getAdminNotice(String noticeUuid) {
        log.info("관리자 공지사항 상세 조회를 시작합니다. noticeUuid={}", noticeUuid);
        try {
            NoticeDetailResponse response = NoticeConvert.toDetailResponse(getNoticeForAdminByUuid(noticeUuid));
            log.info("관리자 공지사항 상세 조회를 완료했습니다. noticeUuid={}", noticeUuid);
            return response;
        } catch (BusinessException exception) {
            log.warn("관리자 공지사항 상세 조회에 실패했습니다. noticeUuid={}, error={}", noticeUuid, exception.getMessage());
            throw exception;
        }
    }

    /** 공지사항을 등록한다. */
    public NoticeDetailResponse createNotice(String loginId, CreateNoticeRequest request) {
        log.info("공지사항 등록을 시작합니다. loginId={}, title={}", loginId, request.title());
        try {
            NtcVo notice = NtcVo.builder()
                    .ttl(request.title())
                    .cntnt(request.content())
                    .createdBy(loginId)
                    .build();

            noticeMapper.insertNotice(notice);
            NoticeDetailResponse response = NoticeConvert.toDetailResponse(getNoticeByIdx(notice.getIdx()));
            log.info("공지사항 등록을 완료했습니다. loginId={}, noticeUuid={}", loginId, response.noticeUuid());
            return response;
        } catch (BusinessException exception) {
            log.warn("공지사항 등록에 실패했습니다. loginId={}, title={}, error={}",
                    loginId, request.title(), exception.getMessage());
            throw exception;
        }
    }

    /** 공지사항을 수정한다. */
    public NoticeDetailResponse updateNotice(String loginId, String noticeUuid, UpdateNoticeRequest request) {
        log.info("공지사항 수정을 시작합니다. loginId={}, noticeUuid={}", loginId, noticeUuid);
        try {
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
                log.warn("공지사항 수정 대상이 없습니다. noticeUuid={}", noticeUuid);
                throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
            }

            NoticeDetailResponse response = NoticeConvert.toDetailResponse(getNoticeByUuid(noticeUuid));
            log.info("공지사항 수정을 완료했습니다. loginId={}, noticeUuid={}", loginId, noticeUuid);
            return response;
        } catch (BusinessException exception) {
            log.warn("공지사항 수정에 실패했습니다. loginId={}, noticeUuid={}, error={}",
                    loginId, noticeUuid, exception.getMessage());
            throw exception;
        }
    }

    /** 공지사항을 삭제한다. */
    public void deleteNotice(String loginId, String noticeUuid) {
        log.info("공지사항 삭제를 시작합니다. loginId={}, noticeUuid={}", loginId, noticeUuid);
        try {
            getNoticeByUuid(noticeUuid);
            int updatedCount = noticeMapper.deleteNotice(noticeUuid, loginId);

            if (updatedCount == 0) {
                log.warn("공지사항 삭제 대상이 없습니다. noticeUuid={}", noticeUuid);
                throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
            }
            log.info("공지사항 삭제를 완료했습니다. loginId={}, noticeUuid={}", loginId, noticeUuid);
        } catch (BusinessException exception) {
            log.warn("공지사항 삭제에 실패했습니다. loginId={}, noticeUuid={}, error={}",
                    loginId, noticeUuid, exception.getMessage());
            throw exception;
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
            log.warn("공지사항 조회에 실패했습니다. noticeUuid={}", noticeUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        return notice;
    }

    private NtcJvo getNoticeForAdminByUuid(String noticeUuid) {
        NtcJvo notice = noticeMapper.selectNoticeForAdminByUuid(noticeUuid);

        if (notice == null) {
            log.warn("관리자 공지사항 조회에 실패했습니다. noticeUuid={}", noticeUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        return notice;
    }

    private NtcJvo getNoticeByIdx(Long noticeIdx) {
        NtcJvo notice = noticeMapper.selectNoticeByIdx(noticeIdx);

        if (notice == null) {
            log.warn("공지사항 식별자 조회에 실패했습니다. noticeIdx={}", noticeIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        return notice;
    }
}
