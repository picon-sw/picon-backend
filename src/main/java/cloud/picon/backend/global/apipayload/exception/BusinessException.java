package cloud.picon.backend.global.apipayload.exception;

import cloud.picon.backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;

// 서비스 코드에서 규칙 위반을 알릴 때 던지는 예외
// 예: throw new BusinessException(ErrorCode.NOT_FOUND);
@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(BaseErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}