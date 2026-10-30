package com.gw.api.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.infra.db.config.MyBatisConfig;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

class LifeDailyMapperContractTest {
    @Test void exportActualSqlForDatabaseVerification() throws Exception {
        var configuration = new MyBatisConfig().sqlSessionFactory(mock(DataSource.class)).getConfiguration();
        Map<String, Object> exported = new TreeMap<>();
        for (String method : List.of("lockOwner", "selectItems", "selectChecks", "selectItem", "insertItem", "updateTitle", "endItem", "deleteItem", "saveCheck")) {
            var statement = configuration.getMappedStatement("com.gw.infra.db.mapper.life.LifeDailyMapper." + method).getBoundSql(new HashMap<>());
            exported.put(method, Map.of("sql", statement.getSql(), "bindings", statement.getParameterMappings().stream().map(binding -> binding.getProperty()).toList()));
        }
        Files.createDirectories(Path.of("build"));
        new ObjectMapper().writeValue(Path.of("build", "life-daily-sql.json").toFile(), exported);
    }
}
