package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.domain.expert.entity.Specialty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record ExpertImageResponse(Long imageId, String imageUrl, Specialty specialty,
                                  @Schema(example = "2026-11-21T20:13:32Z") OffsetDateTime createdAt) {
}
