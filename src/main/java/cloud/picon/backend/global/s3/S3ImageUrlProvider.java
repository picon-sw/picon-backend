package cloud.picon.backend.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3ImageUrlProvider {

    private final ObjectProvider<S3Presigner> presigner;
    private final S3Properties properties;

    public String getImageUrl(String imageKey) {
        if (!StringUtils.hasText(properties.bucket())) {
            throw new IllegalStateException("S3 버킷 설정이 필요합니다.");
        }
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(imageKey)
                .build();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(properties.presignedUrlExpiration())
                .getObjectRequest(objectRequest)
                .build();
        return presigner.getObject().presignGetObject(request).url().toExternalForm();
    }
}
