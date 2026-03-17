package com.gw.api.service.favorite;

import com.gw.api.dto.favorite.FavoriteResponse;
import com.gw.infra.db.mapper.account.AccountMapper;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.favorite.FavoriteMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.account.AcctVo;
import com.gw.share.vo.favorite.FavVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FavoriteService {

    private static final String BOARD_POST = "BOARD_POST";

    private final FavoriteMapper favoriteMapper;
    private final BoardMapper boardMapper;
    private final AccountMapper accountMapper;

    public FavoriteService(FavoriteMapper favoriteMapper, BoardMapper boardMapper, AccountMapper accountMapper) {
        this.favoriteMapper = favoriteMapper;
        this.boardMapper = boardMapper;
        this.accountMapper = accountMapper;
    }

    public FavoriteResponse toggleBoardPostFavorite(String loginId, String boardPostUuid) {
        BrdPstJvo boardPost = getBoardPostByUuid(boardPostUuid);
        AcctVo account = getAccountByLoginId(loginId);
        boolean exists = favoriteMapper.existsFavorite(BOARD_POST, boardPost.getIdx(), account.getIdx());

        if (exists) {
            favoriteMapper.deleteFavorite(BOARD_POST, boardPost.getIdx(), account.getIdx());
        } else {
            favoriteMapper.insertFavorite(
                    FavVo.builder()
                            .trgtType(BOARD_POST)
                            .trgtIdx(boardPost.getIdx())
                            .mbrAcctIdx(account.getIdx())
                            .createdBy(loginId)
                            .build()
            );
        }

        long favoriteCount = favoriteMapper.countFavorite(BOARD_POST, boardPost.getIdx());
        return new FavoriteResponse(!exists, favoriteCount);
    }

    @Transactional(readOnly = true)
    public FavoriteResponse getBoardPostFavoriteCount(String boardPostUuid) {
        BrdPstJvo boardPost = getBoardPostByUuid(boardPostUuid);
        return new FavoriteResponse(false, favoriteMapper.countFavorite(BOARD_POST, boardPost.getIdx()));
    }

    private BrdPstJvo getBoardPostByUuid(String boardPostUuid) {
        BrdPstJvo boardPost = boardMapper.selectBoardPostByUuid(boardPostUuid);

        if (boardPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        return boardPost;
    }

    private AcctVo getAccountByLoginId(String loginId) {
        AcctVo account = accountMapper.selectAccountByLoginId(loginId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }

        return account;
    }
}
