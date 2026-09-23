package com.fawry.lms.config;

import tools.jackson.databind.json.JsonMapper;
import com.fawry.lms.common.ApiErrorResponse;
import com.fawry.lms.security.JwtAuthFilter;
import com.fawry.lms.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthFilter jwtAuthFilter,
                        JsonMapper objectMapper) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .logout(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, exception) -> writeError(
                                                                response, objectMapper, HttpStatus.UNAUTHORIZED,
                                                                "Authentication is required to access this resource."))
                                                .accessDeniedHandler((request, response, exception) -> writeError(
                                                                response, objectMapper, HttpStatus.FORBIDDEN,
                                                                "You do not have permission to perform this action.")))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        JwtTokenProvider jwtTokenProvider(
                        @Value("${JWT_SECRET}") String jwtSecret,
                        @Value("${jwt.access-token-expiration:PT15M}") Duration accessTokenLifetime,
                        @Value("${jwt.refresh-token-expiration:P7D}") Duration refreshTokenLifetime) {
                return new JwtTokenProvider(jwtSecret, accessTokenLifetime, refreshTokenLifetime);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        private static void writeError(
                        jakarta.servlet.http.HttpServletResponse response,
                        JsonMapper objectMapper,
                        HttpStatus status,
                        String message) throws IOException {
                response.setStatus(status.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(status, message));
        }
}