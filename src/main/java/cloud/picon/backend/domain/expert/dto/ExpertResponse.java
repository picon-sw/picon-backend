package cloud.picon.backend.domain.expert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record ExpertResponse(
        Long expertId, String nickname, String profileImage, String representativeImage,
        String introduction, List<ExpertSpecialtyResponse> specialties, List<ExpertRegionResponse> regions,
        int careerYears, BigDecimal rating, int reviewCount, @JsonProperty("isFavorite") boolean isFavorite
) {
}
