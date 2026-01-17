# itda-backend 초기 설정 구현 계획서

> **버전**: 1.1  
> **작성일**: 2026-01-17  
> **목적**: itda-backend 프로젝트 초기 구조 설정 (global + auth)

---

## 1. 프로젝트 개요

| 항목 | 값 |
|------|-----|
| Group ID | `com.itda` |
| Artifact ID | `itda-backend` |
| Java Version | 17 |
| Spring Boot | 3.2.5 |
| Build Tool | Gradle (Kotlin DSL) |

### 기술 스택 (MVP)

| 영역 | 기술 |
|------|------|
| Framework | Spring Boot 3.2.5 |
| DB | MySQL + MyBatis |
| 인증 | JWT (auth0/java-jwt) |
| API 문서 | Swagger (springdoc-openapi) |
| AI | Spring AI + Vertex AI Gemini |

### 이번 구현 범위

> [!IMPORTANT]
> **1차 구현: global + auth만 구현**  
> 나머지 도메인은 DB 스키마 확정 후 진행

- ✅ Gradle 프로젝트 셋업
- ✅ global 패키지 (공통 컴포넌트)
- ✅ auth 패키지 (인증)
- ✅ 사용자 테이블 스키마 + Redis 기반 Refresh Token
- ⬜ project, scene, node, object, ai, timeline (스키마 확정 후)

---

## 2. 패키지 구조

```
itda-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/java/com/itda/backend/
│   ├── ItdaBackendApplication.java
│   │
│   ├── auth/                          # ✅ 1차 구현
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── SignupRequest.java     # DTO는 controller 안에
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   └── UserResponse.java
│   │   ├── service/
│   │   │   └── AuthService.java
│   │   ├── repository/
│   │   │   ├── UserMapper.java
│   │   │   └── RefreshTokenRepository.java  # Redis
│   │   └── domain/
│   │       └── User.java
│   │
│   ├── global/                        # ✅ 1차 구현
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── SwaggerConfig.java
│   │   │   ├── MyBatisConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   └── WebConfig.java
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── CustomUserDetails.java
│   │   ├── response/
│   │   │   ├── ApiResponse.java
│   │   │   └── ErrorCode.java
│   │   └── exception/
│   │       ├── BusinessException.java
│   │       ├── GlobalExceptionHandler.java
│   │       └── UnauthorizedException.java
│   │
│   ├── project/                       # ⬜ 나중에 (폴더만)
│   ├── scene/                         # ⬜ 나중에 (폴더만)
│   ├── node/                          # ⬜ 나중에 (폴더만)
│   ├── object/                        # ⬜ 나중에 (폴더만)
│   ├── ai/                            # ⬜ 나중에 (폴더만)
│   └── timeline/                      # ⬜ 나중에 (폴더만)
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── mapper/
│       └── UserMapper.xml
│
└── uploads/                           # 로컬 파일 저장
```

---

## 3. 핵심 컴포넌트

### 3.1 ApiResponse

```java
public class ApiResponse<T> {
    private String code;      // SUCCESS, ACCEPTED, ERROR_CODE
    private String message;
    private T data;
    private Object details;   // 에러 시 상세정보
    
    public static <T> ApiResponse<T> success(T data);
    public static <T> ApiResponse<T> success(String message, T data);
    public static <T> ApiResponse<T> accepted(T data);
    public static <T> ApiResponse<T> error(ErrorCode code, String message);
}
```

### 3.2 ErrorCode

```java
public enum ErrorCode {
    // 인증
    UNAUTHORIZED("인증이 필요합니다"),
    FORBIDDEN("권한이 없습니다"),
    
    // 사용자
    USER_NOT_FOUND("사용자를 찾을 수 없습니다"),
    EMAIL_ALREADY_EXISTS("이미 가입된 이메일입니다"),
    INVALID_PASSWORD("비밀번호가 일치하지 않습니다"),
    
    // 공통
    INVALID_REQUEST("요청 파라미터가 올바르지 않습니다"),
    INTERNAL_ERROR("서버 내부 오류가 발생했습니다");
    
    // 나머지는 해당 도메인 구현 시 추가
}
```

### 3.3 JWT

```java
public class JwtTokenProvider {
    public String createAccessToken(Long userId, String email);
    public String createRefreshToken(Long userId, String email);
    public boolean validateToken(String token);
    public Long getUserIdFromToken(String token);
}
```

### 3.4 Refresh Token 정책

- Access Token TTL: **1일(24h)**
- Refresh Token TTL: **7일**
- 저장소: **Redis**
- 갱신 정책: **Refresh Token rotation** (갱신 시 새 토큰 발급, 기존 토큰 폐기)
- 로그아웃: Refresh Token 폐기 (Access Token은 만료까지 유효)

### 3.5 보안/예외 처리 규칙

- 비밀번호 저장: **BCryptPasswordEncoder** 사용 (평문 저장 금지)
- 에러 응답: `ErrorCode`에 **HTTP Status 매핑** 포함 (GlobalExceptionHandler에서 ResponseEntity로 반환)

