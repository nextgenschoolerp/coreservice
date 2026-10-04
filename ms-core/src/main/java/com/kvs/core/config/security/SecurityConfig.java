package com.kvs.core.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
// @EnableWebSecurity
public class SecurityConfig {

    /*
     * @Bean
     * public SecurityFilterChain securityFilterChain(HttpSecurity http) throws
     * Exception {
     * http
     * .csrf().disable()
     * .authorizeRequests()
     * .requestMatchers("/api/v1/user/login", "/api/v1/user/logout").permitAll() //
     * Allow /login endpoint without authentication
     * .anyRequest().authenticated() // Protect other endpoints
     * .and()
     * .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
     * 
     * return http.build();
     * }
     */

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
