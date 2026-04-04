package com.gw.api.service.favorite;

import com.gw.api.convert.favorite.FavoriteConvert;
import com.gw.api.dto.favorite.FavoriteResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.favorite.FavoriteMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.common.policy.FavoritePolicy;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.favorite.FavVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final BoardMapper boardMapper;
    private final AccountLookupService accountLookupService;

    public FavoriteService(FavoriteMapper favoriteMapper, BoardMapper boardMapper, AccountLookupService accountLookupService) {
        this.favoriteMapper = favoriteMapper;
        this.boardMapper = boardMapper;
        this.accountLookupService = accountLookupService;
    }

    // 로그인 사용자의 게시글 즐겨찾기 상태를 토글한다.
    public FavoriteResponse toggleBoardPostFavorite(String loginId, String boardPostUuid) {
        log.info("toggleBoardPostFavorite 시작 - loginId: {}, boardPostUuid: {}", loginId, boardPostUuid);
        try {
            BrdPstJvo boardPost = getBoardPostByUuid(boardPostUuid);
            AcctVo account = getAccountByLoginId(loginId);
            boolean exists = favoriteMapper.existsFavorite(FavoritePolicy.TARGET_TYPE_BOARD_POST, boardPost.getIdx(), account.getIdx());

            if (exists) {
                favoriteMapper.deleteFavorite(FavoritePolicy.TARGET_TYPE_BOARD_POST, boardPost.getIdx(), account.getIdx());
            } else {
                favoriteMapper.insertFavorite(
                        FavVo.builder()
                                .trgtType(FavoritePolicy.TARGET_TYPE_BOARD_POST)
                                .trgtIdx(boardPost.getIdx())
                                .mbrAcctIdx(account.getIdx())
                                .createdBy(loginId)
                                .build()
                );
            }

            long favoriteCount = favoriteMapper.countFavorite(FavoritePolicy.TARGET_TYPE_BOARD_POST, boardPost.getIdx());
            FavoriteResponse response = FavoriteConvert.toResponse(!exists, favoriteCount);
            log.info(
                    "toggleBoardPostFavorite 완료 - loginId: {}, boardPostUuid: {}, favorited: {}, favoriteCount: {}",
                    loginId,
                    boardPostUuid,
                    response.favorited(),
                    response.favoriteCount()
            );
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "toggleBoardPostFavorite 실패 - loginId: {}, boardPostUuid: {}, 원인: {}, detailMessage: {}",
                    loginId,
                    boardPostUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    // 게시글 즐겨찾기 개수를 조회한다.
    public FavoriteResponse getBoardPostFavoriteCount(String boardPostUuid) {
        log.info("getBoardPostFavoriteCount 시작 - boardPostUuid: {}", boardPostUuid);
        try {
            BrdPstJvo boardPost = getBoardPostByUuid(boardPostUuid);
            FavoriteResponse response = FavoriteConvert.toResponse(
                    false,
                    favoriteMapper.countFavorite(FavoritePolicy.TARGET_TYPE_BOARD_POST, boardPost.getIdx())
            );
            log.info(
                    "getBoardPostFavoriteCount 완료 - boardPostUuid: {}, favoriteCount: {}",
                    boardPostUuid,
                    response.favoriteCount()
            );
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "getBoardPostFavoriteCount 실패 - boardPostUuid: {}, 원인: {}, detailMessage: {}",
                    boardPostUuid,
                    exception.getMessage(),
                    exception.getDetailMessage(),
                    exception
            );
            throw exception;
        }
    }

    private BrdPstJvo getBoardPostByUuid(String boardPostUuid) {
        BrdPstJvo boardPost = boardMapper.selectBoardPostByUuid(boardPostUuid);

        if (boardPost == null) {
            log.error("getBoardPostByUuid 실패 - 원인: 게시글을 찾을 수 없습니다. boardPostUuid={}", boardPostUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return boardPost;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        return accountLookupService.getAccountByLoginId(loginId);
    }
}
