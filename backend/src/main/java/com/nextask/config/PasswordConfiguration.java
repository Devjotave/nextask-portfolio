package com.nextask.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class PasswordConfiguration {

    private static final String PBKDF2_ID = "pbkdf2@SpringSecurity_v5_8";

    @Bean
    PasswordEncoder passwordEncoder() {
        PasswordEncoder pbkdf2 = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        return new DelegatingPasswordEncoder(
                PBKDF2_ID,
                Map.of(PBKDF2_ID, pbkdf2));
    }
}
