package cloud.picon.backend.domain.expert.code;

import cloud.picon.backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExpertErrorCode implements BaseErrorCode {

    EXPERT_NOT_FOUND(HttpStatus.NOT_FOUND, "EXPERT_NOT_FOUND", "전문가 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
