package com.gw.api.service.board;

import com.gw.api.convert.board.BoardShareConvert;
import com.gw.api.convert.comment.CommentConvert;
import com.gw.api.dto.board.BoardShareAccessRequest;
import com.gw.api.dto.board.BoardShareAccessStatusResponse;
import com.gw.api.dto.board.BoardShareAttachmentResponse;
import com.gw.api.dto.board.BoardShareSettingsResponse;
import com.gw.api.dto.board.PublicBoardShareResponse;
import com.gw.api.dto.board.SaveBoardShareRequest;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.board.BoardShareMapper;
import com.gw.infra.db.mapper.comment.CommentMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.BoardSharePolicy;
import com.gw.share.common.policy.RolePolicy;
import com.gw.share.common.util.PasswordUtil;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.jvo.board.BrdPstShrJvo;
import com.gw.share.jvo.comment.BrdCmtJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.board.BrdPstShrVo;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class BoardShareService {

    private static final String DELETED_COMMENT_MESSAGE = "삭제된 댓글입니다";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SHARE_TOKEN_BYTE_LENGTH = 32;

    private final BoardMapper boardMapper;
    private final BoardShareMapper boardShareMapper;
    private final CommentMapper commentMapper;
    private final AccountLookupService accountLookupService;

    public BoardShareService(
            BoardMapper boardMapper,
            BoardShareMapper boardShareMapper,
            CommentMapper commentMapper,
            AccountLookupService accountLookupService
    ) {
        this.boardMapper = boardMapper;
        this.boardShareMapper = boardShareMapper;
        this.commentMapper = commentMapper;
        this.accountLookupService = accountLookupService;
    }

    @Transactional(readOnly = true)
    // 로그인 사용자가 게시글 외부 공유 설정을 조회한다.
    public BoardShareSettingsResponse getBoardShare(String loginId, String boardPostUuid) {
        log.info("getBoardShare 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        AcctVo account = getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBoardPost(boardPostUuid);
        validateManagePermission(account, boardPost);
        BrdPstShrVo latestBoardShare = boardShareMapper.selectLatestBoardShareByBoardPostIdx(boardPost.getIdx());
        BoardShareSettingsResponse response = toSettingsResponse(latestBoardShare);
        log.info("getBoardShare 완료 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        return response;
    }

    // 로그인 사용자가 게시글 외부 공유 설정을 저장한다.
    public BoardShareSettingsResponse saveBoardShare(String loginId, String boardPostUuid, SaveBoardShareRequest request) {
        log.info("saveBoardShare 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        AcctVo account = getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBoardPost(boardPostUuid);
        validateManagePermission(account, boardPost);
        BrdPstShrVo latestBoardShare = boardShareMapper.selectLatestBoardShareByBoardPostIdx(boardPost.getIdx());

        if (!Boolean.TRUE.equals(request.shareEnabled())) {
            boardShareMapper.deactivateBoardSharesByBoardPostIdx(boardPost.getIdx(), loginId);
            BoardShareSettingsResponse response = toSettingsResponse(boardShareMapper.selectLatestBoardShareByBoardPostIdx(boardPost.getIdx()));
            log.info("saveBoardShare 완료 - 공유 해제 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
            return response;
        }

        OffsetDateTime expiresAt = resolveExpiresAt(request.expirationDays(), request.expiresAt());
        String passwordHash = resolvePasswordHash(request.passwordEnabled(), request.password(), latestBoardShare);
        boardShareMapper.deactivateBoardSharesByBoardPostIdx(boardPost.getIdx(), loginId);

        BrdPstShrVo boardShare = BrdPstShrVo.builder()
                .brdPstIdx(boardPost.getIdx())
                .shrTkn(generateShareToken())
                .pwdHash(passwordHash)
                .pwdUseYn(Boolean.TRUE.equals(request.passwordEnabled()) ? "Y" : "N")
                .actvYn("Y")
                .exprAt(expiresAt)
                .createdBy(loginId)
                .updatedBy(loginId)
                .build();
        boardShareMapper.insertBoardShare(boardShare);

        BoardShareSettingsResponse response = toSettingsResponse(boardShare);
        log.info("saveBoardShare 완료 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        return response;
    }

    // 로그인 사용자가 게시글 외부 공유 링크를 재발급한다.
    public BoardShareSettingsResponse reissueBoardShare(String loginId, String boardPostUuid) {
        log.info("reissueBoardShare 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        AcctVo account = getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBoardPost(boardPostUuid);
        validateManagePermission(account, boardPost);
        BrdPstShrVo latestBoardShare = boardShareMapper.selectLatestBoardShareByBoardPostIdx(boardPost.getIdx());

        if (latestBoardShare == null) {
            log.error("reissueBoardShare 실패 - 원인: 재발급할 공유 설정이 없습니다. boardPostUuid={}", boardPostUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "재발급할 공유 설정이 없습니다.");
        }

        long originalDurationDays = latestBoardShare.getCreatedAt() == null
                ? BoardSharePolicy.DEFAULT_EXPIRATION_DAYS
                : Math.max(
                        1L,
                        Math.min(
                                BoardSharePolicy.MAX_EXPIRATION_DAYS,
                                ChronoUnit.DAYS.between(latestBoardShare.getCreatedAt(), latestBoardShare.getExprAt())
                        )
                );

        boardShareMapper.deactivateBoardSharesByBoardPostIdx(boardPost.getIdx(), loginId);

        BrdPstShrVo reissuedBoardShare = BrdPstShrVo.builder()
                .brdPstIdx(boardPost.getIdx())
                .shrTkn(generateShareToken())
                .pwdHash(latestBoardShare.getPwdHash())
                .pwdUseYn(latestBoardShare.getPwdUseYn())
                .actvYn("Y")
                .exprAt(OffsetDateTime.now().plusDays(originalDurationDays))
                .createdBy(loginId)
                .updatedBy(loginId)
                .build();
        boardShareMapper.insertBoardShare(reissuedBoardShare);

        BoardShareSettingsResponse response = toSettingsResponse(reissuedBoardShare);
        log.info("reissueBoardShare 완료 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        return response;
    }

    // 로그인 사용자가 게시글 외부 공유를 해제한다.
    public BoardShareSettingsResponse deactivateBoardShare(String loginId, String boardPostUuid) {
        log.info("deactivateBoardShare 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        AcctVo account = getAccountByLoginId(loginId);
        BrdPstJvo boardPost = getBoardPost(boardPostUuid);
        validateManagePermission(account, boardPost);
        boardShareMapper.deactivateBoardSharesByBoardPostIdx(boardPost.getIdx(), loginId);
        BoardShareSettingsResponse response = toSettingsResponse(boardShareMapper.selectLatestBoardShareByBoardPostIdx(boardPost.getIdx()));
        log.info("deactivateBoardShare 완료 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        return response;
    }

    @Transactional(readOnly = true)
    // 외부 사용자가 공유 링크 접근 상태를 조회한다.
    public BoardShareAccessStatusResponse getBoardShareAccessStatus(String shareToken) {
        log.info("getBoardShareAccessStatus 시작 - shareToken: {}", shareToken);
        BrdPstShrJvo boardShare = boardShareMapper.selectBoardShareByToken(shareToken);
        BoardShareAccessStatusResponse response;

        if (boardShare == null) {
            response = BoardShareConvert.toAccessStatusResponse(BoardSharePolicy.ACCESS_STATUS_NOT_FOUND, false, null, null);
        } else {
            String accessStatus = resolveAccessStatus(boardShare);
            response = BoardShareConvert.toAccessStatusResponse(
                    accessStatus,
                    BoardSharePolicy.ACCESS_STATUS_PASSWORD_REQUIRED.equals(accessStatus),
                    boardShare.getExprAt(),
                    boardShare.getTtl()
            );
        }

        log.info("getBoardShareAccessStatus 완료 - shareToken: {}", shareToken);
        return response;
    }

    @Transactional(readOnly = true)
    // 외부 사용자가 공유 링크로 게시글을 읽기 전용 조회한다.
    public PublicBoardShareResponse accessBoardShare(String shareToken, BoardShareAccessRequest request) {
        log.info("accessBoardShare 시작 - shareToken: {}", shareToken);
        BrdPstShrJvo boardShare = boardShareMapper.selectBoardShareByToken(shareToken);

        if (boardShare == null) {
            log.error("accessBoardShare 실패 - 원인: 공유 링크가 존재하지 않습니다. shareToken={}", shareToken);
            throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 공유 링크입니다.");
        }

        validateAccessibleBoardShare(boardShare, request == null ? null : request.password());

        List<BoardShareAttachmentResponse> attachments = boardMapper.selectBoardAttachments(boardShare.getBrdPstIdx()).stream()
                .map(BoardShareConvert::toAttachmentResponse)
                .toList();
        List<BrdCmtJvo> comments = commentMapper.selectCommentsByBrdPstIdx(boardShare.getBrdPstIdx());
        PublicBoardShareResponse response = BoardShareConvert.toPublicResponse(
                boardShare,
                attachments,
                CommentConvert.toTreeResponses(comments, DELETED_COMMENT_MESSAGE)
        );
        log.info("accessBoardShare 완료 - shareToken: {}", shareToken);
        return response;
    }

    private BoardShareSettingsResponse toSettingsResponse(BrdPstShrVo boardShare) {
        if (boardShare == null) {
            return BoardShareConvert.toSettingsResponse(null, BoardSharePolicy.SHARE_STATUS_INACTIVE, false, null);
        }

        String status = resolveManagementStatus(boardShare);
        boolean shareEnabled = BoardSharePolicy.SHARE_STATUS_SHARING.equals(status) || BoardSharePolicy.SHARE_STATUS_EXPIRING_SOON.equals(status);
        String shareToken = shareEnabled ? boardShare.getShrTkn() : null;
        return BoardShareConvert.toSettingsResponse(boardShare, status, shareEnabled, shareToken);
    }

    private OffsetDateTime resolveExpiresAt(Integer expirationDays, OffsetDateTime customExpiresAt) {
        OffsetDateTime now = OffsetDateTime.now();

        if (customExpiresAt != null) {
            validateCustomExpiresAt(now, customExpiresAt);
            return customExpiresAt.truncatedTo(ChronoUnit.MINUTES);
        }

        int normalizedExpirationDays = expirationDays == null ? BoardSharePolicy.DEFAULT_EXPIRATION_DAYS : expirationDays;

        if (normalizedExpirationDays < 1 || normalizedExpirationDays > BoardSharePolicy.MAX_EXPIRATION_DAYS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "공유 만료 기간은 1일 이상 30일 이하여야 합니다.");
        }

        return now.plusDays(normalizedExpirationDays).truncatedTo(ChronoUnit.MINUTES);
    }

    private void validateCustomExpiresAt(OffsetDateTime now, OffsetDateTime customExpiresAt) {
        if (!customExpiresAt.isAfter(now)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "공유 만료 시각은 현재 이후여야 합니다.");
        }

        if (customExpiresAt.isAfter(now.plusDays(BoardSharePolicy.MAX_EXPIRATION_DAYS))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "공유 만료 기간은 최대 30일까지 지정할 수 있습니다.");
        }
    }

    private String resolvePasswordHash(Boolean passwordEnabled, String password, BrdPstShrVo latestBoardShare) {
        if (!Boolean.TRUE.equals(passwordEnabled)) {
            return null;
        }

        String normalizedPassword = password == null ? "" : password.trim();

        if (normalizedPassword.isEmpty()
                && latestBoardShare != null
                && "Y".equals(latestBoardShare.getPwdUseYn())
                && latestBoardShare.getPwdHash() != null
                && !latestBoardShare.getPwdHash().isBlank()) {
            return latestBoardShare.getPwdHash();
        }

        if (normalizedPassword.length() < BoardSharePolicy.MIN_PASSWORD_LENGTH
                || normalizedPassword.length() > BoardSharePolicy.MAX_PASSWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "공유 비밀번호는 4자 이상 100자 이하로 입력해야 합니다."
            );
        }

        return PasswordUtil.encodeWithBcrypt(normalizedPassword);
    }

    private String generateShareToken() {
        byte[] randomBytes = new byte[SHARE_TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void validateAccessibleBoardShare(BrdPstShrJvo boardShare, String password) {
        String accessStatus = resolveAccessStatus(boardShare);

        if (BoardSharePolicy.ACCESS_STATUS_UNAVAILABLE.equals(accessStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "삭제되었거나 더 이상 공유할 수 없는 게시글입니다.");
        }

        if (BoardSharePolicy.ACCESS_STATUS_REVOKED.equals(accessStatus)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해제된 공유 링크입니다.");
        }

        if (BoardSharePolicy.ACCESS_STATUS_EXPIRED.equals(accessStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "만료된 공유 링크입니다.");
        }

        if (BoardSharePolicy.ACCESS_STATUS_PASSWORD_REQUIRED.equals(accessStatus)) {
            String normalizedPassword = password == null ? "" : password.trim();

            if (normalizedPassword.isEmpty()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "비밀번호가 필요한 공유 링크입니다.");
            }

            if (!PasswordUtil.matchesBcrypt(normalizedPassword, boardShare.getPwdHash())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "비밀번호가 일치하지 않습니다.");
            }
        }
    }

    private String resolveManagementStatus(BrdPstShrVo boardShare) {
        if (boardShare.getRvkdAt() != null || !"Y".equals(boardShare.getActvYn())) {
            return BoardSharePolicy.SHARE_STATUS_REVOKED;
        }

        if (boardShare.getExprAt() == null || !boardShare.getExprAt().isAfter(OffsetDateTime.now())) {
            return BoardSharePolicy.SHARE_STATUS_EXPIRED;
        }

        if (boardShare.getExprAt().isBefore(OffsetDateTime.now().plusHours(BoardSharePolicy.EXPIRING_SOON_HOURS))) {
            return BoardSharePolicy.SHARE_STATUS_EXPIRING_SOON;
        }

        return BoardSharePolicy.SHARE_STATUS_SHARING;
    }

    private String resolveAccessStatus(BrdPstShrJvo boardShare) {
        if (boardShare.getBrdPstDelAt() != null || boardShare.getTtl() == null) {
            return BoardSharePolicy.ACCESS_STATUS_UNAVAILABLE;
        }

        if (boardShare.getRvkdAt() != null || !"Y".equals(boardShare.getActvYn())) {
            return BoardSharePolicy.ACCESS_STATUS_REVOKED;
        }

        if (boardShare.getExprAt() == null || !boardShare.getExprAt().isAfter(OffsetDateTime.now())) {
            return BoardSharePolicy.ACCESS_STATUS_EXPIRED;
        }

        if ("Y".equals(boardShare.getPwdUseYn())) {
            return BoardSharePolicy.ACCESS_STATUS_PASSWORD_REQUIRED;
        }

        return BoardSharePolicy.ACCESS_STATUS_AVAILABLE;
    }

    private BrdPstJvo getBoardPost(String boardPostUuid) {
        BrdPstJvo boardPost = boardMapper.selectBoardPostByUuid(boardPostUuid);

        if (boardPost == null) {
            log.error("getBoardPost 실패 - 원인: 게시글을 찾을 수 없습니다. boardPostUuid={}", boardPostUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return boardPost;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }

    private void validateManagePermission(AcctVo account, BrdPstJvo boardPost) {
        if (!account.getIdx().equals(boardPost.getMbrAcctIdx()) && !RolePolicy.ADMIN.equals(account.getRole())) {
            log.error(
                    "validateManagePermission 실패 - 원인: 본인 게시글 또는 관리자만 공유를 설정할 수 있습니다. memberAccountIdx={}, boardOwnerIdx={}, boardPostUuid={}",
                    account.getIdx(),
                    boardPost.getMbrAcctIdx(),
                    boardPost.getUuid()
            );
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 게시글 또는 관리자만 공유를 설정할 수 있습니다.");
        }
    }
}
