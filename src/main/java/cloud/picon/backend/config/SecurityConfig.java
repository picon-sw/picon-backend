package cloud.picon.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 허용할 경로 (카카오 로그인 경로는 로그인 기능 구현 시 추가)
                        .requestMatchers("/api/actuator/health").permitAll()
                        .anyRequest().authenticated()
                );
        // JWT 인증 필터는 로그인 기능 구현 시 여기에 추가
        return http.build();
    }
}