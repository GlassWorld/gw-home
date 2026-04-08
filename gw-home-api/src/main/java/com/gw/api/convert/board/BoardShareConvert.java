package com.gw.api.convert.board;

import com.gw.api.dto.board.BoardShareAccessStatusResponse;
import com.gw.api.dto.board.BoardShareAttachmentResponse;
import com.gw.api.dto.board.BoardShareSettingsResponse;
import com.gw.api.dto.board.PublicBoardShareResponse;
import com.gw.api.dto.comment.CommentResponse;
import com.gw.share.jvo.board.BrdPstFileJvo;
import com.gw.share.jvo.board.BrdPstShrJvo;
import com.gw.share.vo.board.BrdPstShrVo;
import java.time.OffsetDateTime;
import java.util.List;

public final class BoardShareConvert {

    private BoardShareConvert() {
    }

    // 게시글 첨부 정보를 외부 공유 전용 응답으로 변환한다.
    public static BoardShareAttachmentResponse toAttachmentResponse(BrdPstFileJvo boardAttachment) {
        return new BoardShareAttachmentResponse(
                boardAttachment.getOrgnlNm(),
                boardAttachment.getMimeType(),
                boardAttachment.getFileSize() == null ? 0L : boardAttachment.getFileSize(),
                boardAttachment.getCreatedAt()
        );
    }

    // 게시글 외부 공유 설정을 응답으로 변환한다.
    public static BoardShareSettingsResponse toSettingsResponse(BrdPstShrVo boardShare, String status, boolean shareEnabled, String shareToken) {
        if (boardShare == null) {
            return new BoardShareSettingsResponse(false, status, null, false, null, null, null);
        }

        return new BoardShareSettingsResponse(
                shareEnabled,
                status,
                shareToken,
                "Y".equals(boardShare.getPwdUseYn()),
                boardShare.getExprAt(),
                boardShare.getRvkdAt(),
                boardShare.getCreatedAt()
        );
    }

    // 공유 링크 접근 상태를 응답으로 변환한다.
    public static BoardShareAccessStatusResponse toAccessStatusResponse(
            String status,
            boolean passwordRequired,
            OffsetDateTime expiresAt,
            String title
    ) {
        return new BoardShareAccessStatusResponse(status, passwordRequired, expiresAt, title);
    }

    // 외부 공유 게시글 조회 결과를 응답으로 변환한다.
    public static PublicBoardShareResponse toPublicResponse(
            BrdPstShrJvo boardShare,
            List<BoardShareAttachmentResponse> attachments,
            List<CommentResponse> comments
    ) {
        return new PublicBoardShareResponse(
                boardShare.getTtl(),
                boardShare.getCtgrNm(),
                boardShare.getCntnt(),
                boardShare.getAthrNickNm(),
                attachments,
                comments,
                boardShare.getBrdPstCreatedAt(),
                boardShare.getBrdPstUpdatedAt(),
                boardShare.getExprAt()
        );
    }
}
