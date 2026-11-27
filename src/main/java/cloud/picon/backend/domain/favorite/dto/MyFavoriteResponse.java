package cloud.picon.backend.domain.favorite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record MyFavoriteResponse(
        Long expertId, String nickname,
        @Schema(nullable = true, types = {"string", "null"}) String profileImage,
        @Schema(nullable = true, types = {"string", "null"}) String introduction,
        @Schema(nullable = true, types = {"integer", "null"}) Integer startingPrice,
        BigDecimal rating, int reviewCount, @JsonProperty("isFavorite") boolean isFavorite
) {
}