### 3.6 User 테이블 스키마 (MVP)

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 4. 의존성 (build.gradle.kts)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.itda"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom("com.google.cloud:libraries-bom:26.51.0")
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.3")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // MyBatis + MySQL
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5")
    runtimeOnly("com.mysql:mysql-connector-j")
    
    // JWT
    implementation("com.auth0:java-jwt:4.3.0")
    
    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Redis (Refresh Token 저장)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // Spring AI (Gemini)
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    implementation("com.google.auth:google-auth-library-oauth2-http")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // DevTools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}
```

---

## 5. 설정 파일

### application.yml (공통)
```yaml
spring:
  profiles:
    active: dev
  application:
    name: itda-backend

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.itda.backend
  configuration:
    map-underscore-to-camel-case: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
  refresh-expiration: 604800000

file:
  upload-dir: ./uploads
```

### application-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/itda_dev?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

spring.ai.vertex.ai.gemini:
  project-id: ${GCP_PROJECT_ID}
  location: ${GCP_LOCATION:us-central1}

logging:
  level:
    com.itda: DEBUG
```

### application-prod.yml
```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

spring.ai.vertex.ai.gemini:
  project-id: ${GCP_PROJECT_ID}
  location: ${GCP_LOCATION}

logging:
  level:
    com.itda: INFO
```

---

## 6. 구현 파일 목록

### Phase 1: 프로젝트 셋업

| 파일 | 설명 |
|------|------|
| `build.gradle.kts` | Gradle 빌드 설정 |
| `settings.gradle.kts` | 프로젝트 설정 |
| `ItdaBackendApplication.java` | 메인 클래스 |
| `application.yml` | 공통 설정 |
| `application-dev.yml` | 개발 환경 설정 |
| `application-prod.yml` | 운영 환경 설정 |

### Phase 2: global 패키지

| 파일 | 설명 |
|------|------|
| `ApiResponse.java` | 공통 응답 포맷 |
| `ErrorCode.java` | 에러 코드 enum |
| `BusinessException.java` | 비즈니스 예외 |
| `GlobalExceptionHandler.java` | 전역 예외 처리 |
| `UnauthorizedException.java` | 인증 예외 |
| `SecurityConfig.java` | Spring Security 설정 |
| `JwtTokenProvider.java` | JWT 생성/검증 |
| `JwtAuthenticationFilter.java` | JWT 인증 필터 |
| `CustomUserDetails.java` | 사용자 인증 정보 |
| `SwaggerConfig.java` | Swagger 설정 |
| `MyBatisConfig.java` | MyBatis 설정 |
| `RedisConfig.java` | Redis 설정 |
| `WebConfig.java` | CORS 등 웹 설정 |

### Phase 3: auth 패키지

| 파일 | 설명 |
|------|------|
| `User.java` | 사용자 도메인 |
| `UserMapper.java` | MyBatis 매퍼 인터페이스 |
| `UserMapper.xml` | MyBatis SQL |
| `AuthService.java` | 인증 서비스 |
| `RefreshTokenRepository.java` | Redis 기반 토큰 저장소 |
| `AuthController.java` | 인증 API 컨트롤러 |
| `SignupRequest.java` | 회원가입 요청 DTO |
| `LoginRequest.java` | 로그인 요청 DTO |
| `LoginResponse.java` | 로그인 응답 DTO |
| `UserResponse.java` | 사용자 정보 DTO |

---

## 7. Auth API 명세 (구현 대상)

| API | 메서드 | 설명 |
|-----|--------|------|
| `/api/auth/signup` | POST | 회원가입 |
| `/api/auth/login` | POST | 로그인 |
| `/api/auth/refresh` | POST | 토큰 재발급 (Rotation) |
| `/api/auth/logout` | POST | 로그아웃 |
| `/api/auth/me` | GET | 내 정보 조회 |
| `/api/auth/me` | PUT | 프로필 수정 |

**토큰 응답 규칙 (MVP)**  
- 로그인/리프레시 응답: `{ accessToken, refreshToken, expiresIn }`  
- 로그아웃: Refresh Token 무효화 (Redis 삭제)

---

## 8. 검증 방법

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 실행
./gradlew bootRun

# Swagger 확인
# http://localhost:8080/swagger-ui.html
```

---

## 9. 환경변수

> [!WARNING]
> **필수 설정**
> - `DB_USERNAME`, `DB_PASSWORD`: MySQL 접속 정보
> - `JWT_SECRET`: JWT 서명 키
> - `GCP_PROJECT_ID`: Vertex AI 프로젝트 (AI 사용 시)
> - `REDIS_HOST`, `REDIS_PORT`: Redis 접속 정보

---

## 10. 추후 작업 (스키마 확정 후)

- [ ] DB 스키마 생성 SQL
- [ ] project 도메인
- [ ] scene 도메인
- [ ] node 도메인
- [ ] object 도메인
- [ ] ai 도메인
- [ ] timeline 도메인
