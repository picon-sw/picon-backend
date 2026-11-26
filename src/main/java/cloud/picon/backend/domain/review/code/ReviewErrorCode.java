package cloud.picon.backend.domain.review.code;

import cloud.picon.backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 등록된 리뷰가 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
