package com.gw.api.service.admin;

import com.gw.api.dto.admin.AdminBoardPostListRequest;
import com.gw.api.dto.admin.AdminBoardPostResponse;
import com.gw.api.dto.admin.AdminMemberListRequest;
import com.gw.api.dto.admin.AdminMemberResponse;
import com.gw.api.dto.admin.AdminSummaryResponse;
import com.gw.infra.db.mapper.admin.AdminMapper;
import com.gw.infra.db.support.PageSortSupport;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.query.SortDirection;
import com.gw.share.common.response.PageResponse;
import com.gw.share.jvo.admin.AdminBrdPstJvo;
import com.gw.share.jvo.admin.AdminMbrJvo;
import com.gw.share.jvo.admin.AdminSmryJvo;
import com.gw.share.vo.admin.AdminBrdPstListSrchVo;
import com.gw.share.vo.admin.AdminMbrListSrchVo;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {

    private static final Map<String, String> MBR_SORT_FIELD_MAP = Map.of(
            "createdAt", "ma.created_at",
            "loginId", "ma.lgn_id",
            "email", "ma.email",
            "role", "ma.role"
    );

    private static final Map<String, String> BRD_PST_SORT_FIELD_MAP = Map.of(
            "createdAt", "bp.created_at",
            "title", "bp.ttl",
            "viewCnt", "bp.view_cnt"
    );

    private final AdminMapper adminMapper;

    public AdminService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminMemberResponse> getMemberList(AdminMemberListRequest request) {
        AdminMbrListSrchVo query = AdminMbrListSrchVo.builder()
                .kwd(request.keyword())
                .role(request.role())
                .deleted(Boolean.TRUE.equals(request.deleted()))
                .page(PageSortSupport.normalizePage(request.page()))
                .size(PageSortSupport.normalizeSize(request.size()))
                .sortBy(request.sortBy())
                .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                .orderByClause(PageSortSupport.resolveOrderByClause(
                        request.sortBy(),
                        request.sortDirection(),
                        MBR_SORT_FIELD_MAP,
                        "createdAt",
                        SortDirection.DESC
                ))
                .build();

        List<AdminMemberResponse> content = adminMapper.selectMemberList(query).stream()
                .map(this::toMemberResponse)
                .toList();
        long totalCount = adminMapper.countMemberList(query);
        int totalPages = (int) Math.ceil((double) totalCount / query.getSize());

        return new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
    }

    @Transactional(readOnly = true)
    public AdminMemberResponse getMember(String memberAccountUuid) {
        AdminMbrJvo member = adminMapper.selectMemberByUuid(memberAccountUuid);

        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }

        return toMemberResponse(member);
    }

    public void forceDeleteMember(String loginId, String memberAccountUuid) {
        int updatedCount = adminMapper.forceDeleteMember(memberAccountUuid, loginId);

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "회원이 없거나 이미 삭제되었습니다.");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBoardPostResponse> getBoardPostList(AdminBoardPostListRequest request) {
        AdminBrdPstListSrchVo query = AdminBrdPstListSrchVo.builder()
                .kwd(request.keyword())
                .deleted(Boolean.TRUE.equals(request.deleted()))
                .page(PageSortSupport.normalizePage(request.page()))
                .size(PageSortSupport.normalizeSize(request.size()))
                .sortBy(request.sortBy())
                .sortDirection(SortDirection.from(request.sortDirection(), SortDirection.DESC).name())
                .orderByClause(PageSortSupport.resolveOrderByClause(
                        request.sortBy(),
                        request.sortDirection(),
                        BRD_PST_SORT_FIELD_MAP,
                        "createdAt",
                        SortDirection.DESC
                ))
                .build();

        List<AdminBoardPostResponse> content = adminMapper.selectBoardPostList(query).stream()
                .map(this::toBoardPostResponse)
                .toList();
        long totalCount = adminMapper.countBoardPostList(query);
        int totalPages = (int) Math.ceil((double) totalCount / query.getSize());

        return new PageResponse<>(content, query.getPage(), query.getSize(), totalCount, totalPages);
    }

    public void forceDeleteBoardPost(String loginId, String boardPostUuid) {
        int updatedCount = adminMapper.forceDeleteBoardPost(boardPostUuid, loginId);

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글이 없거나 이미 삭제되었습니다.");
        }
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary() {
        AdminSmryJvo summary = adminMapper.selectSummary();
        return new AdminSummaryResponse(
                nullSafe(summary.getTotalMbrCnt()),
                nullSafe(summary.getActvMbrCnt()),
                nullSafe(summary.getTotalBrdPstCnt()),
                nullSafe(summary.getTotalBrdCmtCnt()),
                nullSafe(summary.getTotalFileCnt())
        );
    }

    private AdminMemberResponse toMemberResponse(AdminMbrJvo member) {
        return new AdminMemberResponse(
                member.getUuid(),
                member.getLgnId(),
                member.getEmail(),
                member.getRole(),
                member.getNickNm(),
                member.getIntro(),
                member.getCreatedAt(),
                member.getDelAt()
        );
    }

    private AdminBoardPostResponse toBoardPostResponse(AdminBrdPstJvo boardPost) {
        return new AdminBoardPostResponse(
                boardPost.getUuid(),
                boardPost.getCtgrNm(),
                boardPost.getTtl(),
                boardPost.getAthrNickNm(),
                boardPost.getLgnId(),
                nullSafe(boardPost.getFavCnt()),
                nullSafe(boardPost.getCmtCnt()),
                boardPost.getCreatedAt(),
                boardPost.getDelAt()
        );
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
