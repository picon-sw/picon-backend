package cloud.picon.backend.global.apipayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK, "OK", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "CREATED", "생성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}