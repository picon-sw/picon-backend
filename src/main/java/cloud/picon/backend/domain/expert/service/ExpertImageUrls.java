package cloud.picon.backend.domain.expert.service;

import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExpertImageUrls {

    private final S3ImageUrlProvider imageUrlProvider;

    public String resolve(String key, Map<String, String> urls) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return urls.computeIfAbsent(key, imageUrlProvider::getImageUrl);
    }
}
