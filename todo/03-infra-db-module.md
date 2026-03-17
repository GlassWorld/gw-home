# 03. Infra-DB Module

## 목표
MyBatis 설정, DataSource 설정, DDL 초기화 스크립트 구조 생성

## 생성 파일

```
infra-db/src/main/
├── java/com/gw/infra/db/
│   └── config/
│       ├── DataSourceConfig.java
│       └── MyBatisConfig.java
└── resources/
    ├── mapper/               (비어있음, 도메인 작업 시 채움)
    └── sql/
        ├── ddl/
        │   └── .gitkeep
        └── dml/
            └── .gitkeep
```

## 상세 스펙

### DataSourceConfig
```java
@Configuration
public class DataSourceConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

### MyBatisConfig
```java
@Configuration
@MapperScan("com.gw.api.**.mapper")
public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/**/*.xml")
        );
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.setDefaultFetchSize(100);
        config.setDefaultStatementTimeout(30);
        factory.setConfiguration(config);
        return factory.getObject();
    }
}
```

### sql/ddl 디렉토리 구조 (도메인 작업 시 채움)
```
sql/ddl/
├── account/
├── auth/
├── profile/
├── board/
├── comment/
├── file/
├── tag/
├── favorite/
└── admin/
```

## 완료 체크

- [x] DataSourceConfig 생성
- [x] MyBatisConfig 생성
- [x] mapper/ 디렉토리 구조 생성
- [x] sql/ddl/, sql/dml/ 디렉토리 생성
- [x] `./gradlew :api:bootRun` DB 연결 성공 확인
