package cloud.picon.backend.global.apipayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 공통 오류 코드. 기능별 오류는 같은 방식으로 enum을 따로 만들어 BaseErrorCode를 구현
@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "다시 로그인해 주세요."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_AUTH_FAILED", "카카오 인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    ONBOARDING_REQUIRED(HttpStatus.FORBIDDEN, "ONBOARDING_REQUIRED", "온보딩을 먼저 완료해 주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 대상을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다."),
    INVALID_STATUS(HttpStatus.CONFLICT, "INVALID_STATUS", "현재 상태에서는 처리할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),
    KAKAO_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_SERVER_ERROR", "카카오 서버와 통신하지 못했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}