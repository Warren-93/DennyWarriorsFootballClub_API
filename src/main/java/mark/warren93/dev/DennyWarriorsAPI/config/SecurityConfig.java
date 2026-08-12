package mark.warren93.dev.DennyWarriorsAPI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Always allow CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth endpoints are open
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Public read endpoints — anyone can browse the site.
                        .requestMatchers(HttpMethod.GET, "/api/v1/squad/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/fixtures/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/standings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/history/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/media/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stats/**").permitAll()

                        // Sync log viewing — any authenticated admin-panel role.
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/sync/logs").hasAnyRole("SUPER_ADMIN", "EDITOR", "VIEWER")

                        // User management is super-admin only.
                        .requestMatchers("/api/v1/admin/users/**").hasRole("SUPER_ADMIN")

                        // All other admin/write actions (squad/news CRUD, manual sync trigger,
                        // and anything added in later phases) require SUPER_ADMIN or EDITOR.
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "EDITOR")

                        // Default deny — anything unmatched needs at least a valid token.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
