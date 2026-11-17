package cloud.picon.backend.global.s3;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3ImageUrlProviderTest {

    @Test
    @SuppressWarnings("unchecked")
    void signsGetUrlForTenMinutesAndEncodesObjectKeyWithoutNetworkAccess() {
        // 고정된 가짜 테스트 자격 증명이며 AWS에 연결하지 않는다.
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .build()) {
            ObjectProvider<S3Presigner> bean = mock(ObjectProvider.class);
            when(bean.getObject()).thenReturn(presigner);
            var provider = new S3ImageUrlProvider(bean,
                    new S3Properties("test-bucket", "ap-northeast-2", Duration.ofMinutes(10)));

            String url = provider.getImageUrl("portfolio/사진 a+b.jpg");

            assertThat(url).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/portfolio/")
                    .contains("%EC%82%AC%EC%A7%84%20a%2Bb.jpg")
                    .contains("X-Amz-Expires=600")
                    .contains("X-Amz-Signature=");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingBucketFailsBeforeCredentialsAreResolved() {
        ObjectProvider<S3Presigner> bean = mock(ObjectProvider.class);
        var provider = new S3ImageUrlProvider(bean,
                new S3Properties("", "", Duration.ofMinutes(10)));
        assertThatIllegalStateException().isThrownBy(() -> provider.getImageUrl("image.jpg"));
        verifyNoInteractions(bean);
    }
}
