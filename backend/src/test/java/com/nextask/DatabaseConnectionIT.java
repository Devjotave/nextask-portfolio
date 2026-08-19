package com.nextask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseConnectionIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void connectsToNexTaskDatabaseWithApplicationLogin() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
        String loginName = jdbcTemplate.queryForObject("SELECT SUSER_SNAME()", String.class);

        assertThat(databaseName).isEqualTo("nextask");
        assertThat(loginName).isEqualTo("nextask_app");
    }
}

