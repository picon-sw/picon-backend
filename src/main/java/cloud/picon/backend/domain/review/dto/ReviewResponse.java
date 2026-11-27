package cloud.picon.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.node.ArrayNode;

import java.time.OffsetDateTime;

public record ReviewResponse(long reviewId, int rating, ArrayNode reviewTags, String reviewContent,
                             @Schema(example = "2026-11-26T09:00:00Z") OffsetDateTime createdAt) {
}
