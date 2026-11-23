package cloud.picon.backend.global.config;

import cloud.picon.backend.global.apipayload.handler.SecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Access Token 없이 호출하는 인증 API. 헤더에 만료·잘못된 토큰이 남아 있어도 401이 나지 않도록 헤더를 읽지 않음
    private static final Set<String> TOKEN_FREE_AUTH_PATHS =
            Set.of("/api/auth/kakao", "/api/auth/refresh", "/api/auth/logout");

    // Nginx가 /api 요청만 백엔드로 넘기므로 Swagger도 /api 아래에 둠
    // api-docs.yaml은 springdoc이 따로 여는 YAML 문서 경로라 함께 보호
    private static final String[] SWAGGER_PATHS = {
            "/api/swagger-ui.html", "/api/swagger-ui/**",
            "/api/v3/api-docs/**", "/api/v3/api-docs.yaml/**"};

    private final SecurityErrorHandler securityErrorHandler;

    // Swagger 전용: 공개 도메인이라 아이디·비밀번호(HTTP Basic)로 보호. 꺼져 있으면 모두 거부
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(
            HttpSecurity http,
            @Value("${springdoc.swagger-ui.enabled:false}") boolean swaggerUiEnabled,
            @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled,
            @Value("${app.swagger.username:}") String username,
            @Value("${app.swagger.password:}") String password) throws Exception {
        http
                .securityMatcher(SWAGGER_PATHS)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 화면(swagger-ui)과 문서(api-docs) 중 하나라도 켜져 있으면 인증 후 허용
        if (!swaggerUiEnabled && !apiDocsEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
            return http.build();
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("Swagger를 켜려면 SWAGGER_USERNAME, SWAGGER_PASSWORD가 필요합니다.");
        }

        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername(username).password(encoder.encode(password)).roles("SWAGGER").build());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);

        http
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /error: 처리 못 한 오류가 내부적으로 넘어가는 경로. 막으면 원래 오류 대신 401이 나감
                        .requestMatchers("/api/actuator/health", "/error").permitAll()
                        .requestMatchers(TOKEN_FREE_AUTH_PATHS.toArray(String[]::new)).permitAll()
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