package cloud.picon.backend.domain.expert.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpertDetailData(
        Long expertId, String nickname, String profileImageKey, String introduction, int careerYears,
        List<ExpertSpecialtyResponse> specialties, List<ExpertRegionResponse> regions,
        List<ExpertPhotoStyleResponse> photoStyles, BigDecimal rating, int reviewCount,
        String reviewSummary, boolean isFavorite
) {
}
