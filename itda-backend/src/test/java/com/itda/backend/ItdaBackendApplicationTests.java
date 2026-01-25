package com.itda.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Spring context test disabled: project uses MySQL-only and requires external test DB setup.")
@SpringBootTest
@ActiveProfiles("test")
class ItdaBackendApplicationTests {

    @Test
    void contextLoads() {
        // Just verify that the Spring context loads successfully
    }
}
