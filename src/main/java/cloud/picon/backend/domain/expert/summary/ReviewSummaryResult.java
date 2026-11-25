package cloud.picon.backend.domain.expert.summary;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewSummaryResult(String status, @JsonProperty("summary_text") String summaryText) {
}
