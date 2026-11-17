package cloud.picon.backend.global.s3;

import cloud.picon.backend.global.config.S3Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(S3Config.class)
            .withPropertyValues("app.s3.presigned-url-expiration=10m");

    @Test
    void realProviderIsDefaultWithoutResolvingAwsCredentialsOrRegion() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(S3ImageUrlProvider.class);
            assertThat(context.getBean(S3ImageUrlProvider.class))
                    .isInstanceOf(PresignedS3ImageUrlProvider.class);
            assertThat(context.containsBean("s3Presigner")).isTrue();
            assertThat(context.getBeanFactory().containsSingleton("s3Presigner")).isFalse();
        });
    }

    @Test
    void explicitFalseAlsoSelectsRealProvider() {
        runner.withPropertyValues("app.s3.fake-image-url=false").run(context -> {
            assertThat(context).hasSingleBean(S3ImageUrlProvider.class);
            assertThat(context.getBean(S3ImageUrlProvider.class))
                    .isInstanceOf(PresignedS3ImageUrlProvider.class);
            assertThat(context.getBeanFactory().containsSingleton("s3Presigner")).isFalse();
        });
    }
    @Test
    void localAloneAndTrueSelectsOnlyFakeWithoutAnyAwsBeansOrSettings() {
        runner.withPropertyValues("spring.profiles.active=local", "app.s3.fake-image-url=true")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(S3ImageUrlProvider.class);
                    assertThat(context.getBean(S3ImageUrlProvider.class))
                            .isInstanceOf(LocalFakeImageUrlProvider.class);
                    assertThat(context).doesNotHaveBean(S3Presigner.class);
                    assertThat(context.getBean(S3ImageUrlProvider.class).getImageUrl("portfolio/test.jpg"))
                            .isEqualTo("https://local.invalid/portfolio/test.jpg");
                });
    }

    @Test
    void localWithFalseUsesRealProvider() {
        runner.withPropertyValues("spring.profiles.active=local", "app.s3.fake-image-url=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(S3ImageUrlProvider.class);
                    assertThat(context.getBean(S3ImageUrlProvider.class))
                            .isInstanceOf(PresignedS3ImageUrlProvider.class);
                });
    }

    @Test
    void localWithoutFlagUsesRealProvider() {
        runner.withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(S3ImageUrlProvider.class);
            assertThat(context.getBean(S3ImageUrlProvider.class))
                    .isInstanceOf(PresignedS3ImageUrlProvider.class);
        });
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"prod", "test", "staging", "local,prod", "prod,local", "local,test"})
    void fakeModeFailsClosedForOtherOrCombinedProfiles(String profiles) {
        runner.withPropertyValues("spring.profiles.active=" + profiles, "app.s3.fake-image-url=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("Fake image URLs require the local profile alone");
                });
    }

    @Test
    void fakeModeWithoutExplicitActiveProfileFailsEvenIfLocalIsDefaultProfile() {
        runner.withPropertyValues("spring.profiles.default=local", "app.s3.fake-image-url=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void fakeModeWithoutAnyProfileFails() {
        runner.withPropertyValues("app.s3.fake-image-url=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
