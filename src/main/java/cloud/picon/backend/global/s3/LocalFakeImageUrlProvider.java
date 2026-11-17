package cloud.picon.backend.global.s3;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** Local placeholder URLs only; this host does not serve image downloads. */
public class LocalFakeImageUrlProvider implements S3ImageUrlProvider {

    @Override
    public String getImageUrl(String imageKey) {
        try {
            return "https://local.invalid/" + UriUtils.encodePath(imageKey, StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unable to generate local image URL");
        }
    }
}
