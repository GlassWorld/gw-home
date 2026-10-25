package com.gw.api.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.api.convert.board.BoardConvert;
import com.gw.api.convert.notice.NoticeConvert;
import com.gw.api.dto.board.BoardPostListRequest;
import com.gw.api.dto.notice.NoticeListRequest;
import com.gw.infra.db.config.MyBatisConfig;
import com.gw.share.vo.admin.AdminBrdPstListSrchVo;
import com.gw.share.vo.admin.AdminMbrListSrchVo;
import com.gw.share.vo.account.AcctSmryVo;
import com.gw.share.vo.board.BrdPstFileVo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListMapperContractTest {
    @Test
    void largeOffsetIsBoundAsJdbcLong() throws Exception {
        var config = new MyBatisConfig().sqlSessionFactory(mock(DataSource.class)).getConfiguration();
        var query = BoardConvert.toSearchVo(new BoardPostListRequest(null, null, null, Integer.MAX_VALUE, 20, null, null));
        var statement = config.getMappedStatement("com.gw.infra.db.mapper.board.BoardMapper.selectBoardPostList");
        var parameters = Map.of("req", query);
        var bound = statement.getBoundSql(parameters);
        var jdbc = mock(PreparedStatement.class);
        new DefaultParameterHandler(statement, parameters, bound).setParameters(jdbc);
        verify(jdbc).setInt(1, 20);
        verify(jdbc).setLong(2, 42_949_672_920L);
    }

    @Test
    void exportActualBoundSqlForSearchAndDetailRegression() throws Exception {
        var config = new MyBatisConfig().sqlSessionFactory(mock(DataSource.class)).getConfiguration();
        Map<String, Object> exported = new TreeMap<>();
        for (String variant : List.of("none", "title", "content", "author", "all", "unknown", "category",
                "wildcard", "quoted", "page", "large")) {
            String type = List.of("title", "content", "author", "all", "unknown").contains(variant) ? variant : "all";
            String keyword = switch (variant) {
                case "none", "page", "large" -> null;
                case "wildcard" -> "%";
                case "quoted" -> "' OR 1=1 --";
                default -> "needle";
            };
            int page = variant.equals("large") ? Integer.MAX_VALUE : variant.equals("page") ? 2 : 1;
            int size = variant.equals("page") ? 2 : 20;
            var query = BoardConvert.toSearchVo(new BoardPostListRequest(
                    variant.equals("category") ? "10000000-0000-0000-0000-000000000001" : null,
                    type, keyword, page, size, null, null));
            for (String method : List.of("selectBoardPostList", "countBoardPostList")) {
                export(config, exported, "board." + variant + "." + method, "board.BoardMapper." + method, Map.of("req", query));
            }
        }
        for (String variant : List.of("none", "keyword", "blank", "wildcard", "quoted", "page", "large")) {
            String keyword = switch (variant) {
                case "keyword" -> " needle ";
                case "blank" -> "  ";
                case "wildcard" -> "%";
                case "quoted" -> "' OR 1=1 --";
                default -> null;
            };
            var query = NoticeConvert.toSearchVo(new NoticeListRequest(keyword,
                    variant.equals("large") ? Integer.MAX_VALUE : variant.equals("page") ? 2 : 1,
                    variant.equals("page") ? 2 : 20, null, null));
            for (String method : List.of("selectNoticeList", "countNoticeList")) {
                export(config, exported, "notice." + variant + "." + method, "notice.NoticeMapper." + method, Map.of("req", query));
            }
        }
        for (String variant : List.of("active", "deleted")) {
            long id = variant.equals("active") ? 1L : 4L;
            var boardParams = Map.<String, Object>of("idx", id, "uuid", "20000000-0000-0000-0000-00000000000" + id);
            for (String method : List.of("selectBoardPostByUuid", "selectBoardPostByIdx")) {
                export(config, exported, "board." + variant + "." + method, "board.BoardMapper." + method, boardParams);
            }
            long noticeId = variant.equals("active") ? 1L : 3L;
            var noticeParams = Map.<String, Object>of("idx", noticeId, "uuid", "30000000-0000-0000-0000-00000000000" + noticeId);
            for (String method : List.of("selectNoticeByUuid", "selectNoticeForAdminByUuid", "selectNoticeByIdx")) {
                export(config, exported, "notice." + variant + "." + method, "notice.NoticeMapper." + method, noticeParams);
            }
        }
        export(config, exported, "notice.dashboard", "notice.NoticeMapper.selectDashboardNoticeList", Map.of("limit", 2));
        exportFollowUpSql(config, exported);
        Path destination = Path.of("build", "list-refactor-sql.json");
        Files.createDirectories(destination.getParent());
        new ObjectMapper().writeValue(destination.toFile(), exported);
    }

    // 관리자 목록·동률 페이지·첨부 일괄 처리의 실제 SQL을 함께 검증한다.
    private void exportFollowUpSql(Configuration config, Map<String, Object> exported) {
        for (String variant : List.of("active", "deleted", "keyword")) {
            var query = AdminBrdPstListSrchVo.builder().deleted(variant.equals("deleted"))
                    .kwd(variant.equals("keyword") ? "needle" : null).page(1).size(100).orderByClause("bp.created_at DESC").build();
            for (String method : List.of("selectBoardPostList", "countBoardPostList")) {
                export(config, exported, "admin." + variant + "." + method, "admin.AdminMapper." + method, Map.of("req", query));
            }
        }
        for (int page = 1; page <= 2; page++) {
            export(config, exported, "board.tie." + page, "board.BoardMapper.selectBoardPostList", Map.of("req",
                    BoardConvert.toSearchVo(new BoardPostListRequest(null,null,null,page,2,"ttl","asc"))));
            export(config, exported, "notice.tie." + page, "notice.NoticeMapper.selectNoticeList", Map.of("req",
                    NoticeConvert.toSearchVo(new NoticeListRequest(null,page,2,"title","asc"))));
            export(config, exported, "admin.tie." + page, "admin.AdminMapper.selectBoardPostList", Map.of("req",
                    AdminBrdPstListSrchVo.builder().deleted(false).page(page).size(2).orderByClause("bp.ttl ASC").build()));
            export(config, exported, "member.tie." + page, "admin.AdminMapper.selectMemberList", Map.of("req",
                    AdminMbrListSrchVo.builder().deleted(false).page(page).size(2).orderByClause("ma.created_at DESC").build()));
            Map<String,Object> params = new HashMap<>();
            params.put("loginId", null); params.put("role", null); params.put("acctStat", null);
            params.put("limit", 2); params.put("offset", (page - 1L) * 2);
            export(config, exported, "account.tie." + page, "account.AccountMapper.selectAllAccounts", params);
            export(config, exported, "account.count", "account.AccountMapper.countAllAccounts", params);
        }
        var accountStatement = config.getMappedStatement("com.gw.infra.db.mapper.account.AccountMapper.selectAllAccounts");
        assertEquals(AcctSmryVo.class, accountStatement.getResultMaps().getFirst().getType());
        String accountSql = accountStatement.getBoundSql(Map.of("limit", 2, "offset", 0L)).getSql();
        assertFalse(accountSql.contains("pwd")); assertFalse(accountSql.contains("otp_secret"));
        for (String domain : List.of("board", "notice")) {
            String mapper = domain.equals("board") ? "board.BoardMapper" : "notice.NoticeMapper";
            export(config, exported, domain + ".increment", mapper + ".incrementViewCount", Map.of("uuid",
                    domain.equals("board") ? "20000000-0000-0000-0000-000000000001" : "30000000-0000-0000-0000-000000000001"));
            export(config, exported, domain + ".incrementDeleted", mapper + ".incrementViewCount", Map.of("uuid",
                    domain.equals("board") ? "20000000-0000-0000-0000-000000000004" : "30000000-0000-0000-0000-000000000003"));
        }
        export(config, exported, "file.batch", "file.FileMapper.selectFilesByUuids", Map.of("uuids", List.of(
                "aaaaaaaa-0000-0000-0000-000000000001", "aaaaaaaa-0000-0000-0000-000000000002",
                "aaaaaaaa-0000-0000-0000-000000000003", "aaaaaaaa-0000-0000-0000-000000000009")));
        export(config, exported, "file.empty", "file.FileMapper.selectFilesByUuids", Map.of("uuids", List.of()));
        for (boolean invalid : List.of(false, true)) {
            var attachments = List.of(
                    BrdPstFileVo.builder().brdPstIdx(1L).fileIdx(2L).fileRole("ATTACHMENT").sortOrd(0).createdBy("owner").build(),
                    BrdPstFileVo.builder().brdPstIdx(1L).fileIdx(invalid ? null : 1L).fileRole("ATTACHMENT").sortOrd(1).createdBy("owner").build());
            export(config, exported, invalid ? "attachments.invalid" : "attachments.insert", "board.BoardMapper.insertBoardAttachments", Map.of("attachments", attachments));
        }
        export(config, exported, "attachments.delete", "board.BoardMapper.deleteBoardAttachments", Map.of("brdPstIdx",1L));
    }

    // 런타임 Mapper의 최종 SQL과 실제 바인딩 값을 격리 SQL 검증에 전달한다.
    private void export(Configuration config, Map<String, Object> exported, String key, String method, Map<String, ?> params) {
        var bound = config.getMappedStatement("com.gw.infra.db.mapper." + method).getBoundSql(params);
        List<Object> values = new ArrayList<>();
        for (var binding : bound.getParameterMappings()) {
            assertNotNull(binding.getTypeHandler());
            values.add(bound.hasAdditionalParameter(binding.getProperty())
                    ? bound.getAdditionalParameter(binding.getProperty())
                    : SystemMetaObject.forObject(params).getValue(binding.getProperty()));
        }
        assertFalse(bound.getSql().contains("' OR 1=1 --"));
        exported.put(key, Map.of("sql", bound.getSql(), "values", values));
    }
}
