package cloud.picon.backend.global.apipayload.code;

import org.springframework.http.HttpStatus;

// 공통·기능별 오류 코드 enum이 모두 구현
public interface BaseErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}