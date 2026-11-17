package cloud.picon.backend.domain.expert.dto;

import java.util.List;

public record ExpertPageResponse(
        List<ExpertResponse> content, int page, int size, long totalElements, long totalPages, boolean last
) {
    public static ExpertPageResponse from(ExpertSearchData data, List<ExpertResponse> content) {
        long totalPages = data.totalElements() / data.size() + (data.totalElements() % data.size() == 0 ? 0 : 1);
        return new ExpertPageResponse(content, data.page(), data.size(), data.totalElements(), totalPages,
                (long) data.page() + 1 >= totalPages);
    }
}
