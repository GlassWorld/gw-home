package com.gw.api.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.infra.db.config.MyBatisConfig;
import com.gw.share.vo.life.LifeTodoSearchVo;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LifeTodoMapperContractTest {
    @Test void exportActualBoundSqlForReadFiltersAndMutations() throws Exception {
        var configuration = new MyBatisConfig().sqlSessionFactory(mock(DataSource.class)).getConfiguration();
        Map<String, Object> exported = new TreeMap<>();
        var methods = List.of("selectTodos", "selectCount", "selectSummary", "selectTodo", "lockOwner", "insertTodo", "updateTodo",
                "updateCompletion", "deleteTodo", "selectCategories", "selectCategory", "categoryNameExists", "insertCategory", "updateCategory", "clearCategory", "deleteCategory");
        for (String variant : List.of("all", "today", "category", "uncategorized", "keyword", "rename")) {
            var search = LifeTodoSearchVo.builder().mbrAcctIdx(7L).today(LocalDate.parse("2026-10-25")).size(20).group(variant.equals("today") ? "TODAY" : "ALL")
                    .categoryUuid(variant.equals("category") ? "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb" : null)
                    .uncategorized(variant.equals("uncategorized")).keyword(variant.equals("keyword") ? "%" : null).build();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("req", search); parameters.put("today", search.getToday()); parameters.put("owner", 7L);
            parameters.put("exceptUuid", variant.equals("rename") ? "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb" : null);
            for (String method : methods) {
                var statement = configuration.getMappedStatement("com.gw.infra.db.mapper.life.LifeTodoMapper." + method).getBoundSql(parameters);
                var bindings = statement.getParameterMappings().stream().map(binding -> {
                    assertNotNull(binding.getTypeHandler());
                    return Map.of("property", binding.getProperty(), "type", binding.getJavaType().getName());
                }).toList();
                exported.put(variant + "." + method, Map.of("sql", statement.getSql(), "bindings", bindings));
            }
        }
        var destination = Path.of("build", "life-todo-sql.json");
        Files.createDirectories(destination.getParent());
        new ObjectMapper().writeValue(destination.toFile(), exported);
    }
}
