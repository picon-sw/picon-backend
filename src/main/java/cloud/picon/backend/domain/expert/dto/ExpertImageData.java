package cloud.picon.backend.domain.expert.dto;

import cloud.picon.backend.domain.expert.entity.Specialty;

import java.time.LocalDateTime;

public record ExpertImageData(Long imageId, String imageKey, Specialty specialty, LocalDateTime createdAt) {
}
