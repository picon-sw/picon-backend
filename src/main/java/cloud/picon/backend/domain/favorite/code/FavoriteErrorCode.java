package cloud.picon.backend.domain.favorite.code;

import cloud.picon.backend.global.apipayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FavoriteErrorCode implements BaseErrorCode {
    ALREADY_FAVORITED(HttpStatus.CONFLICT, "ALREADY_FAVORITED", "이미 찜한 전문가입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
