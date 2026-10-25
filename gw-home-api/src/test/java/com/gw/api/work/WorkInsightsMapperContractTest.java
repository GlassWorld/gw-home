package com.gw.api.work;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.infra.db.config.MyBatisConfig;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WorkInsightsMapperContractTest {
    @Test void exportBoundSqlWithSharedInboxSourceAndEveryFilter() throws Exception {
        var configuration = new MyBatisConfig().sqlSessionFactory(mock(DataSource.class)).getConfiguration();
        Map<String,Object> exported = new TreeMap<>();
        for (String variant : List.of("active","all","unit","scheduled","keyword")) {
            var query = WorkTodoListSearchVo.builder().mbrAcctIdx(7L).today(LocalDate.parse("2026-10-25")).group("ALL").size(100)
                    .includeInactive(variant.equals("all"))
                    .workUnitUuid(variant.equals("unit") ? "11111111-1111-4111-8111-111111111111" : null)
                    .keyword(variant.equals("keyword") ? "검색" : null)
                    .fromDate(variant.equals("scheduled") ? LocalDate.parse("2026-10-01") : null)
                    .toDate(variant.equals("scheduled") ? LocalDate.parse("2026-10-31") : null).build();
            Map<String,Object> parameters = new HashMap<>();
            parameters.put("req",query);
            parameters.put("fromDate",LocalDate.parse("2026-10-01")); parameters.put("toDate",LocalDate.parse("2026-10-31"));
            parameters.put("fromInstant",OffsetDateTime.parse("2026-10-01T00:00:00+09:00")); parameters.put("beforeInstant",OffsetDateTime.parse("2026-11-01T00:00:00+09:00"));
            parameters.put("timeZone","Asia/Seoul"); parameters.put("bucket","day");
            for (String method : List.of("WorkInsightsMapper.selectGanttRows","WorkInsightsMapper.selectUnitInsights",
                    "WorkInsightsMapper.selectCompletionBuckets","WorkTodoMapper.selectMyWorkTodoSummary","WorkTodoMapper.selectMyWorkTodos","WorkTodoMapper.selectMyWorkTodoCount")) {
                var sql = configuration.getMappedStatement("com.gw.infra.db.mapper.work."+method).getBoundSql(parameters);
                var bindings = sql.getParameterMappings().stream().map(binding -> {
                    assertNotNull(binding.getTypeHandler());
                    return Map.of("property",binding.getProperty(),"type",binding.getJavaType().getName());
                }).toList();
                exported.put(variant+"."+method.substring(method.indexOf('.')+1),Map.of("sql",sql.getSql(),"bindings",bindings));
            }
        }
        Path destination=Path.of("build","insights-sql.json");
        Files.createDirectories(destination.getParent());
        new ObjectMapper().writeValue(destination.toFile(),exported);
    }
}
