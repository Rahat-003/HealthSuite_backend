package com.healthsuite;

import com.healthsuite.support.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class HealthSuiteApplicationIT {

    @Autowired
    private Flyway flyway;

    @Test
    void contextLoadsAndAllMigrationsApply() {
        // Context startup already proves Hibernate's ddl-auto=validate accepted the schema
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }
}
