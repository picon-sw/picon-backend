package cloud.picon.backend.domain.expert.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpertData(
        Long expertId, String nickname, String profileImage, String representativeImageKey,
        String introduction, List<ExpertSpecialtyResponse> specialties, List<ExpertRegionResponse> regions,
        int careerYears, BigDecimal rating, int reviewCount, boolean isFavorite
) {
}
