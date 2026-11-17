package cloud.picon.backend.global.config;

import cloud.picon.backend.global.s3.S3Properties;
import cloud.picon.backend.global.s3.LocalFakeImageUrlProvider;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import cloud.picon.backend.global.s3.PresignedS3ImageUrlProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    @ConditionalOnProperty(prefix = "app.s3", name = "fake-image-url", havingValue = "false", matchIfMissing = true)
    public S3ImageUrlProvider realImageUrlProvider(ObjectProvider<S3Presigner> presigner, S3Properties properties) {
        return new PresignedS3ImageUrlProvider(presigner, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.s3", name = "fake-image-url", havingValue = "true")
    public S3ImageUrlProvider localFakeImageUrlProvider(Environment environment) {
        if (!Arrays.equals(environment.getActiveProfiles(), new String[]{"local"})) {
            throw new IllegalStateException("Fake image URLs require the local profile alone");
        }
        return new LocalFakeImageUrlProvider();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.s3", name = "fake-image-url", havingValue = "false", matchIfMissing = true)
    @Lazy
    public S3Presigner s3Presigner(S3Properties properties) {
        S3Presigner.Builder builder = S3Presigner.builder();
        if (StringUtils.hasText(properties.region())) {
            builder.region(Region.of(properties.region()));
        }
        return builder.build();
    }
}
