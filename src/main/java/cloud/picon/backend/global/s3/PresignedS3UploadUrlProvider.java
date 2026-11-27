package cloud.picon.backend.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@RequiredArgsConstructor
public class PresignedS3UploadUrlProvider implements S3UploadUrlProvider {

    private final ObjectProvider<S3Presigner> presigner;
    private final S3Properties properties;

    @Override
    public String getUploadUrl(String imageKey, String contentType, long contentLength) {
        try {
            if (!StringUtils.hasText(properties.bucket())) {
                throw new IllegalStateException("S3 버킷 설정이 필요합니다.");
            }
            // Content-Type과 Content-Length를 서명에 넣어, 다른 형식이나 크기의 파일은 S3가 거절하게 함
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(imageKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();
            PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                    .signatureDuration(properties.presignedUrlExpiration())
                    .putObjectRequest(objectRequest)
                    .build();
            return presigner.getObject().presignPutObject(request).url().toExternalForm();
        } catch (RuntimeException exception) {
            // Do not propagate SDK messages or causes containing object keys or credentials.
            throw new IllegalStateException("Unable to generate upload URL");
        }
    }
}