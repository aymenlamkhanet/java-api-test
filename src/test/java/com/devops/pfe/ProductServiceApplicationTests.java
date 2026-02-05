package com.devops.pfe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context Tests")
class ProductServiceApplicationTests {

    @Test
    @DisplayName("Should load application context")
    void contextLoads() {
        // Verifies that the Spring context loads successfully
    }
}
