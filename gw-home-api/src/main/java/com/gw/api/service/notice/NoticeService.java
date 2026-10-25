package com.gw.api.service.notice;

import com.gw.api.convert.notice.NoticeConvert;
import com.gw.api.dto.notice.CreateNoticeRequest;
import com.gw.api.dto.notice.NoticeDetailResponse;
import com.gw.api.dto.notice.NoticeListRequest;
import com.gw.api.dto.notice.NoticeSummaryResponse;
import com.gw.api.dto.notice.UpdateNoticeRequest;
import com.gw.infra.db.mapper.notice.NoticeMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.response.PageResponse;
import com.gw.share.common.query.PageSortSupport;
import com.gw.share.jvo.notice.NtcJvo;
import com.gw.share.vo.notice.NtcListSrchVo;
import com.gw.share.vo.notice.NtcVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    /** 공지사항 목록을 조회한다. */
    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> getNoticeList(NoticeListRequest request) {
        log.info("공지사항 목록 조회를 시작합니다. page={}, size={}, keyword={}",
                request.page(), request.size(), request.keyword());
        try {
            NtcListSrchVo query = NoticeConvert.toSearchVo(request);
            List<NoticeSummaryResponse> content = noticeMapper.selectNoticeList(query).stream()
                    .map(NoticeConvert::toSummaryResponse)
                    .toList();
            long totalCount = noticeMapper.countNoticeList(query);
            PageResponse<NoticeSummaryResponse> response =
                    PageResponse.of(content, query.getPage(), query.getSize(), totalCount);
            log.info("공지사항 목록 조회를 완료했습니다. page={}, size={}, totalCount={}",
                    query.getPage(), query.getSize(), totalCount);
            return response;
        } catch (RuntimeException exception) {
            log.error("공지사항 목록 조회에 실패했습니다.", exception);
            throw exception;
        }
    }

    /** 대시보드 공지사항 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<NoticeSummaryResponse> getDashboardNotices(int limit) {
        int normalizedLimit = limit < 1 ? 5 : PageSortSupport.normalizeSize(limit);
        log.info("대시보드 공지사항 조회를 시작합니다. limit={}", normalizedLimit);
        try {
            List<NoticeSummaryResponse> noticeList = noticeMapper.selectDashboardNoticeList(normalizedLimit).stream()
                    .map(NoticeConvert::toSummaryResponse)
                    .toList();
            log.info("대시보드 공지사항 조회를 완료했습니다. count={}", noticeList.size());
            return noticeList;
        } catch (RuntimeException exception) {
            log.error("대시보드 공지사항 조회에 실패했습니다. limit={}", normalizedLimit, exception);
            throw exception;
        }
    }

    /** 공지사항 상세를 조회한다. */
    public NoticeDetailResponse getNotice(String noticeUuid) {
        log.info("공지사항 상세 조회를 시작합니다. noticeUuid={}", noticeUuid);

        try {
            if (noticeMapper.incrementViewCount(noticeUuid) == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
            }
            NtcJvo notice = getNoticeByUuid(noticeUuid);
            NoticeDetailResponse response = NoticeConvert.toDetailResponse(notice);
            log.info("공지사항 상세 조회를 완료했습니다. noticeUuid={}, noticeIdx={}", noticeUuid, notice.getIdx());
            return response;
        } catch (RuntimeException exception) {
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
            NtcVo notice = NoticeConvert.toCreateVo(loginId, request);

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
            NtcVo notice = NoticeConvert.toUpdateVo(loginId, request, savedNotice);

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
