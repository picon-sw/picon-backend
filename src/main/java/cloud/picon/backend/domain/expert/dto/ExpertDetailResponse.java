package cloud.picon.backend.domain.expert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record ExpertDetailResponse(
        Long expertId, String nickname, String profileImage, String introduction, int careerYears,
        List<ExpertSpecialtyResponse> specialties, List<ExpertRegionResponse> regions,
        List<ExpertPhotoStyleResponse> photoStyles, BigDecimal rating, int reviewCount,
        String reviewSummary, @JsonProperty("isFavorite") boolean isFavorite
) {
}
