# 01. Project Setup

## 목표
Gradle 멀티모듈 구조 초기화 + 공통 설정 파일 생성

## 생성 파일

```
gw-home/
├── gradle.properties              (projectName 정의)
├── settings.gradle
├── build.gradle                  (root)
├── gradle/
│   └── libs.versions.toml
├── gw-home-share/
│   └── build.gradle
├── gw-home-api/
│   ├── build.gradle
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       └── application-prod.yml
└── gw-home-infra-db/
    └── build.gradle
```

## 상세 스펙

### settings.gradle
```groovy
def projectName = providers.gradleProperty("projectName").getOrElse("gw-home")
rootProject.name = projectName
include "${projectName}-share", "${projectName}-api", "${projectName}-infra-db"
```

### gradle.properties
```properties
projectName=gw-home
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

### `{project}-share/build.gradle`
```
dependencies:
  - spring-boot-starter
  - lombok
```

### `{project}-api/build.gradle`
```
dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-security
  - spring-boot-starter-validation
  - jjwt (JWT)
  - project(':{project}-share')
  - project(':{project}-infra-db')
```

### `{project}-infra-db/build.gradle`
```
dependencies:
  - mybatis-spring-boot-starter
  - postgresql
  - project(':{project}-share')
```

### application.yml (`{project}-api`)
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
- [x] gradle.properties 생성
- [x] root build.gradle 생성
- [x] libs.versions.toml 생성
- [x] `{project}-share/build.gradle` 생성
- [x] `{project}-api/build.gradle` 생성
- [x] `{project}-infra-db/build.gradle` 생성
- [x] application.yml 생성
- [x] application-local.yml 생성
- [x] `./gradlew build` 성공
- [x] `./gradlew :{project}-api:bootRun` 명령 형식 반영
