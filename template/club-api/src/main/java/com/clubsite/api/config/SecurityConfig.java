package com.clubsite.api.config;

import com.clubsite.api.auth.JwtAuthFilter;
import com.clubsite.api.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Access rules are path conventions, so a new feature module needs no change
 * here as long as it follows them:
 *
 *   GET  /api/v1/{feature}/**        public, anyone browsing the site
 *   ANY  /api/v1/admin/{feature}/**  SUPER_ADMIN or EDITOR (GET also VIEWER)
 *   ANY  /api/v1/admin/users/**      SUPER_ADMIN only
 *   anything else                    needs a valid access token
 */
@Configuration
public class SecurityConfig {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String EDITOR = "EDITOR";
    private static final String VIEWER = "VIEWER";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 (not Spring's default 403) for a missing/expired token, so
                // the client knows to try its refresh token before giving up.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()

                        .requestMatchers("/api/v1/admin/users/**").hasRole(SUPER_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").hasAnyRole(SUPER_ADMIN, EDITOR, VIEWER)
                        .requestMatchers("/api/v1/admin/**").hasAnyRole(SUPER_ADMIN, EDITOR)

                        // Must come after the admin rules: public read access to every other feature.
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()

                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
