package com.gw.api.favorite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.gw.api.dto.favorite.FavoriteResponse;
import com.gw.api.service.account.AccountLookupService;
import com.gw.api.service.favorite.FavoriteService;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.infra.db.mapper.favorite.FavoriteMapper;
import com.gw.share.jvo.board.BrdPstJvo;
import com.gw.share.vo.account.AcctVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private AccountLookupService accountLookupService;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteMapper, boardMapper, accountLookupService);
    }

    @Test
    void toggleBoardPostFavoriteCreatesFavoriteWhenMissing() {
        when(boardMapper.selectBoardPostByUuid("board-uuid")).thenReturn(
                BrdPstJvo.builder().idx(10L).uuid("board-uuid").build()
        );
        when(accountLookupService.getAccountByLoginId("admin")).thenReturn(
                AcctVo.builder().idx(3L).lgnId("admin").build()
        );
        when(favoriteMapper.existsFavorite("BOARD_POST", 10L, 3L)).thenReturn(false);
        when(favoriteMapper.countFavorite("BOARD_POST", 10L)).thenReturn(1L);

        FavoriteResponse response = favoriteService.toggleBoardPostFavorite("admin", "board-uuid");

        assertTrue(response.favorited());
        assertEquals(1L, response.favoriteCount());
    }
}
