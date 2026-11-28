package cloud.picon.backend.global.s3;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** Local placeholder URLs only; this host does not accept uploads. */
public class LocalFakeUploadUrlProvider implements S3UploadUrlProvider {

    @Override
    public String getUploadUrl(String imageKey, String contentType, long contentLength) {
        try {
            return "https://local.invalid/upload/" + UriUtils.encodePath(imageKey, StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unable to generate local upload URL");
        }
    }
}