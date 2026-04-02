package com.gw.api.convert.comment;

import com.gw.api.dto.comment.CommentResponse;
import com.gw.share.jvo.comment.BrdCmtJvo;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommentConvert {

    private CommentConvert() {
    }

    // 댓글 JVO 목록을 트리 응답 목록으로 변환한다.
    public static List<CommentResponse> toTreeResponses(List<BrdCmtJvo> comments, String deletedCommentMessage) {
        Map<Long, MutableCommentNode> nodeMap = new LinkedHashMap<>();
        List<MutableCommentNode> roots = new ArrayList<>();

        for (BrdCmtJvo comment : comments) {
            nodeMap.put(comment.getIdx(), toNode(comment, deletedCommentMessage));
        }

        for (BrdCmtJvo comment : comments) {
            MutableCommentNode node = nodeMap.get(comment.getIdx());

            if (comment.getPrntBrdCmtIdx() == null) {
                roots.add(node);
                continue;
            }

            MutableCommentNode parentNode = nodeMap.get(comment.getPrntBrdCmtIdx());

            if (parentNode == null) {
                roots.add(node);
                continue;
            }

            parentNode.replies().add(node);
        }

        return roots.stream()
                .map(CommentConvert::toResponse)
                .toList();
    }

    // 댓글 JVO를 단건 응답으로 변환한다.
    public static CommentResponse toResponse(BrdCmtJvo comment, String deletedCommentMessage) {
        String content = comment.getDelAt() == null ? comment.getCntnt() : deletedCommentMessage;

        return new CommentResponse(
                comment.getUuid(),
                content,
                comment.getAthrNickNm(),
                comment.getPrntBrdCmtUuid(),
                List.of(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private static MutableCommentNode toNode(BrdCmtJvo comment, String deletedCommentMessage) {
        String content = comment.getDelAt() == null ? comment.getCntnt() : deletedCommentMessage;

        return new MutableCommentNode(
                comment.getUuid(),
                content,
                comment.getAthrNickNm(),
                comment.getPrntBrdCmtUuid(),
                new ArrayList<>(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private static CommentResponse toResponse(MutableCommentNode node) {
        return new CommentResponse(
                node.boardCommentUuid(),
                node.content(),
                node.author(),
                node.parentCommentUuid(),
                node.replies().stream().map(CommentConvert::toResponse).toList(),
                node.createdAt(),
                node.updatedAt()
        );
    }

    private record MutableCommentNode(
            String boardCommentUuid,
            String content,
            String author,
            String parentCommentUuid,
            List<MutableCommentNode> replies,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
