package cloud.picon.backend.global.apipayload.handler;

import cloud.picon.backend.global.apipayload.code.BaseErrorCode;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

// Spring Security 필터 단계의 401·403은 Controller까지 오지 않아서
// GlobalExceptionHandler가 잡지 못함. 여기서 같은 형식으로 응답
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    // 메시지는 enum의 고정 문자열이라 사용자 입력이 섞이지 않음
    private void write(HttpServletResponse response, BaseErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"isSuccess\":false,\"code\":\"" + code.getCode()
                        + "\",\"message\":\"" + code.getMessage() + "\"}");
    }
}