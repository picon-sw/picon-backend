package cloud.picon.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

public record ReviewCreateRequest(@NotNull @Schema(type = "integer", minimum = "1", maximum = "5") JsonNode rating,
                                  @Schema(implementation = String[].class, nullable = true)
                                  ArrayNode reviewTags, String reviewContent) {
}
