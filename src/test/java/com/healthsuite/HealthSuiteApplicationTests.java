package com.healthsuite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HealthSuiteApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test — ensures the entire Spring context boots without errors
    }
}
