package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;

public record ExpertSearchCondition(
        String keyword, Specialty specialty, Long photoStyleId, Long regionCode,
        Integer minPrice, Integer maxPrice, Integer minCareerYears,
        ExpertSort sort, int page, int size
) {
    public ExpertSearchCondition {
        keyword = keyword == null ? "" : keyword.strip();
        if (page < 0 || size < 1 || size > 100 || sort == null
                || (minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)
                || (minPrice != null && maxPrice != null && minPrice > maxPrice)
                || (minCareerYears != null && minCareerYears < 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
