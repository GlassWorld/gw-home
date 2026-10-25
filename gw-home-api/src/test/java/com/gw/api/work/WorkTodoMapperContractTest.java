package com.gw.api.work;

import com.gw.infra.db.config.MyBatisConfig;
import com.gw.share.vo.work.WorkTodoListSearchVo;
import java.time.LocalDate;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WorkTodoMapperContractTest {
    @Test
    void runtimeConfigurationParsesAliasesAndBindsEveryInboxFilter() throws Exception {
        var configuration = new MyBatisConfig().sqlSessionFactory(mock(DataSource.class)).getConfiguration();
        var search = WorkTodoListSearchVo.builder().mbrAcctIdx(7L)
                .workUnitUuid("11111111-1111-4111-8111-111111111111")
                .keyword("%_' 검색").group("TODAY").includeInactive(false)
                .today(LocalDate.of(2026, 10, 25)).fromDate(LocalDate.of(2026, 10, 20))
                .toDate(LocalDate.of(2026, 10, 30)).size(20).offset(20L).build();
        var params = Map.of("req", search);
        for (String method : new String[]{"selectMyWorkTodos", "selectMyWorkTodoCount", "selectMyWorkTodoSummary"}) {
            var statement = configuration.getMappedStatement("com.gw.infra.db.mapper.work.WorkTodoMapper." + method);
            var boundSql = statement.getBoundSql(params);
            assertTrue(boundSql.getSql().contains("?"));
            for (var binding : boundSql.getParameterMappings()) {
                assertNotNull(SystemMetaObject.forObject(params).getValue(binding.getProperty()));
                assertNotNull(binding.getTypeHandler());
            }
        }
    }
}
