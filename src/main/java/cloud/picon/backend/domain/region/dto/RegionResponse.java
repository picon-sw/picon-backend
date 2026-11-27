package cloud.picon.backend.domain.region.dto;

public record RegionResponse(Long regionCode, String firstDepth, String secondDepth, int expertCount) {
}