package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.domain.expert.entity.Specialty;

public record ExpertSpecialtyResponse(Specialty name, int basePrice) {
}
