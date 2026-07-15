package com.bridgeinspection.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class DatabaseBootstrapService implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    public DatabaseBootstrapService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.queryForList("SELECT user_id FROM tb_user WHERE password = '__INIT_ADMIN123__'", Integer.class)
                .forEach(userId -> jdbcTemplate.update(
                        "UPDATE tb_user SET password = ? WHERE user_id = ?",
                        passwordEncoder.encode("admin123"),
                        userId
                ));

    }
}
