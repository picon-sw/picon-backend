package cloud.picon.backend.global.jwt;

import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginMemberIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMemberId.class) && parameter.getParameterType() == Long.class;
    }

    @Override
    public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                NativeWebRequest request, WebDataBinderFactory binderFactory) {
        if (!(request.getUserPrincipal() instanceof JwtAuthenticationToken authentication)
                || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            String subject = authentication.getToken().getSubject();
            if (subject != null && subject.matches("[0-9]+")) {
                long memberId = Long.parseLong(subject);
                if (memberId > 0) {
                    return memberId;
                }
            }
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
