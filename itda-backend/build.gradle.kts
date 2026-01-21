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

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
    }
}

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // MyBatis + MySQL
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5")
    runtimeOnly("com.mysql:mysql-connector-j")

    // JWT (auth0)
    implementation("com.auth0:java-jwt:4.3.0")

    // Swagger (springdoc-openapi)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Redis (Refresh Token)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // DevTools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Dotenv (.env 파일 자동 로딩)
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Vertex AI (Gemini)
    implementation("com.google.cloud:google-cloud-vertexai:1.0.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
