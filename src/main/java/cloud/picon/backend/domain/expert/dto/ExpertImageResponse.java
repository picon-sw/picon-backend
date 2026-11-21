package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.domain.expert.entity.Specialty;

import java.time.LocalDateTime;

public record ExpertImageResponse(Long imageId, String imageUrl, Specialty specialty, LocalDateTime createdAt) {
}
