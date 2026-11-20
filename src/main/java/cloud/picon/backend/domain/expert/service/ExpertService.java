package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.*;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertService {

    private static final Pattern URL_PREFIX = Pattern.compile("^(?:[a-z][a-z0-9+.-]*://|//)", Pattern.CASE_INSENSITIVE);

    private final ExpertQueryService queryService;
    private final S3ImageUrlProvider imageUrlProvider;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<ExpertResponse> search(ExpertSearchCondition condition, long memberId) {
        ExpertSearchData data = queryService.search(condition, memberId);
        // 잘못된 프로필 URL을 하나라도 발견하면 서명 전에 요청 전체를 실패시킨다.
        for (ExpertData expert : data.content()) {
            String image = expert.profileImage();
            if (image != null && URL_PREFIX.matcher(image.strip()).find()) {
                // URL에는 토큰 등이 포함될 수 있으므로 실제 값은 로그에 남기지 않는다.
                log.error("S3 키가 아닌 프로필 이미지 저장값 발견: expertId={}", expert.expertId());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        }
        Map<String, String> urls = new HashMap<>();
        var content = data.content().stream().map(expert -> new ExpertResponse(
                expert.expertId(), expert.nickname(), imageUrl(expert.profileImage(), urls),
                imageUrl(expert.representativeImageKey(), urls), expert.introduction(), expert.specialties(),
                expert.regions(), expert.careerYears(), expert.rating(), expert.reviewCount(), expert.isFavorite())).toList();
        return PageResponse.of(content, data.page(), data.size(), data.totalElements());
    }

    private String imageUrl(String key, Map<String, String> urls) {
        if (key == null || key.isBlank()) {
            return null;
        }
        // 객체 키는 그대로 유지하고 같은 요청에서 중복된 키의 서명만 재사용한다.
        return urls.computeIfAbsent(key, imageUrlProvider::getImageUrl);
    }
}
