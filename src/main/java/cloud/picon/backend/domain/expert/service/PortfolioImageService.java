package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.PortfolioImagePageResponse;
import cloud.picon.backend.domain.expert.dto.PortfolioImageResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class PortfolioImageService {

    private final PortfolioImageQueryService portfolioImageQueryService;
    private final S3ImageUrlProvider imageUrlProvider;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PortfolioImagePageResponse getPortfolioImages(Specialty specialty, int page, int size) {
        var images = portfolioImageQueryService.getPortfolioImages(specialty, page, size);
        return PortfolioImagePageResponse.from(images.map(image -> new PortfolioImageResponse(
                image.imageId(), image.expertId(), image.expertNickname(),
                imageUrlProvider.getImageUrl(image.imageKey()), image.specialty())));
    }
}
