package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.ExpertDetailResponse;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpertDetailService {

    private final ExpertDetailQueryService queryService;
    private final ExpertProfileImageValidator profileImageValidator;
    private final S3ImageUrlProvider imageUrlProvider;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExpertDetailResponse getDetail(long expertId, long memberId) {
        var expert = queryService.getDetail(expertId, memberId);
        String key = expert.profileImageKey();
        profileImageValidator.validate(expertId, key);
        String imageUrl = key == null || key.isBlank() ? null : imageUrlProvider.getImageUrl(key);
        return new ExpertDetailResponse(expert.expertId(), expert.nickname(), imageUrl, expert.introduction(),
                expert.careerYears(), expert.specialties(), expert.regions(), expert.photoStyles(), expert.rating(),
                expert.reviewCount(), expert.reviewSummary(), expert.isFavorite());
    }
}
