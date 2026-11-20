package cloud.picon.backend.global.config;

import cloud.picon.backend.global.apipayload.handler.SecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Access Token 없이 호출하는 인증 API. 헤더에 만료·잘못된 토큰이 남아 있어도 401이 나지 않도록 헤더를 읽지 않음
    private static final Set<String> TOKEN_FREE_AUTH_PATHS =
            Set.of("/api/auth/kakao", "/api/auth/refresh", "/api/auth/logout");

    private final SecurityErrorHandler securityErrorHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /error: 처리 못 한 오류가 내부적으로 넘어가는 경로. 막으면 원래 오류 대신 401이 나감
                        .requestMatchers("/api/actuator/health", "/error").permitAll()
                        .requestMatchers(TOKEN_FREE_AUTH_PATHS.toArray(String[]::new)).permitAll()
                        // Swagger: SWAGGER_ENABLED=true일 때만 열림. 배포 환경에서는 꺼져 있어 404
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Authorization: Bearer {토큰}을 검증. 실패하면 공통 응답 형식의 401
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )
                // 인증 실패(401)·권한 없음(403)도 공통 응답 형식으로
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                );
        return http.build();
    }

    // 인증 API 경로에서는 토큰이 없는 것으로 처리하고, 나머지는 기본 방식으로 헤더에서 토큰을 꺼냄
    private BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return request -> TOKEN_FREE_AUTH_PATHS.contains(request.getRequestURI())
                ? null
                : defaultResolver.resolve(request);
    }
}