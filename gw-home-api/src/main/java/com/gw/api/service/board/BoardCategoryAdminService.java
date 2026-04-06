package com.gw.api.service.board;

import com.gw.api.convert.board.BoardConvert;
import com.gw.api.dto.board.BoardCategoryResponse;
import com.gw.api.dto.board.SaveBoardCategoryRequest;
import com.gw.infra.db.mapper.board.BoardMapper;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import com.gw.share.vo.board.BrdCtgrVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class BoardCategoryAdminService {

    private final BoardMapper boardMapper;

    public BoardCategoryAdminService(BoardMapper boardMapper) {
        this.boardMapper = boardMapper;
    }

    // 관리자용 게시글 카테고리 목록을 조회한다.
    @Transactional(readOnly = true)
    public List<BoardCategoryResponse> getBoardCategories() {
        log.info("getBoardCategories 시작");
        List<BoardCategoryResponse> response = boardMapper.selectAllCategories().stream()
                .map(BoardConvert::toCategoryResponse)
                .toList();
        log.info("getBoardCategories 완료 - count: {}", response.size());
        return response;
    }

    // 관리자가 게시글 카테고리를 등록한다.
    public BoardCategoryResponse createBoardCategory(String loginId, SaveBoardCategoryRequest request) {
        log.info("createBoardCategory 시작 - loginId: {}, categoryName: {}", loginId, request.name());

        try {
            validateDuplicateName(request.name(), null);

            BrdCtgrVo category = BrdCtgrVo.builder()
                    .nm(request.name().trim())
                    .sortOrd(request.sortOrder())
                    .createdBy(loginId)
                    .build();
            boardMapper.insertBoardCategory(category);

            BoardCategoryResponse response = BoardConvert.toCategoryResponse(getBoardCategoryByIdx(category.getIdx()));
            log.info("createBoardCategory 완료 - loginId: {}, categoryUuid: {}", loginId, response.categoryUuid());
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "createBoardCategory 실패 - loginId: {}, categoryName: {}, 원인: {}",
                    loginId,
                    request.name(),
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 관리자가 게시글 카테고리를 수정한다.
    public BoardCategoryResponse updateBoardCategory(String loginId, String categoryUuid, SaveBoardCategoryRequest request) {
        log.info("updateBoardCategory 시작 - loginId: {}, categoryUuid: {}", loginId, categoryUuid);

        try {
            BrdCtgrVo category = getBoardCategory(categoryUuid);
            validateDuplicateName(request.name(), categoryUuid);
            category.setNm(request.name().trim());
            category.setSortOrd(request.sortOrder());
            boardMapper.updateBoardCategory(category);

            BoardCategoryResponse response = BoardConvert.toCategoryResponse(getBoardCategory(categoryUuid));
            log.info("updateBoardCategory 완료 - loginId: {}, categoryUuid: {}", loginId, categoryUuid);
            return response;
        } catch (BusinessException exception) {
            log.error(
                    "updateBoardCategory 실패 - loginId: {}, categoryUuid: {}, 원인: {}",
                    loginId,
                    categoryUuid,
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }

    // 관리자가 게시글 카테고리를 삭제한다.
    public void deleteBoardCategory(String loginId, String categoryUuid) {
        log.info("deleteBoardCategory 시작 - loginId: {}, categoryUuid: {}", loginId, categoryUuid);

        try {
            BrdCtgrVo category = getBoardCategory(categoryUuid);
            validateDeletable(category);
            boardMapper.deleteBoardCategory(categoryUuid);
            log.info("deleteBoardCategory 완료 - loginId: {}, categoryUuid: {}", loginId, categoryUuid);
        } catch (BusinessException exception) {
            log.error(
                    "deleteBoardCategory 실패 - loginId: {}, categoryUuid: {}, 원인: {}",
                    loginId,
                    categoryUuid,
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }

    private BrdCtgrVo getBoardCategory(String categoryUuid) {
        BrdCtgrVo category = boardMapper.selectBoardCategoryByUuid(categoryUuid);

        if (category == null) {
            log.error("getBoardCategory 실패 - 원인: 카테고리를 찾을 수 없습니다. categoryUuid={}", categoryUuid);
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    private BrdCtgrVo getBoardCategoryByIdx(Long categoryIdx) {
        BrdCtgrVo category = boardMapper.selectBoardCategoryByIdx(categoryIdx);

        if (category == null) {
            log.error("getBoardCategoryByIdx 실패 - 원인: 카테고리를 찾을 수 없습니다. categoryIdx={}", categoryIdx);
            throw new BusinessException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        return category;
    }

    private void validateDuplicateName(String name, String excludeUuid) {
        String normalizedName = name == null ? "" : name.trim();

        if (boardMapper.existsBoardCategoryByName(normalizedName, excludeUuid)) {
            log.error("validateDuplicateName 실패 - 원인: 이미 사용 중인 카테고리 이름입니다. name={}, excludeUuid={}", normalizedName, excludeUuid);
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 카테고리 이름입니다.");
        }
    }

    private void validateDeletable(BrdCtgrVo category) {
        long boardPostCount = boardMapper.countBoardPostsByCategoryIdx(category.getIdx());

        if (boardPostCount > 0) {
            log.error(
                    "validateDeletable 실패 - 원인: 사용 중인 카테고리는 삭제할 수 없습니다. categoryUuid={}, boardPostCount={}",
                    category.getUuid(),
                    boardPostCount
            );
            throw new BusinessException(ErrorCode.BAD_REQUEST, "사용 중인 카테고리는 삭제할 수 없습니다.");
        }
    }
}
