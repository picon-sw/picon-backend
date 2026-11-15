package cloud.picon.backend.global.config;

import cloud.picon.backend.global.apipayload.handler.SecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityErrorHandler securityErrorHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /error: 처리 못 한 오류가 내부적으로 넘어가는 경로. 막으면 원래 오류 대신 401이 나감
                        .requestMatchers("/api/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/auth/kakao").permitAll()
                        .anyRequest().authenticated()
                )
                // Authorization: Bearer {토큰}을 검증. 실패하면 공통 응답 형식의 401
                .oauth2ResourceServer(oauth2 -> oauth2
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
}