package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.domain.expert.dto.ExpertImageData;
import cloud.picon.backend.domain.expert.dto.ExpertImageResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertImageService {

    private static final Pattern URL_PREFIX = Pattern.compile("^(?:https?://|//)", Pattern.CASE_INSENSITIVE);

    private final ExpertImageQueryService queryService;
    private final S3ImageUrlProvider imageUrlProvider;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<ExpertImageResponse> getImages(long expertId, Specialty specialty, int page, int size) {
        var images = queryService.getImages(expertId, specialty, page, size);
        // 페이지 전체를 먼저 검증하여 잘못된 저장값은 하나도 서명하지 않는다.
        images.forEach(this::validateImageKey);
        Map<String, String> urls = new HashMap<>();
        var content = images.stream().map(image -> new ExpertImageResponse(image.imageId(),
                imageUrl(image, urls),
                image.specialty(), image.createdAt().atOffset(ZoneOffset.UTC))).toList();
        return PageResponse.of(content, page, size, images.getTotalElements());
    }

    private String imageUrl(ExpertImageData image, Map<String, String> urls) {
        try {
            return urls.computeIfAbsent(image.imageKey(), imageUrlProvider::getImageUrl);
        } catch (RuntimeException e) {
            // SDK 예외 메시지에도 객체 키·URL이 섞일 수 있으므로 원문·cause를 전달하지 않는다.
            log.error("포트폴리오 이미지 URL 생성 실패: imageId={}, type={}", image.imageId(), e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void validateImageKey(ExpertImageData image) {
        String key = image.imageKey();
        if (key == null || key.codePoints().allMatch(codePoint -> Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint) || codePoint == 0x85)
                || ExpertImageKey.hasUrlPrefix(key, URL_PREFIX)) {
            log.error("포트폴리오 이미지 키 무결성 오류: imageId={}", image.imageId());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
