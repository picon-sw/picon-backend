package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.*;
import cloud.picon.backend.global.apipayload.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpertService {

    private final ExpertQueryService queryService;
    private final ExpertProfileImageValidator profileImageValidator;
    private final ExpertImageUrls imageUrls;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<ExpertResponse> search(ExpertSearchCondition condition, long memberId) {
        ExpertSearchData data = queryService.search(condition, memberId);
        // 잘못된 프로필 URL을 하나라도 발견하면 서명 전에 요청 전체를 실패시킨다.
        for (ExpertData expert : data.content()) {
            profileImageValidator.validate(expert.expertId(), expert.profileImage());
        }
        Map<String, String> urls = new HashMap<>();
        var content = data.content().stream().map(expert -> new ExpertResponse(
                expert.expertId(), expert.nickname(), imageUrls.resolve(expert.profileImage(), urls),
                imageUrls.resolve(expert.representativeImageKey(), urls), expert.introduction(), expert.specialties(),
                expert.regions(), expert.careerYears(), expert.rating(), expert.reviewCount(), expert.isFavorite())).toList();
        return PageResponse.of(content, data.page(), data.size(), data.totalElements());
    }
}
