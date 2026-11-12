package cloud.picon.backend.global.config;

import cloud.picon.backend.global.apipayload.handler.SecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        // 인증 없이 허용할 경로 (카카오 로그인 경로는 로그인 기능 구현 시 추가)
                        // /error: 처리 못 한 오류가 내부적으로 넘어가는 경로. 막으면 원래 오류 대신 401이 나감
                        .requestMatchers("/api/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                // 인증 실패(401)·권한 없음(403)도 공통 응답 형식으로
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                );
        // JWT 인증 필터는 로그인 기능 구현 시 여기에 추가
        return http.build();
    }
}