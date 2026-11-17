package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;

public enum ExpertSort {
    RATING, LATEST;

    public static ExpertSort from(String value) {
        return switch (value) {
            case "rating" -> RATING;
            case "latest" -> LATEST;
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
    }
}
