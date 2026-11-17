package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.domain.expert.entity.Specialty;

public record PortfolioImageResponse(
        Long imageId,
        Long expertId,
        String expertNickname,
        String imageUrl,
        Specialty specialty
) {
}
