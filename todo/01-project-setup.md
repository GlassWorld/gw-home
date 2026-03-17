# 01. Project Setup

## 목표
Gradle 멀티모듈 구조 초기화 + 공통 설정 파일 생성

## 생성 파일

```
gw-home/
├── settings.gradle
├── build.gradle                  (root)
├── gradle/
│   └── libs.versions.toml
├── share/
│   └── build.gradle
├── api/
│   ├── build.gradle
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── application-prod.yml
└── infra-db/
    └── build.gradle
```

## 상세 스펙

### settings.gradle
```groovy
rootProject.name = 'gw-home'
include 'share', 'api', 'infra-db'
```

### libs.versions.toml (주요 의존성)
```
spring-boot = "3.3.x"
java = "17"
mybatis-spring-boot = "3.0.x"
postgresql = "latest"
lombok = "latest"
```

### build.gradle (root)
- java 17
- Spring Boot 플러그인 공통 적용
- 공통 의존성: lombok, spring-boot-test

### share/build.gradle
```
dependencies:
  - spring-boot-starter
  - lombok
```

### api/build.gradle
```
dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-security
  - spring-boot-starter-validation
  - jjwt (JWT)
  - project(':share')
  - project(':infra-db')
```

### infra-db/build.gradle
```
dependencies:
  - mybatis-spring-boot-starter
  - postgresql
  - project(':api')
```

### application.yml (api)
```yaml
spring:
  profiles:
    active: local
server:
  port: 8080
```

### application-local.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gw_home
    username: ${DB_USERNAME:gw}
    password: ${DB_PASSWORD:gw}
    driver-class-name: org.postgresql.Driver
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 100
    default-statement-timeout: 30
```

## 완료 체크

- [x] settings.gradle 생성
- [x] root build.gradle 생성
- [x] libs.versions.toml 생성
- [x] share/build.gradle 생성
- [x] api/build.gradle 생성
- [x] infra-db/build.gradle 생성
- [x] application.yml 생성
- [x] application-local.yml 생성
- [x] `./gradlew build` 성공
