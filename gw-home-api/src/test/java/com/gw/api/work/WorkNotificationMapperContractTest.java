package com.gw.api.work;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.infra.db.config.MyBatisConfig;
import com.gw.share.vo.work.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WorkNotificationMapperContractTest {
    @Test void parseEveryStatementAndExportActualBoundSqlForPostgresSmokeTest() throws Exception {
        var configuration=new MyBatisConfig().sqlSessionFactory(mock(DataSource.class)).getConfiguration();
        Map<String,Object> params=new HashMap<>();
        params.put("s",WrkNtfStgVo.builder().mbrAcctIdx(7L).rcptEmail("me@example.test").ver(1L).build());
        params.put("r",WrkTodoRmdVo.builder().mbrAcctIdx(7L).wrkTodoIdx(1L).build());
        params.put("req",WorkTodoListSearchVo.builder().mbrAcctIdx(7L).today(LocalDate.parse("2026-10-25")).includeInactive(false).build());
        Map<String,Object> exported=new TreeMap<>();
        String prefix="com.gw.infra.db.mapper.work.WorkNotificationMapper.";
        for(String name:configuration.getMappedStatementNames().stream().filter(value -> value.startsWith(prefix)).toList()) {
            var statement=configuration.getMappedStatement(name);
            var sql=statement.getBoundSql(params);
            assertFalse(sql.getSql().contains("<include"));
            var bindings=sql.getParameterMappings().stream().map(parameter -> {
                assertNotNull(parameter.getTypeHandler());
                return Map.of("property",parameter.getProperty(),"type",parameter.getJavaType().getName());
            }).toList();
            exported.put(name.substring(prefix.length()),Map.of("sql",sql.getSql(),"bindings",bindings));
        }
        assertTrue(exported.size()>=25);
        assertTrue(configuration.getMappedStatement(prefix+"selectClaimDeliveries").isDirtySelect());
        Path destination=Path.of("build","notification-sql.json");
        Files.createDirectories(destination.getParent());
        new ObjectMapper().writeValue(destination.toFile(),exported);
    }
}
