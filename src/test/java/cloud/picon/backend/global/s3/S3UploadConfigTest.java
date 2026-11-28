package cloud.picon.backend.global.s3;

import cloud.picon.backend.global.config.S3Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class S3UploadConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(S3Config.class)
            .withPropertyValues("app.s3.presigned-url-expiration=10m");

    @Test
    void realUploadProviderIsDefaultWithoutCreatingPresigner() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(S3UploadUrlProvider.class);
            assertThat(context.getBean(S3UploadUrlProvider.class))
                    .isInstanceOf(PresignedS3UploadUrlProvider.class);
            assertThat(context.getBeanFactory().containsSingleton("s3Presigner")).isFalse();
        });
    }

    @Test
    void localFakeModeUsesFakeUploadProviderWithoutAwsBeans() {
        runner.withPropertyValues("spring.profiles.active=local", "app.s3.fake-image-url=true")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(S3UploadUrlProvider.class);
                    assertThat(context.getBean(S3UploadUrlProvider.class))
                            .isInstanceOf(LocalFakeUploadUrlProvider.class);
                    assertThat(context).doesNotHaveBean(S3Presigner.class);
                    assertThat(context.getBean(S3UploadUrlProvider.class)
                            .getUploadUrl("requests/1/a b.jpg", "image/jpeg", 1))
                            .isEqualTo("https://local.invalid/upload/requests/1/a%20b.jpg");
                });
    }
}