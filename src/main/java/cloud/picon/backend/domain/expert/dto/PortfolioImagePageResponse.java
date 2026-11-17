package cloud.picon.backend.domain.expert.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PortfolioImagePageResponse(
        List<PortfolioImageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static PortfolioImagePageResponse from(Page<PortfolioImageResponse> page) {
        return new PortfolioImagePageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(),
                (long) page.getNumber() + 1 >= page.getTotalPages());
    }
}
