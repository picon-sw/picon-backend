package cloud.picon.backend.global.s3;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 네트워크 없이 서명만 확인 (가짜 자격 증명 사용)
class PresignedS3UploadUrlProviderTest {

    private static S3Presigner presigner() {
        return S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .build();
    }

    private static PresignedS3UploadUrlProvider provider(S3Presigner presigner, String bucket) {
        var beans = new StaticListableBeanFactory();
        beans.addBean("s3Presigner", presigner);
        return new PresignedS3UploadUrlProvider(beans.getBeanProvider(S3Presigner.class),
                new S3Properties(bucket, "ap-northeast-2", Duration.ofMinutes(10)));
    }

    private static String queryValue(String rawQuery, String name) {
        return Arrays.stream(rawQuery.split("&"))
                .filter(pair -> pair.startsWith(name + "="))
                .map(pair -> URLDecoder.decode(pair.substring(name.length() + 1), StandardCharsets.UTF_8))
                .findFirst().orElse("");
    }

    @Test
    void signsContentTypeAndLengthSoS3RejectsOtherFiles() {
        try (var presigner = presigner()) {
            URI url = URI.create(provider(presigner, "test-bucket")
                    .getUploadUrl("requests/7/photo.jpg", "image/jpeg", 1234));

            assertThat(url.getScheme()).isEqualTo("https");
            assertThat(url.getHost()).startsWith("test-bucket.");
            assertThat(url.getPath()).isEqualTo("/requests/7/photo.jpg");
            assertThat(queryValue(url.getRawQuery(), "X-Amz-Expires")).isEqualTo("600");
            assertThat(queryValue(url.getRawQuery(), "X-Amz-SignedHeaders").split(";"))
                    .contains("content-length", "content-type", "host");
            // 브라우저가 보낼 수 없는 체크섬 값을 요구하지 않아야 함
            assertThat(url.getRawQuery().toLowerCase(Locale.ROOT)).doesNotContain("checksum");
        }
    }

    @Test
    void failsWithoutLeakingDetailsWhenBucketIsMissing() {
        try (var presigner = presigner()) {
            assertThatThrownBy(() -> provider(presigner, " ").getUploadUrl("requests/7/a.jpg", "image/jpeg", 1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to generate upload URL")
                    .hasNoCause();
        }
    }
}