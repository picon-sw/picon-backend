package cloud.picon.backend.domain.review.dto;

import java.time.LocalDateTime;
import tools.jackson.databind.node.ArrayNode;

public record ReviewResponse(long reviewId, int rating, ArrayNode reviewTags, String reviewContent,
                             LocalDateTime createdAt) {
}
