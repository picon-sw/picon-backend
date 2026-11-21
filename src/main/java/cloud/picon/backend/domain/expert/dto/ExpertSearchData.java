package cloud.picon.backend.domain.expert.dto;

import java.util.List;

public record ExpertSearchData(List<ExpertData> content, int page, int size, long totalElements) {
}
