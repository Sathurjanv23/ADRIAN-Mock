package com.nova.emergency.config;

import com.nova.emergency.security.JwtFilter;
import com.nova.emergency.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtFilter jwtFilter, UserDetailsServiceImpl userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Value("${nova.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/auth/oauth2/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Public citizen emergency report submission (no auth required for anonymous
                        // SOS)
                        .requestMatchers(HttpMethod.POST, "/api/incidents/report", "/api/incidents").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/incidents/media/**").permitAll()
                        // Admin endpoints strictly require ROLE_ADMIN
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // Rescue-team-specific action endpoints
                        .requestMatchers(HttpMethod.POST, "/api/incidents/*/accept")
                        .hasAnyAuthority("ROLE_RESCUE_TEAM", "ROLE_OFFICER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/incidents/*/status")
                        .hasAnyAuthority("ROLE_RESCUE_TEAM", "ROLE_OFFICER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/incidents/*/status")
                        .hasAnyAuthority("ROLE_RESCUE_TEAM", "ROLE_OFFICER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/rescue-teams/*/status")
                        .hasAnyAuthority("ROLE_RESCUE_TEAM", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/rescue-teams/assign")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN")
                        // Citizens + all authenticated roles can READ incidents and alerts
                        // (citizens need this to track their own emergency status)
                        .requestMatchers(HttpMethod.GET, "/api/incidents/**").hasAnyAuthority(
                                "ROLE_CITIZEN", "ROLE_OFFICER", "ROLE_ADMIN", "ROLE_RESCUE_TEAM", "ROLE_HOSPITAL")
                        .requestMatchers(HttpMethod.GET, "/api/alerts/**").hasAnyAuthority(
                                "ROLE_CITIZEN", "ROLE_OFFICER", "ROLE_ADMIN", "ROLE_RESCUE_TEAM", "ROLE_HOSPITAL")
                        // Other incident mutations require authenticated operational roles
                        .requestMatchers("/api/incidents/**").hasAnyAuthority(
                                "ROLE_CITIZEN", "ROLE_OFFICER", "ROLE_ADMIN", "ROLE_RESCUE_TEAM", "ROLE_HOSPITAL")
                        // Operational endpoints restricted to response/command roles only (not citizens)
                        .requestMatchers(HttpMethod.GET,
                                "/api/rescue-teams/**",
                                "/api/hospitals/**",
                                "/api/resources/**",
                                "/api/analytics/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN", "ROLE_RESCUE_TEAM", "ROLE_HOSPITAL")
                        .requestMatchers(HttpMethod.GET, "/api/predictions/**", "/api/notifications/**",
                                "/api/events/**")
                        .authenticated()
                        // ─── ADRN Relief Logistics ─────────────────────────────────
                        // Food sources — all authenticated roles can read; only OFFICER/ADMIN can write
                        .requestMatchers(HttpMethod.GET, "/api/food-sources/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/food-sources/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/food-sources/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/food-sources/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN")
                        // Relief requests — read for all authenticated; write for OFFICER/ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/relief-requests/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/relief-requests/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN", "ROLE_RESCUE_TEAM")
                        .requestMatchers(HttpMethod.PUT, "/api/relief-requests/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN")
                        // Relief missions — read for all authenticated; write for OFFICER/ADMIN/RESCUE_TEAM
                        .requestMatchers(HttpMethod.GET, "/api/relief-missions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/relief-missions/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/relief-missions/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN", "ROLE_RESCUE_TEAM")
                        // AI endpoints — all authenticated operational roles
                        .requestMatchers("/api/ai/**")
                        .hasAnyAuthority("ROLE_OFFICER", "ROLE_ADMIN", "ROLE_RESCUE_TEAM", "ROLE_HOSPITAL")
                        // All other operations require valid authenticated JWT
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required. Please log in.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"Access denied. Insufficient privileges.\"}");
                        }))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
